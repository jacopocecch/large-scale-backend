package com.unipi.data.mining.backend.service.db;

import com.unipi.data.mining.backend.entities.mongodb.MongoSong;
import com.unipi.data.mining.backend.entities.neo4j.Neo4jSong;

import java.util.List;
import java.util.Optional;

public class SongService extends EntityService{

    public void populateNeo4jDatabase() {

        List<MongoSong> mongoSongs = mongoSongRepository.findAllToPopulateNeo4j();

        for (MongoSong mongoSong: mongoSongs) {

            String objectId = mongoSong.getId().toString();

            Optional<Neo4jSong> optionalNeo4jSong = neo4jSongDao.getByMongoId(objectId);

            if (optionalNeo4jSong.isPresent()) continue;

            neo4jSongDao.createSong(new Neo4jSong(objectId, mongoSong.getName(), String.join(", ", mongoSong.getArtists().stream().toList()), mongoSong.getAlbum()));
        }
    }
}
