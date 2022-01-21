package com.unipi.data.mining.backend.entities.neo4j;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Neo4jUser {

    private String mongoId;
    private String firstName;
    private String lastName;
    private String username;
    private String country;
    private String image;
    private FriendRequest friendRequest;

    public Neo4jUser() {
    }

    public Neo4jUser(String mongoId, String firstName, String lastName, String username, String country, String image) {
        this.mongoId = mongoId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.country = country;
        this.image = image;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public FriendRequest getFriendRequest() {
        return friendRequest;
    }

    public void setFriendRequest(FriendRequest friendRequest) {
        this.friendRequest = friendRequest;
    }

    @Override
    public String toString() {
        return "Neo4jUser{" +
                "mongoId='" + mongoId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", country='" + country + '\'' +
                ", image='" + image + '\'' +
                ", friendRequest=" + friendRequest +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Neo4jUser neo4jUser = (Neo4jUser) o;
        return mongoId.equals(neo4jUser.mongoId) && firstName.equals(neo4jUser.firstName) && Objects.equals(country, neo4jUser.country) && Objects.equals(image, neo4jUser.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mongoId, firstName, country, image);
    }

    public String getMongoId() {
        return mongoId;
    }

    public void setMongoId(String mongoId) {
        this.mongoId = mongoId;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
