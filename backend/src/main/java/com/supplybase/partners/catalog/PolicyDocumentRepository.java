package com.supplybase.partners.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, Long> {
    Optional<PolicyDocument> findByTypeAndCurrentTrue(PolicyDocument.PolicyType type);

    List<PolicyDocument> findAllByCurrentTrue();
}
