package com.supplybase.partners.verification.dto;

import com.supplybase.partners.verification.IdentityDocument;

import java.time.Instant;

public record IdentityDocumentResponse(Long id, String docType, String originalFilename, Instant uploadedAt) {
    public static IdentityDocumentResponse from(IdentityDocument d) {
        return new IdentityDocumentResponse(d.getId(), d.getDocType().name(), d.getOriginalFilename(), d.getUploadedAt());
    }
}
