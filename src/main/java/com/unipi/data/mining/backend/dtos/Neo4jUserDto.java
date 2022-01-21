package com.unipi.data.mining.backend.dtos;

import java.io.Serializable;

public class Neo4jUserDto implements Serializable {

    private String id;
    private String firstName;
    private String lastName;
    private String username;
    private String country;
    private String image;
    private FriendRequestDto friendRequest;

    public Neo4jUserDto() {
    }

    public Neo4jUserDto(String id, String firstName, String lastName, String username, String country, String image, FriendRequestDto friendRequest) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.country = country;
        this.image = image;
        this.friendRequest = friendRequest;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public FriendRequestDto getFriendRequest() {
        return friendRequest;
    }

    public void setFriendRequest(FriendRequestDto friendRequest) {
        this.friendRequest = friendRequest;
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
