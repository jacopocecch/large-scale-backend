package com.unipi.data.mining.backend.repositories;

import com.unipi.data.mining.backend.entities.mongodb.MongoUser;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class CustomUserRepository extends CustomRepository{

    public void updateEmail(MongoUser user, String email) {

        Query query = new Query(Criteria.where("id").is(user.getId()));
        Update update = new Update();
        update.set("email", email);
        mongoTemplate.updateFirst(query, update, MongoUser.class);
    }

    public void updateCluster(MongoUser user)   {

        Query query = new Query(Criteria.where("id").is(user.getId()));
        Update update = new Update();
        update.set("cluster", user.getCluster());
        mongoTemplate.updateFirst(query, update, MongoUser.class);
    }

    public void updatePassword(MongoUser user)   {

        Query query = new Query(Criteria.where("id").is(user.getId()));
        Update update = new Update();
        update.set("password", user.getPassword());
        mongoTemplate.updateFirst(query, update, MongoUser.class);
    }
}
