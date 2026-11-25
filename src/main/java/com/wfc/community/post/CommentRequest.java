package com.wfc.community.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank @Size(max = 40) String authorName,
        @NotBlank @Size(max = 1000) String content) {
}
