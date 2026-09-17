package com.supplybase.partners.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IdentityDocumentRepository extends JpaRepository<IdentityDocument, Long> {
    List<IdentityDocument> findAllByPartnerId(Long partnerId);
}
