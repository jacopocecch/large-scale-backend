package com.unipi.data.mining.backend.service.clustering;

import com.unipi.data.mining.backend.entities.mongodb.MongoUser;
import com.unipi.data.mining.backend.entities.mongodb.Survey;
import com.unipi.data.mining.backend.repositories.CustomUserRepository;
import com.unipi.data.mining.backend.repositories.MongoUserRepository;
import com.unipi.data.mining.backend.service.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import weka.clusterers.FilteredClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.*;
import weka.filters.unsupervised.attribute.Remove;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class Clustering {

    private final MongoUserRepository mongoUserRepository;

    private CustomUserRepository customUserRepository;

    private final ArrayList<Attribute> attributes = new ArrayList<>();
    private Instances dataset;
    /* using the FilteredClusterer that allows to save the id of the user
    as attribute of the instance without considering it in the KMeans algorithm */
    private FilteredClusterer filteredClusterer;

    public Clustering(MongoUserRepository mongoUserRepository, CustomUserRepository customUserRepository, Utils utils) {
        this.customUserRepository = customUserRepository;
        attributes.add(new Attribute("id",(ArrayList<String>)null));
        attributes.add(new Attribute("neurotic"));
        attributes.add(new Attribute("agreeable"));
        attributes.add(new Attribute("openness"));
        attributes.add(new Attribute("extraversion"));
        attributes.add(new Attribute("conscientiousness"));
        attributes.add(new Attribute("timeSpent"));
        this.mongoUserRepository = mongoUserRepository;
        this.customUserRepository = customUserRepository;
    }

    public void startClustering() {
        System.out.println("Initializing the KMeans Clustering Algorithm....");
        List<MongoUser> mongoUsers = mongoUserRepository.findAllWithSurveyAndCluster();
        Map<String, MongoUser> mongoUserMap = mongoUsers.stream().collect(Collectors.toMap(mongoUser -> mongoUser.getId().toString(), Function.identity()));
        int numInstances = mongoUserMap.size();
        dataset = new Instances("PersonalityDataset", attributes, numInstances);
        double[] values;
        for (Map.Entry<String, MongoUser> mongoUserEntry: mongoUserMap.entrySet()) {
            values = getValues(mongoUserEntry.getValue());
            Instance inst = new DenseInstance(1, values);
            dataset.add(inst);
        }
        buildClusterer();
        updateClusters(mongoUserMap);
    }

    private void buildClusterer() {
        filteredClusterer = new FilteredClusterer();
        try {
            SimpleKMeans clusterer = new SimpleKMeans();
            clusterer.setNumClusters(5);
            clusterer.setDistanceFunction(new EuclideanDistance());
            filteredClusterer.setClusterer(clusterer);
            Remove filter = new Remove();
            // not considering the id as a feature
            filter.setAttributeIndices("first");
            filteredClusterer.setFilter(filter);
            if(dataset.size() != 0)
                filteredClusterer.buildClusterer(dataset);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* training the clusterer with the instances on the database
     and updating the field in the database if changed */
    private void updateClusters(Map<String, MongoUser> mongoUserMap) {
        Enumeration<Instance> instances = dataset.enumerateInstances();
        List<MongoUser> usersToBeUpdated = new ArrayList<>();
        while (instances.hasMoreElements()) {
            try {
                Instance instance = instances.nextElement();
                int cluster = filteredClusterer.clusterInstance(instance) + 1;
                String userId = instance.toString(0);
                MongoUser user = mongoUserMap.get(userId);
                if (user.getCluster() != cluster) {
                    user.setCluster(cluster);
                    //customUserRepository.updateCluster(user);
                    usersToBeUpdated.add(user);
                    if (usersToBeUpdated.size() == 1000) {
                        customUserRepository.bulkUpdateCluster(usersToBeUpdated);
                        usersToBeUpdated.clear();
                        System.out.println("Updating 1000 users");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!usersToBeUpdated.isEmpty()) {
            customUserRepository.bulkUpdateCluster(usersToBeUpdated);
        }
        System.out.println("Users' cluster updated!");
    }

    // function called after registration to perform clustering on the new instance
    public void performClustering(MongoUser user){
        System.out.println("Performing clustering on the new instance..");
        try {
            double[] values;
            values = getValues(user);
            Instance instance = new DenseInstance(1,values);
            instance.setDataset(dataset);
            int cluster = filteredClusterer.clusterInstance(instance) + 1;
            user.setCluster(cluster);
            System.out.println("Cluster assigned: " + cluster);
            dataset.add(instance);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    // function to determine the clusters' average values
    private double[] getValues(MongoUser mongoUser) {
        double[] values = new double[dataset.numAttributes()];
        Survey survey = mongoUser.getSurvey();
        values[0] = dataset.attribute("id").addStringValue(mongoUser.getId().toString());
        values[1] = survey.getNeuroticism();
        values[2] = survey.getAgreeableness();
        values[3] = survey.getOpenness();
        values[4] = survey.getExtraversion();
        values[5] = survey.getConscientiousness();
        values[6] = survey.getTimeSpent();
        return values;
    }
}
