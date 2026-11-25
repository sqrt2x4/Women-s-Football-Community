package com.wfc.community.community;

import java.time.Instant;
import java.util.UUID;

public record MembershipResponse(UUID id, String displayName, Instant joinedAt) {

    static MembershipResponse from(Membership membership) {
        return new MembershipResponse(membership.getId(), membership.getDisplayName(), membership.getJoinedAt());
    }
}
