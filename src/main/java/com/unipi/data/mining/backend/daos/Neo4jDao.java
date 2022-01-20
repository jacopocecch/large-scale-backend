package com.unipi.data.mining.backend.daos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.driver.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

abstract class Neo4jDao {

    protected static final Logger LOGGER = LoggerFactory.getLogger(Neo4jDao.class.getName());

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected Driver driver;
}
