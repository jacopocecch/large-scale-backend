package com.unipi.data.mining.backend.repositories;

import com.unipi.data.mining.backend.entities.mongodb.MongoSong;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CustomSongRepository extends CustomRepository{

    public List<MongoSong> findSongsSortBy(String field, Sort.Direction direction) {

        Query query = new Query();
        query.with(Sort.by(direction, field));
        query.fields().include("id");
        query.limit(1000);
        return mongoTemplate.find(query, MongoSong.class);
    }

    public List<MongoSong> findAllIds() {

        Query query = new Query();
        query.fields().include("id");
        return mongoTemplate.find(query, MongoSong.class);
    }

    public void bulkDeleteSongs(List<MongoSong> songs) {

        BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, MongoSong.class);
        for (MongoSong song: songs) {
            Query query = Query.query(Criteria.where("id").is(song.getId()));
            bulkOperations.remove(query);
        }
        System.out.println(bulkOperations.execute());
    }

    public boolean deleteSongById(ObjectId id) {

        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(id));
        return mongoTemplate.remove(query, MongoSong.class).wasAcknowledged();
    }

    public List<MongoSong> getSongsStartingWith(String name) {

        Query query = new Query();
        query.addCriteria(Criteria.where("name").regex("^" + name));
        query.with(Sort.by(Sort.Direction.ASC, "name"));
        return mongoTemplate.find(query, MongoSong.class);
    }

}
