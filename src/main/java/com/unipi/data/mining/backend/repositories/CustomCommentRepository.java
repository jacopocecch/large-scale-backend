package com.unipi.data.mining.backend.repositories;

import com.unipi.data.mining.backend.entities.mongodb.Comment;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class CustomCommentRepository extends CustomRepository{

    public List<Comment> bulkInsertComments(List<Comment> comments) {

        return mongoTemplate.insert(comments, Comment.class).stream().toList();
    }
}
