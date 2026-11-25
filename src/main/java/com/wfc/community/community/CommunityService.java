package com.wfc.community.community;

import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class CommunityService {

    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;

    CommunityService(CommunityRepository communityRepository, MembershipRepository membershipRepository) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    List<CommunityResponse> findAll() {
        return communityRepository.findAllByOrderByNameAsc().stream()
                .map(community -> CommunityResponse.from(
                        community, membershipRepository.countByCommunityId(community.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    CommunityResponse findById(UUID id) {
        Community community = findCommunity(id);
        return CommunityResponse.from(community, membershipRepository.countByCommunityId(id));
    }

    @Transactional
    CommunityResponse create(CommunityRequest request) {
        if (communityRepository.findBySlug(request.slug()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A community with this slug already exists");
        }

        try {
            Community community = communityRepository.saveAndFlush(
                    new Community(request.name().trim(), request.slug(), request.description().trim()));
            return CommunityResponse.from(community, 0);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A community with this slug already exists", exception);
        }
    }

    @Transactional
    MembershipResponse join(UUID communityId, MembershipRequest request) {
        Community community = findCommunity(communityId);
        String displayName = request.displayName().trim();
        if (membershipRepository.existsByCommunityIdAndDisplayNameIgnoreCase(communityId, displayName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already joined this community");
        }
        return MembershipResponse.from(membershipRepository.saveAndFlush(new Membership(community, displayName)));
    }

    @Transactional
    void leave(UUID communityId, String displayName) {
        findCommunity(communityId);
        membershipRepository.deleteByCommunityIdAndDisplayNameIgnoreCase(communityId, displayName.trim());
    }

    private Community findCommunity(UUID id) {
        return communityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Community not found"));
    }
}
