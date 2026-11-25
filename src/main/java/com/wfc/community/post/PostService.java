package com.wfc.community.post;

import com.wfc.community.auth.UserAccount;
import com.wfc.community.auth.UserAccountRepository;
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
    private final UserAccountRepository userAccountRepository;

    PostService(
            PostRepository postRepository,
            CommunityRepository communityRepository,
            MembershipRepository membershipRepository,
            UserAccountRepository userAccountRepository) {
        this.postRepository = postRepository;
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    List<PostResponse> findByCommunity(UUID communityId) {
        ensureCommunityExists(communityId);
        return postRepository.findAllByCommunityIdOrderByCreatedAtDesc(communityId).stream()
                .map(PostResponse::from)
                .toList();
    }

    @Transactional
    PostResponse create(UUID communityId, PostRequest request, String email) {
        Community community = ensureCommunityExists(communityId);
        UserAccount user = findUser(email);
        if (!membershipRepository.existsByCommunityIdAndUserId(communityId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Join the community before publishing");
        }
        Post post = postRepository.saveAndFlush(
                new Post(community, user, request.content().trim()));
        return PostResponse.from(post);
    }

    private UserAccount findUser(String email) {
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }

    private Community ensureCommunityExists(UUID id) {
        return communityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Community not found"));
    }
}
