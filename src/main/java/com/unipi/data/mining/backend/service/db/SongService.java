package com.unipi.data.mining.backend.service.db;

import com.unipi.data.mining.backend.entities.mongodb.Comment;
import com.unipi.data.mining.backend.entities.mongodb.CommentSubset;
import com.unipi.data.mining.backend.entities.mongodb.MongoSong;
import com.unipi.data.mining.backend.entities.mongodb.MongoUser;
import com.unipi.data.mining.backend.entities.neo4j.Neo4jSong;
import com.unipi.data.mining.backend.entities.neo4j.Neo4jUser;
import com.unipi.data.mining.backend.service.exceptions.DbException;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class SongService extends EntityService{


    public MongoSong getMongoSongById(String id) {

        Optional<MongoSong> mongoSong = mongoSongRepository.findById(new ObjectId(id));

        if (mongoSong.isEmpty()){
            throw new DbException("No Song found with id " + id);
        }

        return mongoSong.get();
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

        MongoSong song = mongoSongRepository.save(mongoSong);
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

    public void likeSong(String fromId, String toId, int like) {

        neo4jSongDao.updateLikeRelationship(fromId, toId, like);
    }

    public List<Comment> getSongComments(String id) {

        return customCommentRepository.getSongComments(new ObjectId(id));
    }

    public Comment commentSong(String songId, Comment comment) {

        Optional<MongoUser> mongoUser = mongoUserRepository.findById(comment.getUserId());

        if (mongoUser.isEmpty()) {
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

}
