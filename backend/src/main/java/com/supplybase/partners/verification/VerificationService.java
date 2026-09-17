package com.supplybase.partners.verification;

import com.supplybase.partners.common.domain.AuditService;
import com.supplybase.partners.common.domain.BadRequestException;
import com.supplybase.partners.common.domain.ForbiddenException;
import com.supplybase.partners.common.storage.FileStorageService;
import com.supplybase.partners.partner.OnboardingStage;
import com.supplybase.partners.partner.PartnerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

/**
 * A manually approved document is never labelled "Aadhaar verified" to the
 * partner -- see VerificationController's response mapping, which only
 * reports provider + status, and the frontend copy keys off that pairing
 * rather than assuming MANUAL_REVIEW implies government verification.
 */
@Service
public class VerificationService {

    private final VerificationRequestRepository verificationRequestRepository;
    private final IdentityDocumentRepository identityDocumentRepository;
    private final FileStorageService fileStorageService;
    private final PartnerService partnerService;
    private final AuditService auditService;
    private final String providerMode;

    public VerificationService(VerificationRequestRepository verificationRequestRepository,
                                IdentityDocumentRepository identityDocumentRepository,
                                FileStorageService fileStorageService, PartnerService partnerService,
                                AuditService auditService,
                                @Value("${supplybase.providers.identity-verification.mode}") String providerMode) {
        this.verificationRequestRepository = verificationRequestRepository;
        this.identityDocumentRepository = identityDocumentRepository;
        this.fileStorageService = fileStorageService;
        this.partnerService = partnerService;
        this.auditService = auditService;
        this.providerMode = providerMode;
    }

    public VerificationRequest getOrCreate(Long partnerId) {
        return verificationRequestRepository.findByPartnerId(partnerId).orElseGet(() -> {
            VerificationRequest vr = new VerificationRequest();
            vr.setPartnerId(partnerId);
            vr.setProvider("dev_simulator".equalsIgnoreCase(providerMode)
                    ? VerificationRequest.Provider.DEV_SIMULATOR : VerificationRequest.Provider.MANUAL_REVIEW);
            vr.setStatus(VerificationRequest.Status.NOT_STARTED);
            return verificationRequestRepository.save(vr);
        });
    }

    public List<IdentityDocument> documentsFor(Long partnerId) {
        return identityDocumentRepository.findAllByPartnerId(partnerId);
    }

    @Transactional
    public VerificationRequest submit(Long partnerId, IdentityDocument.DocType docType, MultipartFile file) {
        var stored = fileStorageService.store("identity/" + partnerId, file);
        IdentityDocument doc = new IdentityDocument();
        doc.setPartnerId(partnerId);
        doc.setDocType(docType);
        doc.setStorageKey(stored.storageKey());
        doc.setOriginalFilename(stored.originalFilename());
        doc.setContentType(stored.contentType());
        doc.setSizeBytes(stored.sizeBytes());
        identityDocumentRepository.save(doc);

        VerificationRequest vr = getOrCreate(partnerId);
        if (vr.getStatus() == VerificationRequest.Status.APPROVED) {
            throw new BadRequestException("Verification is already approved; changes to verified fields require re-verification (contact support).");
        }
        vr.setSubmittedAt(Instant.now());

        if (vr.getProvider() == VerificationRequest.Provider.DEV_SIMULATOR) {
            vr.setStatus(VerificationRequest.Status.APPROVED);
            vr.setDecidedAt(Instant.now());
            vr.setReason("Auto-approved by DEV_SIMULATOR identity provider (local/demo only).");
            partnerService.advanceStageIfCurrent(partnerId, OnboardingStage.VERIFICATION, OnboardingStage.SCREENING);
        } else {
            vr.setStatus(VerificationRequest.Status.PENDING_REVIEW);
        }
        return verificationRequestRepository.save(vr);
    }

    @Transactional
    public VerificationRequest decide(Long partnerId, boolean approve, String reason, Long reviewerUserId) {
        VerificationRequest vr = verificationRequestRepository.findByPartnerId(partnerId)
                .orElseThrow(() -> new BadRequestException("No verification request for this partner."));
        if (vr.getStatus() != VerificationRequest.Status.PENDING_REVIEW && vr.getStatus() != VerificationRequest.Status.SUBMITTED) {
            throw new BadRequestException("Only a pending verification request can be decided.");
        }
        vr.setStatus(approve ? VerificationRequest.Status.APPROVED : VerificationRequest.Status.REJECTED);
        vr.setReason(reason);
        vr.setReviewerUserId(reviewerUserId);
        vr.setDecidedAt(Instant.now());
        verificationRequestRepository.save(vr);

        auditService.record(reviewerUserId, approve ? "VERIFICATION_APPROVED" : "VERIFICATION_REJECTED",
                "VerificationRequest", vr.getId(), reason);

        if (approve) {
            partnerService.advanceStageIfCurrent(partnerId, OnboardingStage.VERIFICATION, OnboardingStage.SCREENING);
        }
        return vr;
    }

    /** Authorization check for document download: only the owning partner or staff may read it. */
    public IdentityDocument requireDocumentOwnedBy(Long documentId, Long partnerId) {
        IdentityDocument doc = identityDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BadRequestException("Document not found."));
        if (!doc.getPartnerId().equals(partnerId)) {
            throw new ForbiddenException("You do not have access to this document.");
        }
        return doc;
    }
}
