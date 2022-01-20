package com.unipi.data.mining.backend.entities.mongodb;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.Objects;

@Document("comment")
public class Comment {

    //id, id canzone, id utente, testo
    @MongoId
    private ObjectId id;
    @Field("user_id")
    private ObjectId userId;
    @Field("song_id")
    private ObjectId songId;
    private String text;

    public Comment() {
    }

    public Comment(ObjectId id, ObjectId userId, ObjectId songId, String text) {
        this.id = id;
        this.userId = userId;
        this.songId = songId;
        this.text = text;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ObjectId getUserId() {
        return userId;
    }

    public void setUserId(ObjectId userId) {
        this.userId = userId;
    }

    public ObjectId getSongId() {
        return songId;
    }

    public void setSongId(ObjectId songId) {
        this.songId = songId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", userId=" + userId +
                ", songId=" + songId +
                ", text='" + text + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comment comment = (Comment) o;
        return id.equals(comment.id) && userId.equals(comment.userId) && songId.equals(comment.songId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, songId);
    }
}
