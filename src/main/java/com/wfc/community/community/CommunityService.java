package com.wfc.community.community;

import com.wfc.community.auth.UserAccount;
import com.wfc.community.auth.UserAccountRepository;
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
    private final UserAccountRepository userAccountRepository;

    CommunityService(
            CommunityRepository communityRepository,
            MembershipRepository membershipRepository,
            UserAccountRepository userAccountRepository) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.userAccountRepository = userAccountRepository;
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
    MembershipResponse join(UUID communityId, String email) {
        Community community = findCommunity(communityId);
        UserAccount user = findUser(email);
        if (membershipRepository.existsByCommunityIdAndUserId(communityId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already joined this community");
        }
        return MembershipResponse.from(membershipRepository.saveAndFlush(new Membership(community, user)));
    }

    @Transactional
    void leave(UUID communityId, String email) {
        findCommunity(communityId);
        UserAccount user = findUser(email);
        membershipRepository.deleteByCommunityIdAndUserId(communityId, user.getId());
    }

    @Transactional(readOnly = true)
    List<UUID> findMembershipIds(String email) {
        UserAccount user = findUser(email);
        return membershipRepository.findAllByUserId(user.getId()).stream()
                .map(membership -> membership.getCommunity().getId())
                .toList();
    }

    private Community findCommunity(UUID id) {
        return communityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Community not found"));
    }

    private UserAccount findUser(String email) {
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }
}
