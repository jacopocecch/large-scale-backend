package com.unipi.data.mining.backend.daos;

import com.unipi.data.mining.backend.entities.neo4j.Neo4jSong;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.exceptions.Neo4jException;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class Neo4jSongDao extends Neo4jDao{

    public Optional<Neo4jSong> getByMongoId(String id) {

        try (Session session = driver.session()){

            List<Neo4jSong> songs = session.readTransaction(transaction -> {
                String query = """
                    MATCH (s:Song)
                    WHERE s.mongoId = $mongo_id
                    RETURN s""";

                Map<String, Object> params = Collections.singletonMap("mongo_id", id);
                return getNeo4jSongs(transaction, query, params);
            });
            return songs.stream().findFirst();
        }
    }

    public Optional<Neo4jSong> getBySongName(String name) {

        try (Session session = driver.session()){

            List<Neo4jSong> songs = session.readTransaction(transaction -> {
                String query = """
                    MATCH (s:Song)
                    WHERE s.name = $name
                    RETURN s""";

                Map<String, Object> params = Collections.singletonMap("name", name);
                return getNeo4jSongs(transaction, query, params);
            });
            return songs.stream().findFirst();
        }
    }

    public List<Neo4jSong> getRecommendedSongs(String id) {

        try (Session session = driver.session()){

            return session.readTransaction(transaction -> {
                String query = """
                        MATCH (u:User)-[p:PREFERENCE]->(s:Song)<-[p2:PREFERENCE]-(su:User)
                        WHERE su.mongoId <> u.mongoId AND p.value = p2.value AND u.mongoId = $mongo_id
                        WITH u AS user, su AS similarUser, count(*) AS coherence
                        MATCH (u:User)-[pr:PREFERENCE]->(s:Song)
                        WHERE u = user
                        WITH count(*) AS numLikes1, similarUser, user, coherence
                        MATCH (u:User)-[pr:PREFERENCE]->(s:Song)
                        WHERE u = similarUser
                        WITH count(*) AS numLikes2, numLikes1, similarUser, user, coherence
                        UNWIND [numLikes1,numLikes2] AS numLikes
                        WITH user, similarUser, coherence/(toFloat(min(numLikes))) AS BetaStrenght
                        MATCH (u:User)-[r:SIMILAR_TO]->(su:User)-[p:PREFERENCE]->(s:Song)
                        WHERE u = user and su = similarUser AND NOT (u)-[:PREFERENCE]->(s)
                        OPTIONAL MATCH (u)-[f:FRIEND_REQUEST {status: "accepted"}]->(su)
                        WITH BetaStrenght, s AS Song, u AS User, su as SimilarUser, p as Preference, CASE when count(f)>0 then r.weight * 2 else r.weight end AS Weight
                        RETURN Song, sum(0.8 * Weight * Preference.value + 0.2 * BetaStrenght * Preference.value) AS Strenght
                        ORDER BY Strenght DESC
                        LIMIT 50
                        """;
                Map<String, Object> params = new HashMap<>();
                params.put("mongo_id", id);
                params.put("alpha", 0.8);
                params.put("beta", 0.2);
                // rendere alpha e beta settabili da parametri di configurazione
                return getNeo4jSongs(transaction, query, params);
            });
        }
    }

    private Object addOrUpdatePreference(Transaction transaction, String fromId, String toId, int preference) {
        String query = """
                        MATCH(u: User)
                        MATCH(s: Song)
                        WHERE u.mongoId = $from_mongo_id
                        AND s.mongoId = $to_mongo_id
                        MERGE(u)-[r:SIMILAR_TO]-(s)
                        SET r.value = $preference""";

        Map<String, Object> params = new HashMap<>();
        params.put("from_mongo_id", fromId);
        params.put("to_mongo_id", toId);
        params.put("preference", preference);
        try {
            transaction.run(query, params);
        } catch (Neo4jException e) {
            LOGGER.error(query + " raised an exception", e);
            throw e;
        }
        return null;
    }

    public void createSong(Neo4jSong song) {

        try (Session session = driver.session()){

            session.writeTransaction(transaction -> {
                String query = """
                        CREATE (s: Song {mongoId: $mongo_id, name: $name, artists: $artists, album: $album})""";

                Map<String, Object> params = setCreateUpdateParameters(song);

                runTransaction(transaction, query, params);
                return null;
            });
        }
    }

    private List<Neo4jSong> getNeo4jSongs(Transaction transaction, String query, Map<String, Object> params) {

        try {
            Result result = transaction.run(query, params);
            return result.list(record -> objectMapper.convertValue(record.get(0).asMap(), Neo4jSong.class));
        } catch (Neo4jException e) {
            LOGGER.error(query + " raised an exception", e.getMessage());
            throw e;
        }
    }

    private Map<String, Object> setCreateUpdateParameters(Neo4jSong song) {

        Map<String, Object> params = new HashMap<>();
        params.put("mongo_id", song.getMongoId());
        params.put("name", song.getName());
        params.put("artists", song.getAuthors());
        params.put("album", song.getAlbum());
        return params;
    }

    public void deleteByMongoId(String id) {

        try (Session session = driver.session()){

            session.writeTransaction(transaction -> {
                String query = """
                        MATCH (s:Song)
                        WHERE s.mongoId = $mongo_id
                        DETACH DELETE s""";

                Map<String, Object> params = Collections.singletonMap("mongo_id", id);

                runTransaction(transaction, query, params);
                return null;
            });
        }
    }

    private void runTransaction(Transaction transaction, String query, Map<String, Object> params) {

        try {
            transaction.run(query, params);
        } catch (Neo4jException e) {
            LOGGER.error(query + " raised an exception", e);
            throw e;
        }
    }
}
