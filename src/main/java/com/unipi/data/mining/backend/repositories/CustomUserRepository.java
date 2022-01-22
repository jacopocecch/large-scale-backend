package com.unipi.data.mining.backend.repositories;

import com.unipi.data.mining.backend.entities.mongodb.MongoUser;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomUserRepository extends CustomRepository{

    public List<MongoUser> getUsersByUsernameStartingWith(String username) {

        Query query = new Query();
        query.addCriteria(Criteria.where("username").regex("^" + username));
        query.with(Sort.by(Sort.Direction.ASC, "username"));
        query.fields().include("first_name").include("last_name").include("picture");
        query.limit(10);
        return mongoTemplate.find(query, MongoUser.class);
    }

    public MongoUser insertUser(MongoUser user) {

        return mongoTemplate.insert(user);
    }

    public boolean deleteUser(ObjectId id) {

        return mongoTemplate.remove(Query.query(Criteria.where("id").is(id)), MongoUser.class).wasAcknowledged();
    }

    public boolean existsByUsername(String username) {

        return mongoTemplate.exists(Query.query(Criteria.where("username").is(username)), MongoUser.class);
    }

    public boolean existsByEmail(String email) {

        return mongoTemplate.exists(Query.query(Criteria.where("email").is(email)), MongoUser.class);
    }

    // UTILITIES

    public void updateEmail(MongoUser user, String email) {

        Query query = new Query(Criteria.where("id").is(user.getId()));
        Update update = new Update();
        update.set("email", email);
        mongoTemplate.updateFirst(query, update, MongoUser.class);
    }

    public void bulkUpdateEmail(List<MongoUser> users) {

        BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, MongoUser.class);
        for (MongoUser user: users) {
            Update update = new Update();
            update.set("email", user.getEmail());
            bulkOperations.updateOne(Query.query(Criteria.where("id").is(user.getId())), update);
        }
        System.out.println(bulkOperations.execute());
    }

    public boolean updateCluster(MongoUser user)   {

        Query query = new Query(Criteria.where("id").is(user.getId()));
        Update update = new Update();
        update.set("cluster", user.getCluster());
        return mongoTemplate.updateFirst(query, update, MongoUser.class).wasAcknowledged();
    }

    public void bulkUpdateCluster(List<MongoUser> users) {

        BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, MongoUser.class);
        for (MongoUser user: users) {
            Update update = new Update();
            update.set("cluster", user.getCluster());
            bulkOperations.updateOne(Query.query(Criteria.where("id").is(user.getId())), update);
        }
        System.out.println(bulkOperations.execute());
    }

    public void updatePassword(MongoUser user)   {

        Query query = new Query(Criteria.where("id").is(user.getId()));
        Update update = new Update();
        update.set("password", user.getPassword());
        mongoTemplate.updateFirst(query, update, MongoUser.class);
    }

    public void bulkUpdatePassword(List<MongoUser> users) {

        BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, MongoUser.class);
        for (MongoUser user: users) {
            Update update = new Update();
            update.set("password", user.getPassword());
            bulkOperations.updateOne(Query.query(Criteria.where("id").is(user.getId())), update);
        }
        System.out.println(bulkOperations.execute());
    }

    public void bulkCreateNewUser(List<MongoUser> users) {

        System.out.println(mongoTemplate.insert(users, MongoUser.class));
    }

    public void bulkDeleteUsers(List<MongoUser> users) {

        BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, MongoUser.class);
        for (MongoUser user: users) {
            Query query = Query.query(Criteria.where("id").is(user.getId()));
            bulkOperations.remove(query);
        }
        System.out.println(bulkOperations.execute());
    }

    public List<MongoUser> findAllIds() {

        Query query = new Query();
        query.fields().include("id");
        return mongoTemplate.find(query, MongoUser.class);
    }

    public List<MongoUser> findAllClusters() {

        Query query = new Query();
        query.fields().include("cluster");
        return mongoTemplate.find(query, MongoUser.class);
    }

    public List<MongoUser> findToGenerateComments() {

        Query query = new Query();
        query.fields().include("first_name").include("last_name");
        return mongoTemplate.find(query, MongoUser.class);
    }

}
