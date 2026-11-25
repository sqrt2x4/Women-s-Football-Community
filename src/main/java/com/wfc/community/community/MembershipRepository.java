package com.wfc.community.community;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    long countByCommunityId(UUID communityId);

    boolean existsByCommunityIdAndUserId(UUID communityId, UUID userId);

    void deleteByCommunityIdAndUserId(UUID communityId, UUID userId);

    List<Membership> findAllByUserId(UUID userId);
}
