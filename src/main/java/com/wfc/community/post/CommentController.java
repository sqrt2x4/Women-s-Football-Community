package com.wfc.community.post;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
class CommentController {

    private final CommentService commentService;

    CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    List<CommentResponse> findByPost(@PathVariable UUID postId) {
        return commentService.findByPost(postId);
    }

    @PostMapping
    ResponseEntity<CommentResponse> create(
            @PathVariable UUID postId, @Valid @RequestBody CommentRequest request) {
        CommentResponse created = commentService.create(postId, request);
        return ResponseEntity.created(URI.create("/api/posts/" + postId + "/comments/" + created.id()))
                .body(created);
    }
}
