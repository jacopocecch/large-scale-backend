package com.unipi.data.mining.backend.service.db;

import com.unipi.data.mining.backend.data.Distance;
import com.unipi.data.mining.backend.data.Survey;
import com.unipi.data.mining.backend.entities.mongodb.MongoSong;
import com.unipi.data.mining.backend.entities.mongodb.MongoUser;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

@Service
public class Neo4jService extends EntityService{

    private static final String CSV_FILE_NAME_LIKES = "/Users/jacopo/IdeaProjects/large-scale-project/large-scale-backend/song_preference.csv";
    private static final String CSV_FILE_NAME_SIMILARITIES = "/Users/jacopo/IdeaProjects/large-scale-project/large-scale-backend/similarities.csv";

    public void addLikesHeuristic() {

        List<String[]> csvRelationships = new ArrayList<>();
        List<MongoUser> mongoUsers = mongoUserRepository.findAllWithSurveyAndCluster();
        List<MongoUser> extroverseUsers = new ArrayList<>();
        List<MongoUser> agreeableUsers = new ArrayList<>();
        List<MongoUser> conscentiousUsers = new ArrayList<>();
        List<MongoUser> neuroticUsers = new ArrayList<>();
        List<MongoUser> openUsers = new ArrayList<>();

        for (MongoUser mongoUser: mongoUsers) {

            switch (getBiggestPersonalityTrait(mongoUser)) {
                case 0 -> extroverseUsers.add(mongoUser);
                case 1 -> agreeableUsers.add(mongoUser);
                case 2 -> conscentiousUsers.add(mongoUser);
                case 3 -> neuroticUsers.add(mongoUser);
                case 4 -> openUsers.add(mongoUser);
            }
        }

        addRelationships(openUsers, "liveness", csvRelationships, Sort.Direction.DESC, Sort.Direction.ASC);
        addRelationships(conscentiousUsers, "instrumentalness", csvRelationships, Sort.Direction.ASC, Sort.Direction.DESC);
        addRelationships(extroverseUsers, "energy", csvRelationships, Sort.Direction.ASC, Sort.Direction.DESC);
        addRelationships(agreeableUsers, "tempo", csvRelationships, Sort.Direction.ASC, Sort.Direction.DESC);
        addRelationships(neuroticUsers, "instrumentalness", csvRelationships, Sort.Direction.ASC, Sort.Direction.DESC);

        File csvOutputFile = new File(CSV_FILE_NAME_LIKES);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            csvRelationships.stream()
                    .map(this::convertToCSV)
                    .forEach(pw::println);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void addRelationships(List<MongoUser> users, String type, List<String[]> csvRelationships, Sort.Direction bestDirection, Sort.Direction worstDirection) {

        List<MongoSong> bestTypeSongs = customSongRepository.findSongsSortBy(type, bestDirection);
        List<MongoSong> worstTypeSongs = customSongRepository.findSongsSortBy(type, worstDirection);

        for (MongoUser mongoUser: users) {

            int NUM_BEST_SONGS = 10;
            List<MongoSong> bestSongs = extractKRandom(bestTypeSongs, NUM_BEST_SONGS);

            for (MongoSong song: bestSongs) {
                csvRelationships.add(new String[] {mongoUser.getId().toString(), song.getId().toString(), "1"});
            }

            int NUM_WORST_SONGS = 5;
            List<MongoSong> worstSongs = extractKRandom(worstTypeSongs, NUM_WORST_SONGS);
            for (MongoSong song: worstSongs) {
                csvRelationships.add(new String[] {mongoUser.getId().toString(), song.getId().toString(), "-1"});
            }
        }
    }

    private String convertToCSV(String[] data) {
        return String.join(",", data);
    }

    private List<MongoSong> extractKRandom(List<MongoSong> songs, int k) {

        Random random = new Random();
        List<MongoSong> extractedSongs = new ArrayList<>(k);

        for (int i = 0; i < k; i++) {
            int randomIndex;
            MongoSong song;
            do {
                randomIndex = random.nextInt(songs.size());
                song = songs.get(randomIndex);
            } while (extractedSongs.contains(song));

            extractedSongs.add(song);
        }

        return extractedSongs;
    }

    private int getBiggestPersonalityTrait(MongoUser user) {

        double[] personalityValues = new double[] {user.getExtraversion(), user.getAgreeableness(), user.getConscientiousness(), user.getNeuroticism(), user.getOpenness()};

        int maxAt = 0;

        for (int i = 0; i < personalityValues.length; i++) {
            maxAt = personalityValues[i] > personalityValues[maxAt] ? i : maxAt;
        }

        return maxAt;
    }

    public void generateSimilarities() {

        List<String[]> csvRelationships = new ArrayList<>();

        for (int i = 1; i < 6; i++) {

            List<MongoUser> users = mongoUserRepository.findMongoUsersByCluster(i);
            Map<String, List<Distance>> stringListMap = new HashMap<>();

            for (MongoUser user: users) {

                Survey userSurvey = new Survey(user);

                List<Distance> distances = new ArrayList<>();

                for (MongoUser toUser: users) {
                    if (user.getId().toString().equals(toUser.getId().toString())) continue;
                    if (!utils.areSurveyValuesCorrect(toUser)) continue;
                    Survey toUserSurvey = new Survey(toUser);
                    distances.add(new Distance(toUser.getId().toString(), utils.getDistance(userSurvey, toUserSurvey)));
                }
                distances.sort(Comparator.comparingDouble(Distance::getDistance));

                for (int j = 0; j < 10; j++) {
                    Distance distance = distances.get(j);
                    double weight;
                    if (distance.getDistance() == 0) {
                        weight = 100;
                    } else {
                        weight = Math.round(1/Math.pow(distance.getDistance(), 2)* 100.00) / 100.00;
                    }
                    csvRelationships.add(new String[] {user.getId().toString(), distance.getUserId(), String.valueOf(weight)});
                }
            }
        }

        File csvOutputFile = new File(CSV_FILE_NAME_SIMILARITIES);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            csvRelationships.stream()
                    .map(this::convertToCSV)
                    .forEach(pw::println);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
