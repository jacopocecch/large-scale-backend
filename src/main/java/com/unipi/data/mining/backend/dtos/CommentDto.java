package com.unipi.data.mining.backend.dtos;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.bson.types.ObjectId;

import java.io.Serializable;

public class CommentDto implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId userId;
    private String name;
    private String surname;
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId songId;
    private String text;

    public CommentDto() {
    }

    public CommentDto(ObjectId id, ObjectId userId, String name, String surname, ObjectId songId, String text) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.surname = surname;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }
}