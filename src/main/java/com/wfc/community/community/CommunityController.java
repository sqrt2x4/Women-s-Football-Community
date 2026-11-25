package com.wfc.community.community;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
@RequestMapping("/api/communities")
class CommunityController {

    private final CommunityService communityService;

    CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping
    List<CommunityResponse> findAll() {
        return communityService.findAll();
    }

    @GetMapping("/{id}")
    CommunityResponse findById(@PathVariable UUID id) {
        return communityService.findById(id);
    }

    @PostMapping
    ResponseEntity<CommunityResponse> create(@Valid @RequestBody CommunityRequest request) {
        CommunityResponse created = communityService.create(request);
        return ResponseEntity.created(URI.create("/api/communities/" + created.id())).body(created);
    }

    @PostMapping("/{id}/memberships")
    ResponseEntity<MembershipResponse> join(
            @PathVariable UUID id, @AuthenticationPrincipal UserDetails principal) {
        MembershipResponse membership = communityService.join(id, principal.getUsername());
        return ResponseEntity.created(URI.create("/api/communities/" + id + "/memberships/" + membership.id()))
                .body(membership);
    }

    @DeleteMapping("/{id}/memberships")
    ResponseEntity<Void> leave(@PathVariable UUID id, @AuthenticationPrincipal UserDetails principal) {
        communityService.leave(id, principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/memberships/mine")
    List<UUID> findMyMemberships(@AuthenticationPrincipal UserDetails principal) {
        return communityService.findMembershipIds(principal.getUsername());
    }
}
