package com.wfc.community.post;

import java.time.Instant;
import java.util.UUID;

public record PostResponse(
        UUID id, UUID communityId, String authorName, String content, Instant createdAt) {

    static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(), post.getCommunity().getId(), post.getAuthorName(), post.getContent(), post.getCreatedAt());
    }
}
