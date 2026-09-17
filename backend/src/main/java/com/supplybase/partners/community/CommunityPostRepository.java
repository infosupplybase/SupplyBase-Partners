package com.supplybase.partners.community;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {
    Page<CommunityPost> findAllByStatusOrderByCreatedAtDesc(CommunityPost.Status status, Pageable pageable);
}
