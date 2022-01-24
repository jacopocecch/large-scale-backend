package com.unipi.large.scale.backend.daos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unipi.large.scale.backend.configs.RecommendedSongsConfigurationProperties;
import org.neo4j.driver.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(RecommendedSongsConfigurationProperties.class)
abstract class Neo4jDao {

    protected static final Logger LOGGER = LoggerFactory.getLogger(Neo4jDao.class.getName());

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected Driver driver;

    @Autowired
    protected RecommendedSongsConfigurationProperties recommendedSongsConfigurationProperties;
}
