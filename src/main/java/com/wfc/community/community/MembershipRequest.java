package com.wfc.community.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MembershipRequest(@NotBlank @Size(max = 40) String displayName) {
}
