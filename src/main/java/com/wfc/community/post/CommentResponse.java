package com.wfc.community.post;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(UUID id, String authorName, String content, Instant createdAt) {

    static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getAuthorName(), comment.getContent(), comment.getCreatedAt());
    }
}
