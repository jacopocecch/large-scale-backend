package com.unipi.data.mining.backend.entities.mongodb;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("song")
public class MongoSong {

    @MongoId
    private ObjectId id;
    //tutte le cose

}
