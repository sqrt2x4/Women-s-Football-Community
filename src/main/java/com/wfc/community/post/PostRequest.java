package com.wfc.community.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @NotBlank @Size(max = 40) String authorName,
        @NotBlank @Size(max = 2000) String content) {
}
