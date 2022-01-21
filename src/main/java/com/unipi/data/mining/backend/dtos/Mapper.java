package com.unipi.data.mining.backend.dtos;

import com.unipi.data.mining.backend.data.Login;
import com.unipi.data.mining.backend.data.Survey;
import com.unipi.data.mining.backend.entities.mongodb.MongoSong;
import com.unipi.data.mining.backend.entities.mongodb.MongoUser;
import com.unipi.data.mining.backend.entities.neo4j.Neo4jSong;
import com.unipi.data.mining.backend.entities.neo4j.Neo4jUser;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public record Mapper(ModelMapper modelMapper) {

    public UserDto mongoUserToUserDto(MongoUser mongoUser) {

        return modelMapper.map(mongoUser, UserDto.class);
    }

    public List<UserDto> mongoUsersToUsersDto(List<MongoUser> mongoUsers) {

        return mongoUsers.stream().map(this::mongoUserToUserDto).toList();
    }

    public MongoUser userDtoToMongoUser(UserDto userDto) {

        return modelMapper.map(userDto, MongoUser.class);
    }

    public Login loginDtoToLogin(LoginDto loginDto) {

        return modelMapper.map(loginDto, Login.class);
    }

    public SurveyDto surveyToSurveyDto(Survey survey) {

        return modelMapper.map(survey, SurveyDto.class);
    }

    public Neo4jUserDto neo4jUserToNeo4jUserDto(Neo4jUser neo4jUser) {

        modelMapper.typeMap(Neo4jUser.class, Neo4jUserDto.class).addMapping(
                Neo4jUser::getMongoId,
                Neo4jUserDto::setId
        );

        return modelMapper.map(neo4jUser, Neo4jUserDto.class);
    }

    public List<Neo4jUserDto> neo4jUsersToNeo4jUsersDto(List<Neo4jUser> neo4jUsers) {

        return neo4jUsers.stream().map(this::neo4jUserToNeo4jUserDto).toList();
    }

    public SongDto mongoSongToSongDto(MongoSong song) {

        return modelMapper.map(song, SongDto.class);
    }

    public List<SongDto> mongoSongsToSongsDto(List<MongoSong> songs) {

        return songs.stream().map(this::mongoSongToSongDto).toList();
    }

    public MongoSong songDtoToMongoSong(SongDto songDto) {

        return modelMapper.map(songDto, MongoSong.class);
    }

    public Neo4jSongDto neo4jSongToNeo4jSongDto(Neo4jSong neo4jSong) {

        modelMapper.typeMap(Neo4jSong.class, Neo4jSongDto.class).addMapping(
                Neo4jSong::getMongoId,
                Neo4jSongDto::setId
        );

        return modelMapper.map(neo4jSong, Neo4jSongDto.class);
    }

    public List<Neo4jSongDto> neo4jSongsToNeo4jSongsDto(List<Neo4jSong> neo4jSongs) {

        return neo4jSongs.stream().map(this::neo4jSongToNeo4jSongDto).toList();
    }
}
