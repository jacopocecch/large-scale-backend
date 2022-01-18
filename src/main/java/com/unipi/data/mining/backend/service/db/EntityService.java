package com.unipi.data.mining.backend.service.db;

import com.unipi.data.mining.backend.daos.Neo4jUserDao;
import com.unipi.data.mining.backend.repositories.MongoUserRepository;
import com.unipi.data.mining.backend.service.Utils;
import com.unipi.data.mining.backend.service.clustering.Clustering;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;


abstract class EntityService {

    @Autowired
    protected MongoUserRepository mongoUserRepository;

    @Autowired
    protected Neo4jUserDao neo4jUserDao;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected Clustering clustering;

    @Autowired
    protected Utils utils;
}
