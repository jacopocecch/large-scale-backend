package com.unipi.data.mining.backend.repositories;

import com.unipi.data.mining.backend.entities.mongodb.MongoUser;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomUserRepository extends CustomRepository{

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

    public void updateCluster(MongoUser user)   {

        Query query = new Query(Criteria.where("id").is(user.getId()));
        Update update = new Update();
        update.set("cluster", user.getCluster());
        mongoTemplate.updateFirst(query, update, MongoUser.class);
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

}
