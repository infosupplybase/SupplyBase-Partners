package com.supplybase.partners.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {
    List<CommunityComment> findAllByPostIdAndStatusOrderByCreatedAtAsc(Long postId, CommunityComment.Status status);
}
