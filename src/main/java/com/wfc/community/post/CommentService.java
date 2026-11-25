package com.wfc.community.post;

import com.wfc.community.community.MembershipRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MembershipRepository membershipRepository;

    CommentService(
            CommentRepository commentRepository,
            PostRepository postRepository,
            MembershipRepository membershipRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    List<CommentResponse> findByPost(UUID postId) {
        ensurePostExists(postId);
        return commentRepository.findAllByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    CommentResponse create(UUID postId, CommentRequest request) {
        Post post = ensurePostExists(postId);
        String authorName = request.authorName().trim();
        if (!membershipRepository.existsByCommunityIdAndDisplayNameIgnoreCase(
                post.getCommunity().getId(), authorName)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Join the community before commenting");
        }
        Comment comment = commentRepository.saveAndFlush(
                new Comment(post, authorName, request.content().trim()));
        return CommentResponse.from(comment);
    }

    private Post ensurePostExists(UUID id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }
}
