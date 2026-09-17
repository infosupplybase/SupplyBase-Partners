package com.supplybase.partners.verification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, Long> {
    Optional<VerificationRequest> findByPartnerId(Long partnerId);

    Page<VerificationRequest> findAllByStatus(VerificationRequest.Status status, Pageable pageable);
}
