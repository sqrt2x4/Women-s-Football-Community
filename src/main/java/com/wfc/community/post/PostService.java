package com.wfc.community.post;

import com.wfc.community.community.Community;
import com.wfc.community.community.CommunityRepository;
import com.wfc.community.community.MembershipRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class PostService {

    private final PostRepository postRepository;
    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;

    PostService(
            PostRepository postRepository,
            CommunityRepository communityRepository,
            MembershipRepository membershipRepository) {
        this.postRepository = postRepository;
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    List<PostResponse> findByCommunity(UUID communityId) {
        ensureCommunityExists(communityId);
        return postRepository.findAllByCommunityIdOrderByCreatedAtDesc(communityId).stream()
                .map(PostResponse::from)
                .toList();
    }

    @Transactional
    PostResponse create(UUID communityId, PostRequest request) {
        Community community = ensureCommunityExists(communityId);
        String authorName = request.authorName().trim();
        if (!membershipRepository.existsByCommunityIdAndDisplayNameIgnoreCase(communityId, authorName)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Join the community before publishing");
        }
        Post post = postRepository.saveAndFlush(
                new Post(community, authorName, request.content().trim()));
        return PostResponse.from(post);
    }

    private Community ensureCommunityExists(UUID id) {
        return communityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Community not found"));
    }
}
