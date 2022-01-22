package com.unipi.data.mining.backend.service.db;

import com.unipi.data.mining.backend.entities.mongodb.MongoSong;
import com.unipi.data.mining.backend.entities.neo4j.Neo4jSong;
import com.unipi.data.mining.backend.entities.neo4j.Neo4jUser;
import com.unipi.data.mining.backend.service.exceptions.DbException;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SongService extends EntityService{


    public MongoSong getMongoSongById(String id) {

        Optional<MongoSong> mongoSong = mongoSongRepository.findById(new ObjectId(id));

        if (mongoSong.isEmpty()){
            throw new DbException("No Song found with id " + id);
        }

        return mongoSong.get();
    }

    @Transactional
    public void deleteSongById(String id) {

        if (customSongRepository.deleteSongById(new ObjectId(id))) throw new DbException("Unable to delete mongodb user of id " + id);

        neo4jSongDao.deleteByMongoId(id);

        if (neo4jSongDao.getByMongoId(id).isPresent()) throw new DbException("Unable to delete neo4j song of id " + id);
    }

    public MongoSong createSong(MongoSong mongoSong) {

        MongoSong song = mongoSongRepository.save(mongoSong);
        ObjectId id = song.getId();

        Neo4jSong neo4jSong = new Neo4jSong(id.toString(), song.getName(), song.getArtists().stream().toList(), song.getAlbum());

        neo4jSongDao.createSong(neo4jSong);

        return song;
    }


    public Neo4jSong getNeo4jSongByMongoId(String id) {

        Optional<Neo4jSong> optionalNeo4jSong = neo4jSongDao.getByMongoId(id);

        if (optionalNeo4jSong.isEmpty()) throw new DbException("Unable to find song with id: " + id + " on neo4j");

        return optionalNeo4jSong.get();
    }

    public List<MongoSong> searchSongsByName(String name) {

        return customSongRepository.getSongsStartingWithName(name);
    }

    public List<Neo4jSong> getRecommendedSongs(String id) {

        Optional<Neo4jUser> optionalNeo4jUser = neo4jUserDao.getByMongoId(id);
        if (optionalNeo4jUser.isEmpty()) throw new DbException("Unable to find user with id: " + id + " on neo4j");

        return neo4jSongDao.getRecommendedSongs(id);
    }

    public void likeSong(String fromId, String toId, int like) {

        neo4jSongDao.updateLikeRelationship(fromId, toId, like);
    }
}
