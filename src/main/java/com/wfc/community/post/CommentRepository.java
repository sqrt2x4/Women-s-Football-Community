package com.wfc.community.post;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findAllByPostIdOrderByCreatedAtAsc(UUID postId);
}
