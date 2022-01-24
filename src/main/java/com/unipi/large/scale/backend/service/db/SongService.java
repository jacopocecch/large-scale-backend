package com.unipi.large.scale.backend.service.db;

import com.unipi.large.scale.backend.data.aggregations.Album;
import com.unipi.large.scale.backend.data.aggregations.AverageMusicFeatures;
import com.unipi.large.scale.backend.data.aggregations.Id;
import com.unipi.large.scale.backend.entities.mongodb.*;
import com.unipi.large.scale.backend.entities.neo4j.Neo4jSong;
import com.unipi.large.scale.backend.entities.neo4j.Neo4jUser;
import com.unipi.large.scale.backend.service.exceptions.DbException;
import org.bson.types.ObjectId;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class SongService extends EntityService{


    public MongoSong getMongoSongById(String id) {

        MongoSong mongoSong = customSongRepository.findById(new ObjectId(id));

        if (mongoSong == null){
            throw new DbException("No Song found with id " + id);
        }

        return mongoSong;
    }

    @Transactional
    public void deleteSongById(String id) {

        ObjectId objectId = new ObjectId(id);

        if (customSongRepository.deleteSongById(objectId)) throw new DbException("Unable to delete mongodb user of id " + id);

        neo4jSongDao.deleteByMongoId(id);

        if (neo4jSongDao.getByMongoId(id).isPresent()) throw new DbException("Unable to delete neo4j song of id " + id);

        customCommentRepository.deleteSongComments(objectId);
    }

    public MongoSong createSong(MongoSong mongoSong) {

        MongoSong song = customSongRepository.createSong(mongoSong);
        ObjectId id = song.getId();

        Neo4jSong neo4jSong = new Neo4jSong(id.toString(), song.getName(), song.getArtists().stream().toList(), song.getAlbum());

        neo4jSongDao.createSong(neo4jSong);

        return song;
    }


    public Neo4jSong getNeo4jSongByMongoId(String id) {

        Optional<Neo4jSong> optionalNeo4jSong = neo4jSongDao.getByMongoId(id);

        if (optionalNeo4jSong.isEmpty()) throw new DbException("Unable to find song with id: " + id + " on neo4j");

        return optionalNeo4jSong.get();
    }

    public List<MongoSong> searchSongsByName(String name) {

        return customSongRepository.getSongsStartingWithName(name);
    }

    public List<Neo4jSong> getRecommendedSongs(String id) {

        Optional<Neo4jUser> optionalNeo4jUser = neo4jUserDao.getByMongoId(id);
        if (optionalNeo4jUser.isEmpty()) throw new DbException("Unable to find user with id: " + id + " on neo4j");

        return neo4jSongDao.getRecommendedSongs(id);
    }

    public void likeSong(String userId, String songId, int like) {

        Optional<Neo4jUser> optionalNeo4jUser = neo4jUserDao.getByMongoId(userId);
        if (optionalNeo4jUser.isEmpty()) throw new DbException("Unable to find user with id: " + userId + " on neo4j");

        getNeo4jSongByMongoId(songId);

        Optional<Record> currentLike = neo4jSongDao.getLikeValue(userId, songId);

        if (currentLike.isEmpty() || currentLike.get().get(0).asInt() != like){
            neo4jSongDao.updateLikeRelationship(userId, songId, like);
        } else {
            throw new DbException("Song already liked by the user");
        }

        MongoSong song = customSongRepository.findById(new ObjectId(songId));
        MongoUser user = customUserRepository.findById(new ObjectId(userId));

        List<Like> likeList = song.getLikes();

        for (Like likeElement: likeList) {

            if (likeElement.getCluster() == user.getCluster()){

                if (like == 1) {
                    likeElement.setNumLikes(likeElement.getNumLikes() + 1);
                } else {
                    likeElement.setNumUnlikes(likeElement.getNumUnlikes() + 1);
                }

                if (currentLike.isPresent()) {
                    if (like == 1) {
                        likeElement.setNumUnlikes(likeElement.getNumUnlikes() - 1);
                    } else {
                        likeElement.setNumLikes(likeElement.getNumLikes() - 1);
                    }
                }
            }
        }

        int predominantCluster = 0;
        int maxLikes = 0;

        for (Like likeElement: likeList) {

            int totalLikes = likeElement.getNumLikes() - likeElement.getNumUnlikes();

            if (totalLikes > maxLikes) {
                predominantCluster = likeElement.getCluster();
                maxLikes = totalLikes;
            }
        }

        song.setCluster(predominantCluster);

        if (customSongRepository.updateLikes(song)){
            // gestione
        }
    }

    public List<Comment> getSongComments(String id) {

        return customCommentRepository.getSongComments(new ObjectId(id));
    }

    public Comment commentSong(String songId, Comment comment) {

        MongoUser mongoUser = customUserRepository.findById(comment.getUserId());

        if (mongoUser == null) {
            throw new DbException("No User found with id " + comment.getUserId().toString());
        }

        MongoSong song = getMongoSongById(songId);

        Comment insertedComment = customCommentRepository.insertComment(comment);

        ObjectId id = insertedComment.getId();

        if (id == null) {
            throw new DbException("Unable to create new comment");
        }

        List<CommentSubset> commentSubsets = song.getComments();
        CommentSubset newComment = new CommentSubset(comment.getId(), comment.getUserId(), comment.getName(), comment.getSurname(), comment.getText(), LocalDate.now());

        if (commentSubsets == null) {
            commentSubsets = new ArrayList<>();
        }
        Collections.sort(commentSubsets);
        commentSubsets.add(0, newComment);
        if (commentSubsets.size() > 10){
            commentSubsets.remove(commentSubsets.size() - 1);
        }

        song.setComments(commentSubsets);

        if (customSongRepository.updateComments(song) == 0) {
            throw new DbException("Unable to update song's comments");
        }

        return insertedComment;
    }

    public List<Album> getClusterKHighestRatedAlbums(int cluster, int k) {

        return customSongRepository.getClusterKHighestRatedAlbums(cluster, k);
    }

    public Id getMostDanceableCluster() {

        return customSongRepository.getMostDanceableCluster();
    }

    public List<AverageMusicFeatures> getAverageMusicFeaturesByCluster() {

        return customSongRepository.getAverageMusicFeaturesByCluster();
    }

}
