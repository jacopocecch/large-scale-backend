package com.unipi.data.mining.backend.dtos;

import java.io.Serializable;

public class Neo4jSongDto implements Serializable {

    private String id;
    private String name;
    private String authors;
    private String album;

    public Neo4jSongDto() {
    }

    public Neo4jSongDto(String id, String name, String authors, String album) {
        this.id = id;
        this.name = name;
        this.authors = authors;
        this.album = album;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }
}
