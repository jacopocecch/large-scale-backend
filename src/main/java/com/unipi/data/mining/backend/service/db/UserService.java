package com.unipi.data.mining.backend.service.db;

import com.unipi.data.mining.backend.data.*;
import com.unipi.data.mining.backend.data.aggregations.Country;
import com.unipi.data.mining.backend.entities.mongodb.MongoUser;
import com.unipi.data.mining.backend.entities.neo4j.FriendRequest.Status;
import com.unipi.data.mining.backend.entities.neo4j.Neo4jUser;
import com.unipi.data.mining.backend.service.exceptions.DbException;
import com.unipi.data.mining.backend.service.exceptions.LoginException;
import com.unipi.data.mining.backend.service.exceptions.RegistrationException;
import com.unipi.data.mining.backend.service.exceptions.SimilarityException;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class UserService extends EntityService {

    public MongoUser getMongoUserById(ObjectId id) {

        MongoUser mongoUser = customUserRepository.findById(id);

        if (mongoUser == null) {
            throw new DbException("No User found with id " + id);
        }

        return mongoUser;
    }

    @Transactional
    public void deleteUserById(String id) {

        ObjectId objectId = new ObjectId(id);

        if (!customUserRepository.deleteUser(objectId)) throw new RuntimeException("Unable to delete mongodb user of id " + id);

        customCommentRepository.deleteUserComments(objectId);

        neo4jUserDao.deleteByMongoId(id);

        if (neo4jUserDao.getByMongoId(id).isPresent()) throw new RuntimeException("Unable to delete neo4j user of id " + id);
    }

    @Transactional
    public MongoUser registerUser(MongoUser mongoUser) {

        if (customUserRepository.existsByUsername(mongoUser.getUsername())) throw new RegistrationException("Username already taken");

        if (customUserRepository.existsByEmail(mongoUser.getEmail())) throw new RegistrationException("This email already belongs to another user");

        mongoUser.setPassword(passwordEncoder.encode(mongoUser.getPassword()));
        mongoUser.setRegistrationDate(LocalDate.now());

        MongoUser insertedUser = customUserRepository.insertUser(mongoUser);
        ObjectId objectId = insertedUser.getId();

        Neo4jUser neo4jUser = new Neo4jUser(objectId.toString(), insertedUser.getFirstName(), insertedUser.getLastName(), insertedUser.getCountry(), insertedUser.getImage());

        neo4jUserDao.createUser(neo4jUser);

        if (utils.areSurveyValuesCorrect(insertedUser)) {

            clustering.performClustering(insertedUser);
            customUserRepository.updateCluster(insertedUser);
            neo4jUser.setCluster(insertedUser.getCluster());
            neo4jUserDao.updateCluster(neo4jUser);
            setNearestNeighbors(insertedUser);
        }

        return insertedUser;
    }

    @Transactional
    public void updateUser(MongoUser newData) {

        ObjectId id = newData.getId();

        MongoUser dbData = getMongoUserById(id);

        Map<String, String> toBeUpdated = getChangedInfo(dbData, newData);

        if (!customUserRepository.updateUserInfo(id, toBeUpdated)){
            // gestisco
        }

        if (toBeUpdated.containsKey("first_name") || toBeUpdated.containsKey("last_name") || toBeUpdated.containsKey("country") || toBeUpdated.containsKey("picture")){
            neo4jUserDao.updateUser(newData);
        }
    }

    public MongoUser login(Login login) {

        MongoUser mongoUser = customUserRepository.findByEmail(login.getEmail());

        if (mongoUser == null) {
            throw new LoginException("No user found with email: " + login.getEmail());
        }

        if (!passwordEncoder.matches(login.getPassword(), mongoUser.getPassword())) throw new LoginException("Invalid password");

        return mongoUser;
    }

    public List<MongoUser> getAllUsers() {

        return customUserRepository.findAll();
    }

    public List<Neo4jUser> getSimilarUsers(String id) {

        getNeo4jUserByMongoId(id);

        return neo4jUserDao.getSimilarUsersWithFriendshipStatus(id);
    }

    public void updateSimilarUsers(String id) {

        getNeo4jUserByMongoId(id);

        MongoUser user = getMongoUserById(new ObjectId(id));

        setNearestNeighbors(user);
    }

    public List<Neo4jUser> getNearbySimilarUsers(String id) {

        getNeo4jUserByMongoId(id);

        return neo4jUserDao.getNearbySimilarUsersWithFriendshipStatus(id);
    }

    @Transactional
    public void updateSimilarUsers(String fromUserId, String toUserId, double weight) {

        getNeo4jUserByMongoId(fromUserId);
        getNeo4jUserByMongoId(toUserId);

        neo4jUserDao.addSimilarityRelationship(fromUserId, toUserId, weight);
    }

    @Transactional
    public void addFriendRequest(String fromUserId, String toUserId) {

        getNeo4jUserByMongoId(fromUserId);
        getNeo4jUserByMongoId(toUserId);

        neo4jUserDao.addFriendRequest(fromUserId, toUserId);
    }

    @Transactional
    public void deleteFriendRequest(String fromUserId, String toUserId) {

        getNeo4jUserByMongoId(fromUserId);
        getNeo4jUserByMongoId(toUserId);

        neo4jUserDao.deleteFriendRequest(fromUserId, toUserId);
    }

    @Transactional
    public void updateFriendRequest(String fromUserId, String toUserId, int status) {

        getNeo4jUserByMongoId(fromUserId);
        getNeo4jUserByMongoId(toUserId);

        Status newStatus = status == 0 ? Status.REFUSED : Status.ACCEPTED;

        neo4jUserDao.updateFriendRequest(fromUserId, toUserId, newStatus);
    }

    public List<Neo4jUser> getIncomingFriendRequests(String id) {

        getNeo4jUserByMongoId(id);

        return neo4jUserDao.getIncomingFriendRequests(id);
    }

    public Neo4jUser getNeo4jUserByMongoId(String id) {

        Optional<Neo4jUser> optionalNeo4jUser = neo4jUserDao.getByMongoId(id);

        if (optionalNeo4jUser.isEmpty()) throw new DbException("Unable to find user with id: " + id + " on neo4j");

        return optionalNeo4jUser.get();
    }

    private Map<String, String> getChangedInfo(MongoUser dbData, MongoUser newData) {

        Map<String, String> toBeUpdated = new HashMap<>();

        if (!Objects.equals(dbData.getFirstName(), newData.getFirstName())) {
            toBeUpdated.put("first_name", newData.getFirstName());
        }
        if (!Objects.equals(dbData.getLastName(), newData.getLastName())) {
            toBeUpdated.put("last_name", newData.getLastName());
        }
        if (!Objects.equals(dbData.getGender(), newData.getGender())) {
            toBeUpdated.put("gender", newData.getGender());
        }
        if (!Objects.equals(dbData.getCountry(), newData.getCountry())) {
            toBeUpdated.put("country", newData.getCountry());
        }
        if (!Objects.equals(dbData.getUsername(), newData.getUsername())) {
            if (customUserRepository.existsByUsername(newData.getUsername())) {
                throw new RegistrationException("Username already taken!");
            }
            toBeUpdated.put("username", newData.getUsername());
        }
        if (!Objects.equals(dbData.getPhone(), newData.getPhone())) {
            toBeUpdated.put("phone", newData.getPhone());
        }
        if (!Objects.equals(dbData.getEmail(), newData.getEmail())) {
            if (customUserRepository.existsByEmail(newData.getEmail())) {
                throw new RegistrationException("Email already used!");
            }
            toBeUpdated.put("email", newData.getEmail());
        }
        if (!Objects.equals(newData.getPassword(), "")) {
            toBeUpdated.put("password", passwordEncoder.encode(newData.getPassword()));
        }

        if (!Objects.equals(dbData.getImage(), newData.getImage())) {
            toBeUpdated.put("picture", newData.getImage());
        }

        return toBeUpdated;
    }

    public Survey getUserClusterValues(String id) {

        MongoUser mongoUser = getMongoUserById(new ObjectId(id));

        if (!utils.areSurveyValuesCorrect(mongoUser)) {
            throw new RuntimeException("The user " + id + " does not have a valid survey");
        }

        return new Survey(mongoUser);
    }

    public List<Neo4jUser> getFriends(String id) {

        getNeo4jUserByMongoId(id);

        return neo4jUserDao.getUserFriends(id);
    }

    public Survey getUserClusterClusterValues(String id) {

        return customUserRepository.getAverageClusterValues(getMongoUserById(new ObjectId(id)).getCluster());
    }

    public MongoUser getMostSimilarUser(String id) {

        getNeo4jUserByMongoId(id);

        List<Neo4jUser> mostSimilarUser = neo4jUserDao.getMostSimilarUser(id);

        Optional<Neo4jUser> similarUser = mostSimilarUser.stream().findFirst();

        if (similarUser.isEmpty()) throw new SimilarityException("User " + id + " does not have similar users");

        return getMongoUserById(new ObjectId(similarUser.get().getMongoId()));
    }


    private void setNearestNeighbors(MongoUser mongoUser) {

        getNeo4jUserByMongoId(mongoUser.getId().toString());

        if (!utils.areSurveyValuesCorrect(mongoUser)) {
            throw new RuntimeException("User does not have a survey");
        }
        if (mongoUser.getCluster() == 0){
            throw new RuntimeException("User does not belong to any cluster");
        }

        Survey userSurvey = new Survey(mongoUser);
        List<MongoUser> mongoUsers = customUserRepository.findByClusterWithSurvey(mongoUser.getCluster());

        List<Distance> distances = new ArrayList<>();

        for (MongoUser user: mongoUsers) {

            if (user.getId().toString().equals(mongoUser.getId().toString())) continue;
            if (!utils.areSurveyValuesCorrect(user)) continue;
            Survey survey = new Survey(user);
            distances.add(new Distance(user.getId().toString(), utils.getDistance(userSurvey, survey)));
        }
        distances.sort(Comparator.comparingDouble(Distance::getDistance));

        neo4jUserDao.setNearestNeighbors(mongoUser.getId().toString(), distances, 20);
    }

    public void quarantineUser(String id) {

        getNeo4jUserByMongoId(id);

        neo4jUserDao.quarantineUser(id);
    }

    public List<MongoUser> searchUsersByUsername(String username) {

        return customUserRepository.getUsersByUsernameStartingWith(username);
    }

    public int getClusterWithHighestVariance() {

        return customUserRepository.getClusterWithHighestVariance().getId();
    }

    public List<Country> getTopKCountries(int k){

        return customUserRepository.getTopKCountries(k);
    }
}
