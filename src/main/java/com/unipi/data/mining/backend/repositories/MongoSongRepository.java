package com.unipi.data.mining.backend.repositories;

import com.unipi.data.mining.backend.entities.mongodb.MongoSong;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface MongoSongRepository extends MongoRepository<MongoSong, ObjectId> {

    @Query(value="{}", fields = "{name: 1, album: 1, artists: 1}")
    List<MongoSong> findAllToPopulateNeo4j();
}
