package com.unipi.data.mining.backend.controllers.services;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("songs")
public class SongController extends ServiceController{

    @Transactional
    @PostMapping("populate_neo4j")
    ResponseEntity<Object> populateNeo4jDatabase() {

        songService.populateNeo4jDatabase();
        return new ResponseEntity<>(
                HttpStatus.OK
        );
    }
}
