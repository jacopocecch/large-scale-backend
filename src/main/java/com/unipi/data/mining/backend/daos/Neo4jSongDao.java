package com.unipi.data.mining.backend.daos;

import com.unipi.data.mining.backend.entities.neo4j.Neo4jSong;
import com.unipi.data.mining.backend.entities.neo4j.Neo4jUser;
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

    private void runTransaction(Transaction transaction, String query, Map<String, Object> params) {

        try {
            transaction.run(query, params);
        } catch (Neo4jException e) {
            LOGGER.error(query + " raised an exception", e);
            throw e;
        }
    }
}
