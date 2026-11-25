package com.wfc.community.community;

import java.time.Instant;
import java.util.UUID;

public record CommunityResponse(
        UUID id, String name, String slug, String description, Instant createdAt, long memberCount) {

    static CommunityResponse from(Community community, long memberCount) {
        return new CommunityResponse(
                community.getId(),
                community.getName(),
                community.getSlug(),
                community.getDescription(),
                community.getCreatedAt(),
                memberCount);
    }
}
