package com.wfc.community.community;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    long countByCommunityId(UUID communityId);

    boolean existsByCommunityIdAndDisplayNameIgnoreCase(UUID communityId, String displayName);

    void deleteByCommunityIdAndDisplayNameIgnoreCase(UUID communityId, String displayName);
}
