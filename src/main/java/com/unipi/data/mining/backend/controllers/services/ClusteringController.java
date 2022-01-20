package com.unipi.data.mining.backend.controllers.services;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("clustering")
public class ClusteringController extends ServiceController{

    @EventListener(ContextRefreshedEvent.class)
    public void clustering(){
        //userService.generatePasswords();
        //userService.hashPasswords();
        clustering.startClustering();
    }
}
