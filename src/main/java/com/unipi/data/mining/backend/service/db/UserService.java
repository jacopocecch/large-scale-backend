package com.unipi.data.mining.backend.service.db;

import com.unipi.data.mining.backend.data.Distance;
import com.unipi.data.mining.backend.data.Login;
import com.unipi.data.mining.backend.entities.mongodb.MongoUser;
import com.unipi.data.mining.backend.entities.mongodb.Survey;
import com.unipi.data.mining.backend.entities.neo4j.Neo4jUser;
import com.unipi.data.mining.backend.service.exceptions.Neo4jRelationshipException;
import com.unipi.data.mining.backend.service.exceptions.LoginException;
import com.unipi.data.mining.backend.service.exceptions.RegistrationException;
import com.unipi.data.mining.backend.service.exceptions.SimilarityException;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class UserService extends EntityService {


    public MongoUser getMongoUserById(String id) {

        Optional<MongoUser> mongoUser = mongoUserRepository.findById(new ObjectId(id));

        if (mongoUser.isEmpty()) {
            throw new Neo4jRelationshipException("No User found with id " + id);
        }

        return mongoUser.get();
    }

    @Transactional
    public void deleteUserById(String id) {

        mongoUserRepository.deleteById(new ObjectId(id));

        if (mongoUserRepository.existsById(new ObjectId(id))) throw new RuntimeException("Unable to delete mongodb user of id " + id);

        neo4jUserDao.deleteByMongoId(id);

        if (neo4jUserDao.getByMongoId(id).isPresent()) throw new RuntimeException("Unable to delete neo4j user of id " + id);

    }

    @Transactional
    public MongoUser registerUser(MongoUser mongoUser) {

        if (mongoUserRepository.existsByUsername(mongoUser.getUsername())) throw new RegistrationException("Username already taken");

        if (mongoUserRepository.existsByEmail(mongoUser.getEmail())) throw new RegistrationException("This email already belongs to another user");

        //mongoUser.setPassword(passwordEncoder.encode(mongoUser.getPassword()));
        mongoUser.setRegistrationDate(LocalDate.now());

        mongoUser = mongoUserRepository.save(mongoUser);
        ObjectId objectId = mongoUser.getId();

        Neo4jUser neo4jUser = new Neo4jUser(objectId.toString(), mongoUser.getFirstName(), mongoUser.getLastName(), mongoUser.getCountry(), mongoUser.getImage());

        neo4jUserDao.createUser(neo4jUser);

        if (mongoUser.getSurvey() != null) {

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

        //if (!passwordEncoder.matches(login.getPassword(), mongoUser.getPassword())) throw new LoginException("Invalid password");

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

        if (optionalNeo4jUser.isEmpty()) throw new Neo4jRelationshipException("Unable to find user with id: " + id + " on neo4j");

        return optionalNeo4jUser.get();
    }


    public void populateNeo4jDatabase() {

        List<MongoUser> mongoUsers = mongoUserRepository.findAll();
        for (MongoUser mongoUser:mongoUsers) {

            String objectId = mongoUser.getId().toString();

            Optional<Neo4jUser> optionalNeo4jUser = neo4jUserDao.getByMongoId(objectId);

            if (optionalNeo4jUser.isPresent()) continue;

            neo4jUserDao.createUser(new Neo4jUser(objectId, mongoUser.getFirstName(), mongoUser.getLastName(), mongoUser.getCountry(), mongoUser.getImage()));
        }
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

        Survey survey = mongoUser.getSurvey();

        if (survey == null) {
            throw new RuntimeException("The user " + id + " does not have a survey");
        }

        return survey;
    }

    public List<Neo4jUser> getFriends(String id) {

        getNeo4jUserByMongoId(id);

        return neo4jUserDao.getUserFriends(id);
    }

    public Survey getUserClusterClusterValues(String id) {

        List<MongoUser> mongoUsers = mongoUserRepository.findMongoUsersByCluster(getMongoUserById(id).getCluster());
        List<Survey> surveys = new ArrayList<>();

        for (MongoUser mongoUser:mongoUsers) {

            if (mongoUser.getSurvey() == null) {
                continue;
            }

            surveys.add(mongoUser.getSurvey());
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

        if (mongoUser.getSurvey() == null) {
            throw new RuntimeException("User does not have a survey");
        }
        if (mongoUser.getCluster() == 0){
            throw new RuntimeException("User does not belong to any cluster");
        }

        Survey userSurvey = mongoUser.getSurvey();
        List<MongoUser> mongoUsers = mongoUserRepository.findMongoUsersByCluster(mongoUser.getCluster());

        List<Distance> distances = new ArrayList<>();

        for (MongoUser user: mongoUsers) {

            if (user.getId().toString().equals(mongoUser.getId().toString())) continue;
            if (user.getSurvey() == null) continue;
            Survey survey = user.getSurvey();
            distances.add(new Distance(user.getId().toString(), utils.getDistance(userSurvey, survey)));
        }
        distances.sort(Comparator.comparingDouble(Distance::getDistance));

        neo4jUserDao.setNearestNeighbors(mongoUser.getId().toString(), distances);
    }

    public void hashPasswords() {

        List<MongoUser> mongoUsers;
        int i = 0;
        do {
            Pageable pageable = PageRequest.of(i, 1000);
            mongoUsers = mongoUserRepository.findAll(pageable).toList();
            for (MongoUser mongoUser: mongoUsers) {
                String password = mongoUser.getPassword();
                if (password.length() == 16) continue;
                mongoUser.setPassword(passwordEncoder.encode(password));
            }
            customUserRepository.bulkUpdatePassword(mongoUsers);
        } while (!mongoUsers.isEmpty());
    }

    public void generatePasswords() {

        List<MongoUser> mongoUsers = mongoUserRepository.findAll();
        List<MongoUser> usersToBeUpdated = new ArrayList<>();

        for (MongoUser mongoUser: mongoUsers) {
            if (mongoUser.getPassword().length() == 10) continue;
            String password = utils.generatePassword();
            mongoUser.setPassword(password);
            usersToBeUpdated.add(mongoUser);
        }

        mongoUserRepository.saveAll(usersToBeUpdated);
    }

    public void changeDuplicateEmails() {


        List<MongoUser> users = mongoUserRepository.findEmails();

        List<String> emails = users.stream().map(MongoUser::getEmail).toList();

        Map<String, Integer> emailMap = new HashMap<>();

        List<String> duplicateEmails = new ArrayList<>();

        for (String email: emails) {

            if (emailMap.containsKey(email)) emailMap.put(email, emailMap.get(email) + 1);
            else emailMap.put(email, 1);
        }

        for (Map.Entry<String, Integer> entry: emailMap.entrySet()) {
            if (entry.getValue() > 1) duplicateEmails.add(entry.getKey());
        }

        for (String email: duplicateEmails) {

            List<MongoUser> mongoUsers = mongoUserRepository.findMongoUsersByEmail(email);

            if (mongoUsers.size() > 1) {

                int i = 0;

                for (MongoUser mongoUser: mongoUsers) {

                    String userEmail = mongoUser.getEmail();
                    userEmail = userEmail.replace("@", i + "@");
                    mongoUser.setEmail(userEmail);
                    i++;
                }
            }

            customUserRepository.bulkUpdateEmail(mongoUsers);
        }
    }

    /*
    public void convertUsers() {

        List<MongoUser> mongoUsers = mongoUserRepository.findAll();
        List<NewUser> newUsers = new ArrayList<>();

        for (MongoUser mongoUser: mongoUsers) {

            if (mongoUser.getSurvey() == null) continue;

            NewUser newUser = new NewUser();
            newUser.setId(mongoUser.getId());
            newUser.setFirstName(mongoUser.getFirstName());
            newUser.setLastName(mongoUser.getLastName());
            newUser.setDateOfBirth(mongoUser.getDateOfBirth());
            newUser.setGender(mongoUser.getGender());
            newUser.setCountry(mongoUser.getCountry());
            newUser.setUsername(mongoUser.getUsername());
            newUser.setPhone(mongoUser.getPhone());
            newUser.setEmail(mongoUser.getEmail());
            newUser.setPassword(mongoUser.getPassword());
            newUser.setRegistrationDate(mongoUser.getRegistrationDate());
            newUser.setImage(mongoUser.getImage());
            newUser.setCluster(mongoUser.getCluster());

            NewSurvey newSurvey = new NewSurvey();
            ClusterValues clusterValues = utils.getClusterValues(mongoUser.getSurvey());
            newSurvey.setAgreeableness(clusterValues.getAgreeableness());
            newSurvey.setConscientiousness(clusterValues.getConscientiousness());
            newSurvey.setNeuroticism(clusterValues.getNeuroticism());
            newSurvey.setExtraversion(clusterValues.getExtraversion());
            newSurvey.setOpenness(clusterValues.getOpenness());
            newSurvey.setTimeSpent(clusterValues.getTimeSpent());

            newUser.setSurvey(newSurvey);

            newUsers.add(newUser);
        }

        mongoNewUserRepository.saveAll(newUsers);
    }

     */
}
