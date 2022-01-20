package com.unipi.data.mining.backend.repositories;

import com.unipi.data.mining.backend.entities.mongodb.MongoUser;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface MongoUserRepository extends MongoRepository<MongoUser, ObjectId> {

    MongoUser findMongoUserByEmail(String email);

    @Query(value="{}", fields = "{first_name: 1, last_name: 1, country: 1, picture: 1}")
    List<MongoUser> findAllWithNeo4jInfo();

    List<MongoUser> findMongoUsersByEmail(String email);

    @Query(value="{}", fields = "{survey: 1, cluster: 1}")
    List<MongoUser> findAllWithSurveyAndCluster();

    @Query(value="{'cluster': ?0}", fields = "{survey: 1}")
    List<MongoUser> findMongoUsersByCluster(int cluster);

    @Query(value="{}", fields = "{email: 1}")
    List<MongoUser> findEmails();

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query(value = "{}", fields = "{password: 1}")
    List<MongoUser> findAllPasswords();

    @Query(value = "{}", fields = "{_id: 1}")
    List<MongoUser> findAllIds();
}