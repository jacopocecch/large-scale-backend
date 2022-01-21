package com.unipi.data.mining.backend.daos;

import com.unipi.data.mining.backend.data.Distance;
import com.unipi.data.mining.backend.entities.mongodb.MongoUser;
import com.unipi.data.mining.backend.entities.neo4j.FriendRequest;
import com.unipi.data.mining.backend.entities.neo4j.Neo4jUser;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.exceptions.Neo4jException;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class Neo4jUserDao extends Neo4jDao{

    public Optional<Neo4jUser> getByMongoId(String id) {

        try (Session session = driver.session()){

            List<Neo4jUser> users = session.readTransaction(transaction -> {
                String query = """
                    MATCH (u:User)
                    WHERE u.mongoId = $mongo_id
                    RETURN u""";

                Map<String, Object> params = Collections.singletonMap("mongo_id", id);
                return getNeo4jUsers(transaction, query, params);
            });
            return users.stream().findFirst();
        }
    }

    public void deleteByMongoId(String id) {

        try (Session session = driver.session()){

            session.writeTransaction(transaction -> {
                String query = """
                        MATCH (u:User)
                        WHERE u.mongoId = $mongo_id
                        DETACH DELETE u""";

                Map<String, Object> params = Collections.singletonMap("mongo_id", id);

                runTransaction(transaction, query, params);
                return null;
            });
        }
    }

    public void createUser(Neo4jUser user) {

        try (Session session = driver.session()){

            session.writeTransaction(transaction -> {
                String query = """
                        CREATE (u: User {mongoId: $mongo_id, firstName: $first_name, lastName: $last_name, country: $country, picture: $image})""";

                Map<String, Object> params = setCreateUpdateParameters(user);

                runTransaction(transaction, query, params);
                return null;
            });
        }
    }

    public void updateUser(Neo4jUser user) {

        try (Session session = driver.session()){

            session.writeTransaction(transaction -> {
                String query = """
                        MATCH (u: User {mongoId: $mongo_id})
                        SET u.firstName: $first_name, u.lastName: $last_name, u.country = $country, u.picture = $image
                        RETURN u""";

                Map<String, Object> params = setCreateUpdateParameters(user);

                runTransaction(transaction, query, params);
                return null;
            });
        }
    }

    public List<Neo4jUser> getUserFriends(String id){

        try (Session session = driver.session()){

            return session.readTransaction(transaction -> {
                String query = """
                    MATCH (u: User)-[r:FRIEND_REQUEST]-(friend:User)
                    WHERE r.status = 'ACCEPTED'
                    AND u.mongoId = $mongo_id
                    RETURN friend""";

                Map<String, Object> params = Collections.singletonMap("mongo_id", id);
                return getNeo4jUsers(transaction, query, params);
            });
        }
    }

    public void addSimilarityRelationship(String fromId, String toId, double weight){

        try (Session session = driver.session()){

            session.writeTransaction(transaction -> addOrUpdateSimilarity(transaction, fromId, toId, weight));
        }
    }

    public List<Neo4jUser> getIncomingFriendRequests(String id) {

        try (Session session = driver.session()){

            return session.readTransaction(transaction -> {
                String query = """
                    MATCH (a: User {mongoId: $mongo_id})<-[r:FRIEND_REQUEST {status: $status}]-(b:User)
                    RETURN b""";

                Map<String, Object> params = new HashMap<>();
                params.put("mongo_id", id);
                params.put("status", FriendRequest.Status.UNKNOWN.name());

                return getNeo4jUsers(transaction, query, params);
            });
        }
    }

    public List<Neo4jUser> getOutgoingFriendRequests(String id) {

        try (Session session = driver.session()){

            return session.readTransaction(transaction -> {
                String query = """
                    MATCH (a: User {mongoId: $mongo_id})-[r:FRIEND_REQUEST {status: $status]->(b:User)
                    RETURN b""";

                Map<String, Object> params = new HashMap<>();
                params.put("mongo_id", id);
                params.put("status", FriendRequest.Status.UNKNOWN.name());

                return getNeo4jUsers(transaction, query, params);
            });
        }
    }

    public void addFriendRequest(String fromId, String toId){

        try (Session session = driver.session()){

            int count = session.readTransaction(transaction -> {
                String query = """
                    MATCH (a: User)-[r:FRIEND_REQUEST]-(b:User)
                    WHERE a.mongoId = $from_mongo_id
                    AND b.mongoId = $to_mongo_id
                    RETURN count(r)""";

                Map<String, Object> params = new HashMap<>();
                params.put("from_mongo_id", fromId);
                params.put("to_mongo_id", toId);

                return getCount(transaction, query, params);
            });

            if (count != 0) {
                throw new RuntimeException("Friend request already sent or incoming!");
            }

            session.writeTransaction(transaction -> {
                String query = """
                        MATCH(a: User)
                        MATCH(b: User)
                        WHERE a.mongoId = $from_mongo_id
                        AND b.mongoId = $to_mongo_id
                        CREATE(a)-[r:FRIEND_REQUEST {status: $status}]->(b)""";

                Map<String, Object> params = new HashMap<>();
                params.put("from_mongo_id", fromId);
                params.put("to_mongo_id", toId);
                params.put("status", FriendRequest.Status.UNKNOWN.name());

                runTransaction(transaction, query, params);
                return null;
            });
        }
    }

    public void updateFriendRequest(String fromId, String toId, FriendRequest.Status status) {

        try (Session session = driver.session()) {
            session.writeTransaction(transaction -> {
                String query = """
                        MATCH(a: User)
                        MATCH(b: User)
                        WHERE a.mongoId = $from_mongo_id
                        AND b.mongoId = $to_mongo_id
                        MERGE(a)-[r:FRIEND_REQUEST]-(b)
                        SET r.status = $status""";

                Map<String, Object> params = new HashMap<>();
                params.put("from_mongo_id", fromId);
                params.put("to_mongo_id", toId);
                params.put("status", status.name());

                runTransaction(transaction, query, params);
                return null;
            });
        }
    }

    public void deleteFriendRequest(String fromId, String toId) {

        try (Session session = driver.session()){

            session.writeTransaction(transaction -> {
                String query = """
                        MATCH (a: User {mongoId: $from_mongo_id})-[r:FRIEND_REQUEST]-(b: User {mongoId: $to_mongo_id})
                        DELETE r""";

                Map<String, Object> params = new HashMap<>();
                params.put("from_mongo_id", fromId);
                params.put("to_mongo_id", toId);

               runTransaction(transaction, query, params);
                return null;
            });
        }
    }

    public void quarantineUser(String id) {
        try (Session session = driver.session()){

            session.writeTransaction(transaction -> {
                String query = """
                         MATCH (u:User)-[f:FRIEND_REQUEST]-(:User)
                         WHERE u.mongoId = $mongo_id
                         DELETE f  
                        """;

                Map<String, Object> params = Collections.singletonMap("mongo_id", id);
                runTransaction(transaction, query, params);
                query = """
                         MATCH (u:User)-[r:SIMILAR_TO]-(:User)
                         WHERE u.mongoId = $mongo_id
                         SET r.weight = 0 
                """;
                params = Collections.singletonMap("mongo_id", id);
                runTransaction(transaction, query, params);
                return null;
            });
        }
    }



    public boolean areFriends(String fromId, String toId) {

        try (Session session = driver.session()){

            int count = session.readTransaction(transaction -> {
                String query = """
                    MATCH (a: User)-[r:FRIEND_REQUEST]-(b:User)
                    WHERE r.status = 'ACCEPTED'
                    WHERE a.mongoId = $from_mongo_id
                    AND b.mongoId = $to_mongo_id
                    RETURN count(*)""";

                Map<String, Object> params = new HashMap<>();
                params.put("from_mongo_id", fromId);
                params.put("to_mongo_id", toId);

                return getCount(transaction, query, params);
            });
            return count != 0;
        }
    }

    public List<Neo4jUser> getSimilarUsers(String id) {

        try (Session session = driver.session()){

            return session.readTransaction(transaction -> {
                String query = """
                    MATCH (a:User)-[f:FRIEND_REQUEST]-(b:User)
                    WHERE a.mongoId = $mongo_id
                    AND f.status <> 'UNKNOWN'
                    WITH collect(b) AS excluded
                    MATCH (a: User)-[r:SIMILAR_TO]-(b:User)
                    WHERE a.mongoId = $mongo_id
                    WITH excluded, r, collect(b) as users
                    WHERE NONE(b in users where b in excluded)
                    RETURN users
                    ORDER BY r.weight DESC""";

                Map<String, Object> params = Collections.singletonMap("mongo_id", id);

                return getNeo4jUsers(transaction, query, params);
            });
        }
    }

    public List<Neo4jUser> getMostSimilarUser(String id) {

        try (Session session = driver.session()){

            return session.readTransaction(transaction -> {
                String query = """
                    MATCH (a: User {mongoId: $mongo_id})-[r:SIMILAR_TO]-(b:User)
                    RETURN b
                    ORDER BY r.weight DESC
                    LIMIT 1""";

                Map<String, Object> params = Collections.singletonMap("mongo_id", id);

                return getNeo4jUsers(transaction, query, params);
            });
        }
    }

    public List<Neo4jUser> getSimilarUsersWithFriendshipStatus(String id) {


        try (Session session = driver.session()){

            return session.readTransaction(transaction -> {
                String query = """
                        MATCH (a:User)-[f:FRIEND_REQUEST]-(b:User)
                        WHERE a.mongoId = $mongo_id
                        AND f.status <> 'UNKNOWN'
                        WITH collect(b) AS excluded
                        MATCH (a:User {mongoId:$mongo_id })-[r:SIMILAR_TO]-(b:User)
                        OPTIONAL MATCH (a)-[outgoing:FRIEND_REQUEST]->(b)
                        OPTIONAL MATCH (a)<-[incoming:FRIEND_REQUEST]-(b)
                        WITH excluded, r, outgoing, incoming, collect(b) as users
                        WHERE NONE(b in users where b in excluded)
                        RETURN users, outgoing, incoming
                        ORDER BY r.weight DESC""";

                Map<String, Object> params = Collections.singletonMap("mongo_id", id);

                return getNeo4jUsersWithFriendshipStatus(transaction, query, params);
            });
        }
    }

    public List<Neo4jUser> getNearbySimilarUsersWithFriendshipStatus(String id) {


        try (Session session = driver.session()){

            return session.readTransaction(transaction -> {
                String query = """
                        MATCH (a:User)-[f:FRIEND_REQUEST]-(b:User)
                        WHERE a.mongoId = $mongo_id
                        AND f.status <> 'UNKNOWN'
                        WITH collect(b) AS excluded
                        MATCH (a:User {mongoId:$mongo_id })-[r:SIMILAR_TO]-(b:User {country: a.country})
                        OPTIONAL MATCH (a)-[outgoing:FRIEND_REQUEST]->(b)
                        OPTIONAL MATCH (a)<-[incoming:FRIEND_REQUEST]-(b)
                        WITH excluded, r, outgoing, incoming, collect(b) as users
                        WHERE NONE(b in users where b in excluded)
                        RETURN users, outgoing, incoming
                        ORDER BY r.weight DESC""";

                Map<String, Object> params = Collections.singletonMap("mongo_id", id);

                return getNeo4jUsersWithFriendshipStatus(transaction, query, params);
            });
        }
    }

    public String getFriendshipStatus(String fromId, String toId) {

        try (Session session = driver.session()) {

            return session.readTransaction(transaction -> {
                String query = """
                        MATCH (a: User)-[f:FRIEND_REQUEST]-(b:User)
                        WHERE a.mongoId = $from_mongo_id
                        AND b.mongoId = $to_mongo_id
                        RETURN f.status""";

                Map<String, Object> params = new HashMap<>();
                params.put("from_mongo_id", fromId);
                params.put("to_mongo_id", toId);

                try {
                    Result result = transaction.run(query, params);
                    return result.single().get(0).asString();
                } catch (Neo4jException e) {
                    LOGGER.error(query + " raised an exception", e);
                    throw e;
                }
            });
        }
    }

    public void setNearestNeighbors(String id, List<Distance> distances) {

        try (Session session = driver.session()) {

            session.writeTransaction(transaction -> {

                for (int i = 0; i < 30; i++) {
                    Distance distance = distances.get(i);
                    Optional<Neo4jUser> optionalNeo4jUser = getByMongoId(distance.getUserId());
                    if (optionalNeo4jUser.isEmpty()) {
                        continue;
                    }
                    Neo4jUser neo4jUser = optionalNeo4jUser.get();
                    double weight;
                    if (distance.getDistance() == 0) {
                        weight = 10;
                    } else {
                        weight = Math.round(1/distance.getDistance()* 100.00) / 100.00;
                    }
                    addOrUpdateSimilarity(transaction, id, neo4jUser.getMongoId(), weight);
                }
                return null;
            });
        }
    }

    private Object addOrUpdateSimilarity(Transaction transaction, String fromId, String toId, double weight) {
        String query = """
                        MATCH(a: User)
                        MATCH(b: User)
                        WHERE a.mongoId = $from_mongo_id
                        AND b.mongoId = $to_mongo_id
                        MERGE(a)-[r:SIMILAR_TO]-(b)
                        SET r.weight = $weight""";

        Map<String, Object> params = new HashMap<>();
        params.put("from_mongo_id", fromId);
        params.put("to_mongo_id", toId);
        params.put("weight", weight);

        try {
            transaction.run(query, params);
        } catch (Neo4jException e) {
            LOGGER.error(query + " raised an exception", e);
            throw e;
        }
        return null;
    }

    private Map<String, Object> setCreateUpdateParameters(Neo4jUser user) {

        Map<String, Object> params = new HashMap<>();
        params.put("mongo_id", user.getMongoId());
        params.put("first_name", user.getFirstName());
        params.put("last_name", user.getLastName());
        params.put("country", user.getCountry());
        params.put("image", user.getImage());
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

    private List<Neo4jUser> getNeo4jUsers(Transaction transaction, String query, Map<String, Object> params) {

        try {
            Result result = transaction.run(query, params);
            return result.list(record -> objectMapper.convertValue(record.get(0).asMap(), Neo4jUser.class));
        } catch (Neo4jException e) {
            LOGGER.error(query + " raised an exception", e.getMessage());
            throw e;
        }
    }

    private List<Neo4jUser> getNeo4jUsersWithFriendshipStatus(Transaction transaction, String query, Map<String, Object> params) {

        try {
            Result result = transaction.run(query, params);
            List<Neo4jUser> users = new ArrayList<>();
            for (Record record: result.list()){
                Neo4jUser user = objectMapper.convertValue(record.get("users").get(0).asMap(), Neo4jUser.class);
                if (!record.get("incoming").isNull()){
                    FriendRequest.Status status = FriendRequest.Status.valueOf(record.get("incoming").asMap().get("status").toString());
                    user.setFriendRequest(new FriendRequest(FriendRequest.Type.INCOMING, status));
                } else if (!record.get("outgoing").isNull()) {
                    FriendRequest.Status status = FriendRequest.Status.valueOf(record.get("outgoing").asMap().get("status").toString());
                    user.setFriendRequest(new FriendRequest(FriendRequest.Type.OUTGOING, status));
                }
                users.add(user);
            }
            return users;
        } catch (Neo4jException e) {
            LOGGER.error(query + " raised an exception", e.getMessage());
            throw e;
        }
    }

    private int getCount(Transaction transaction, String query, Map<String, Object> params) {

        try {
            Result result = transaction.run(query, params);
            return result.single().get(0).asInt();
        } catch (Neo4jException e) {
            LOGGER.error(query + " raised an exception", e.getMessage());
            throw e;
        }
    }
    /*
:auto USING PERIODIC COMMIT 500
LOAD CSV WITH HEADERS FROM 'file:///user.csv' AS row
WITH row._id as mongoId, row.country as country, row.first_name as firstName, row.last_name as lastName, row.picture as picture
CREATE (u:User {mongoId: mongoId, country : country, firstName : firstName, lastName : lastName, picture : picture})


:auto USING PERIODIC COMMIT 500
LOAD CSV WITH HEADERS FROM 'file:///song.csv' AS row
WITH row.mongoId as mongoId, row.name as name, row.album as album, row.artists as artists
CREATE (s:Song {mongoId: mongoId, name : name, album : album, artists : artists})
     */
}
