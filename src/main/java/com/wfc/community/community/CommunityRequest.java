package com.wfc.community.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CommunityRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank
                @Size(max = 80)
                @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "must be a lowercase URL-friendly slug")
                String slug,
        @NotBlank @Size(max = 500) String description) {
}
