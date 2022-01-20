package com.unipi.data.mining.backend.service.db;

import com.unipi.data.mining.backend.entities.mongodb.MongoSong;
import com.unipi.data.mining.backend.entities.neo4j.Neo4jSong;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SongService extends EntityService{

    public void populateNeo4jDatabase() {

        List<MongoSong> mongoSongs = mongoSongRepository.findAllToPopulateNeo4j();

        for (MongoSong mongoSong: mongoSongs) {

            neo4jSongDao.populateNeo4j(mongoSong);
        }
    }

}
