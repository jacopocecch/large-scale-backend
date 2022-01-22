package com.unipi.data.mining.backend.service.db;

import com.unipi.data.mining.backend.data.Counter;
import com.unipi.data.mining.backend.data.Distance;
import com.unipi.data.mining.backend.data.Login;
import com.unipi.data.mining.backend.data.Survey;
import com.unipi.data.mining.backend.entities.mongodb.MongoUser;
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

    public MongoUser getMongoUserById(String id) {

        Optional<MongoUser> mongoUser = mongoUserRepository.findById(new ObjectId(id));

        if (mongoUser.isEmpty()) {
            throw new DbException("No User found with id " + id);
        }

        return mongoUser.get();
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

        Neo4jUser neo4jUser = new Neo4jUser(objectId.toString(), insertedUser.getFirstName(), insertedUser.getLastName(), insertedUser.getCluster(), insertedUser.getCountry(), insertedUser.getImage());

        neo4jUserDao.createUser(neo4jUser);

        if (utils.areSurveyValuesCorrect(mongoUser)) {

            clustering.performClustering(mongoUser);
            mongoUserRepository.save(mongoUser);
            setNearestNeighbors(mongoUser);
        }

        return mongoUser;
    }

    @Transactional
    public MongoUser updateUser(MongoUser newData) {

        MongoUser dbData = getMongoUserById(newData.getId().toString());
        Neo4jUser neo4jUser = getNeo4jUserByMongoId(newData.getId().toString());

        updateMongoUserInfo(dbData, newData);
        updateNeo4jUserInfo(newData, neo4jUser);

        MongoUser updatedMongoUser = mongoUserRepository.save(dbData);
        neo4jUserDao.updateUser(neo4jUser);

        return updatedMongoUser;
    }

    public MongoUser login(Login login) {

        MongoUser mongoUser = mongoUserRepository.findMongoUserByEmail(login.getEmail());

        if (mongoUser == null) {
            throw new LoginException("No user found with email: " + login.getEmail());
        }

        if (!passwordEncoder.matches(login.getPassword(), mongoUser.getPassword())) throw new LoginException("Invalid password");

        if (!Objects.equals(login.getPassword(), mongoUser.getPassword())) {
            throw new LoginException("Invalid password");
        }

        setNearestNeighbors(mongoUser);

        return mongoUser;
    }

    public List<MongoUser> getAllUsers() {

        return mongoUserRepository.findAll();
    }

    public List<Neo4jUser> getSimilarUsers(String id) {

        getNeo4jUserByMongoId(id);

        return neo4jUserDao.getSimilarUsersWithFriendshipStatus(id);
    }

    public void updateSimilarUsers(String id) {

        getNeo4jUserByMongoId(id);

        MongoUser user = getMongoUserById(id);

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

        com.unipi.data.mining.backend.entities.neo4j.FriendRequest.Status newStatus = status == 0 ? com.unipi.data.mining.backend.entities.neo4j.FriendRequest.Status.REFUSED : com.unipi.data.mining.backend.entities.neo4j.FriendRequest.Status.ACCEPTED;

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

    private void updateMongoUserInfo(MongoUser dbData, MongoUser newData) {

        if (!Objects.equals(dbData.getFirstName(), newData.getFirstName())) dbData.setFirstName(newData.getFirstName());
        if (!Objects.equals(dbData.getLastName(), newData.getLastName())) dbData.setLastName(newData.getLastName());
        if (!Objects.equals(dbData.getDateOfBirth(), newData.getDateOfBirth())) dbData.setDateOfBirth(newData.getDateOfBirth());
        if (!Objects.equals(dbData.getGender(), newData.getGender())) dbData.setGender(newData.getGender());
        if (!Objects.equals(dbData.getCountry(), newData.getCountry())) dbData.setCountry(newData.getCountry());
        if (!Objects.equals(dbData.getUsername(), newData.getUsername())) {
            if (mongoUserRepository.existsByUsername(newData.getUsername())) {
                throw new RegistrationException("Username already taken!");
            }
            dbData.setUsername(newData.getUsername());
        }
        if (!Objects.equals(dbData.getPhone(), newData.getPhone())) dbData.setPhone(newData.getPhone());
        if (!Objects.equals(dbData.getEmail(), newData.getEmail())) {
            if (mongoUserRepository.existsByEmail(newData.getEmail())) {
                throw new RegistrationException("Email already used!");
            }
            dbData.setEmail(newData.getEmail());
        }
        if (!Objects.equals(newData.getPassword(), "")) {
            //dbData.setPassword(passwordEncoder.encode(newData.getPassword()));
            dbData.setPassword(newData.getPassword());
        }

        if (!Objects.equals(dbData.getImage(), newData.getImage())) {
            dbData.setImage(newData.getImage());
        }
    }

    private void updateNeo4jUserInfo(MongoUser newData, Neo4jUser dbData) {

        if (!Objects.equals(newData.getFirstName() + " " + newData.getLastName(), dbData.getFirstName())) {
            dbData.setFirstName(newData.getFirstName() + " " + newData.getLastName());
        }
        if (!Objects.equals(dbData.getCountry(), newData.getCountry())) dbData.setCountry(newData.getCountry());

        if (!Objects.equals(dbData.getImage(), newData.getImage())) dbData.setImage(newData.getImage());
    }

    public Survey getUserClusterValues(String id) {

        MongoUser mongoUser = getMongoUserById(id);

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

        List<MongoUser> mongoUsers = mongoUserRepository.findMongoUsersByCluster(getMongoUserById(id).getCluster());
        List<Survey> surveys = new ArrayList<>();

        for (MongoUser mongoUser:mongoUsers) {

            if (!utils.areSurveyValuesCorrect(mongoUser)) {
                continue;
            }

            surveys.add(new Survey(mongoUser));
        }

        Survey survey = new Survey();

        survey.setExtraversion(Math.round(surveys.stream().mapToDouble(Survey::getExtraversion).average().orElse(0.0) * 100.00) / 100.00);
        survey.setNeuroticism(Math.round(surveys.stream().mapToDouble(Survey::getNeuroticism).average().orElse(0.0) * 100.00) / 100.00);
        survey.setAgreeableness(Math.round(surveys.stream().mapToDouble(Survey::getAgreeableness).average().orElse(0.0) * 100.00) / 100.00);
        survey.setOpenness(Math.round(surveys.stream().mapToDouble(Survey::getOpenness).average().orElse(0.0) * 100.00) / 100.00);
        survey.setConscientiousness(Math.round(surveys.stream().mapToDouble(Survey::getConscientiousness).average().orElse(0.0) * 100.00) / 100.00);
        survey.setTimeSpent(Math.round(surveys.stream().mapToDouble(Survey::getTimeSpent).average().orElse(0.0) * 100.00) / 100.00);

        return survey;
    }

    public MongoUser getMostSimilarUser(String id) {

        getNeo4jUserByMongoId(id);

        List<Neo4jUser> mostSimilarUser = neo4jUserDao.getMostSimilarUser(id);

        Optional<Neo4jUser> similarUser = mostSimilarUser.stream().findFirst();

        if (similarUser.isEmpty()) throw new SimilarityException("User " + id + " does not have similar users");

        return getMongoUserById(similarUser.get().getMongoId());
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
        List<MongoUser> mongoUsers = mongoUserRepository.findMongoUsersByCluster(mongoUser.getCluster());

        List<Distance> distances = new ArrayList<>();

        for (MongoUser user: mongoUsers) {

            if (user.getId().toString().equals(mongoUser.getId().toString())) continue;
            if (!utils.areSurveyValuesCorrect(user)) continue;
            Survey survey = new Survey(user);
            distances.add(new Distance(user.getId().toString(), utils.getDistance(userSurvey, survey)));
        }
        distances.sort(Comparator.comparingDouble(Distance::getDistance));

        neo4jUserDao.setNearestNeighbors(mongoUser.getId().toString(), distances, 30);
    }


    public void hashPasswords() {

        List<MongoUser> mongoUsers = mongoUserRepository.findAllPasswords();
        List<MongoUser> usersToBeUpdated = new ArrayList<>();

        for (MongoUser mongoUser: mongoUsers) {
            String password = mongoUser.getPassword();
            mongoUser.setPassword(passwordEncoder.encode(password));
            usersToBeUpdated.add(mongoUser);

            if (usersToBeUpdated.size() == 1000) {
                customUserRepository.bulkUpdatePassword(usersToBeUpdated);
                usersToBeUpdated.clear();
            }
        }

        if (!usersToBeUpdated.isEmpty()) {
            customUserRepository.bulkUpdatePassword(usersToBeUpdated);
        }
    }

    public void generatePasswords() {

        List<MongoUser> mongoUsers = mongoUserRepository.findAllIds();
        List<MongoUser> usersToBeUpdated = new ArrayList<>();

        for (MongoUser mongoUser: mongoUsers) {
            String password = utils.generatePassword();
            mongoUser.setPassword(password);
            usersToBeUpdated.add(mongoUser);

            if (usersToBeUpdated.size() == 1000) {
                customUserRepository.bulkUpdatePassword(usersToBeUpdated);
                usersToBeUpdated.clear();
            }
        }

        if (!usersToBeUpdated.isEmpty()) {
            customUserRepository.bulkUpdatePassword(usersToBeUpdated);
        }
    }

    public void changeDuplicateEmails() {

        List<MongoUser> users = mongoUserRepository.findEmails();

        Map<String, Counter> emailMap = new HashMap<>();

        Map<String, List<MongoUser>> duplicateEmails = new HashMap<>();

        for (MongoUser user: users) {

            if (emailMap.containsKey(user.getEmail())) {
                Counter counter = emailMap.get(user.getEmail());
                counter.setCount(counter.getCount() + 1);
                counter.getUsers().add(user);
                emailMap.put(user.getEmail(), counter);
            }
            else {
                emailMap.put(user.getEmail(), new Counter(1, user));
            }
        }

        for (Map.Entry<String, Counter> entry: emailMap.entrySet()) {
            if (entry.getValue().getCount() > 1) duplicateEmails.put(entry.getKey(), entry.getValue().getUsers());
        }

        List<MongoUser> toBeUpdated = new ArrayList<>();

        for (Map.Entry<String, List<MongoUser>> entry: duplicateEmails.entrySet()) {

            List<MongoUser> mongoUsers = entry.getValue();

            if (mongoUsers.size() > 1) {

                int i = 0;

                for (MongoUser mongoUser: mongoUsers) {

                    String userEmail = mongoUser.getEmail();
                    userEmail = userEmail.replace("@", i + "@");
                    mongoUser.setEmail(userEmail);
                    i++;
                }
            }

            toBeUpdated.addAll(mongoUsers);

            if (toBeUpdated.size() > 950) {
                customUserRepository.bulkUpdateEmail(toBeUpdated);
                toBeUpdated.clear();
            }
        }
        customUserRepository.bulkUpdateEmail(toBeUpdated);
    }

    public void quarantineUser(String id) {

        getNeo4jUserByMongoId(id);

        neo4jUserDao.quarantineUser(id);
    }

    public List<MongoUser> searchUsersByUsername(String username) {

        return customUserRepository.getUsersByUsernameStartingWith(username);
    }
}
