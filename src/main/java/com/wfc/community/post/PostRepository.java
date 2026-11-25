package com.wfc.community.post;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PostRepository extends JpaRepository<Post, UUID> {

    List<Post> findAllByCommunityIdOrderByCreatedAtDesc(UUID communityId);
}
