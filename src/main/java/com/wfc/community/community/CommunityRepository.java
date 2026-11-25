package com.wfc.community.community;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityRepository extends JpaRepository<Community, UUID> {

    List<Community> findAllByOrderByNameAsc();

    Optional<Community> findBySlug(String slug);
}
