package com.unipi.data.mining.backend.service.db;

import com.unipi.data.mining.backend.daos.Neo4jSongDao;
import com.unipi.data.mining.backend.daos.Neo4jUserDao;
import com.unipi.data.mining.backend.entities.neo4j.Neo4jSong;
import com.unipi.data.mining.backend.repositories.CustomSongRepository;
import com.unipi.data.mining.backend.repositories.CustomUserRepository;
import com.unipi.data.mining.backend.repositories.MongoSongRepository;
import com.unipi.data.mining.backend.repositories.MongoUserRepository;
import com.unipi.data.mining.backend.service.Utils;
import com.unipi.data.mining.backend.service.clustering.Clustering;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;


abstract class EntityService {

    @Autowired
    protected MongoUserRepository mongoUserRepository;

    @Autowired
    protected MongoSongRepository mongoSongRepository;

    @Autowired
    protected Neo4jUserDao neo4jUserDao;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected Clustering clustering;

    @Autowired
    protected Utils utils;

    @Autowired
    protected Neo4jSongDao neo4jSongDao;

    @Autowired
    protected CustomUserRepository customUserRepository;

    @Autowired
    protected CustomSongRepository customSongRepository;
}
