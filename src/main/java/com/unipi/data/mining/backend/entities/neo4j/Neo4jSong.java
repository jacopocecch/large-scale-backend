package com.unipi.data.mining.backend.entities.neo4j;

import java.util.List;
import java.util.Objects;

public class Neo4jSong {

    private String mongoId;
    private String name;
    private String authors;
    private String album;

    public Neo4jSong() {
    }


    public Neo4jSong(String mongoId, String name, String authors, String album) {
        this.mongoId = mongoId;
        this.name = name;
        this.authors = authors;
        this.album = album;
    }

    public Neo4jSong(String mongoId, String name, List<String> authors, String album) {
        this.mongoId = mongoId;
        this.name = name;
        this.authors = String.join(", ", authors);
        this.album = album;
    }

    public String getMongoId() {
        return mongoId;
    }

    public void setMongoId(String mongoId) {
        this.mongoId = mongoId;
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

    public void setAuthors(List<String> authors) {
        this.authors = String.join(", ", authors);
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    @Override
    public String toString() {
        return "Neo4jSong{" +
                "mongoId=" + mongoId +
                ", name='" + name + '\'' +
                ", authors=" + authors +
                ", album='" + album + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Neo4jSong neo4jSong = (Neo4jSong) o;
        return mongoId.equals(neo4jSong.mongoId) && name.equals(neo4jSong.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mongoId, name);
    }
}
