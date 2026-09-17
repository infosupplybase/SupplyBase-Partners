package com.supplybase.partners.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {

    Optional<OtpChallenge> findFirstByPhoneE164OrderByCreatedAtDesc(String phoneE164);

    List<OtpChallenge> findAllByPhoneE164AndCreatedAtAfter(String phoneE164, Instant since);
}
