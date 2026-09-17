package com.supplybase.partners.verification;

import com.supplybase.partners.identity.CurrentUser;
import com.supplybase.partners.verification.dto.VerificationStatusResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/verification")
@PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
public class AdminVerificationController {

    private final VerificationService verificationService;
    private final VerificationRequestRepository verificationRequestRepository;
    private final com.supplybase.partners.common.storage.FileStorageService fileStorageService;
    private final CurrentUser currentUser;

    public AdminVerificationController(VerificationService verificationService,
                                        VerificationRequestRepository verificationRequestRepository,
                                        com.supplybase.partners.common.storage.FileStorageService fileStorageService,
                                        CurrentUser currentUser) {
        this.verificationService = verificationService;
        this.verificationRequestRepository = verificationRequestRepository;
        this.fileStorageService = fileStorageService;
        this.currentUser = currentUser;
    }

    @GetMapping("/pending")
    public Page<VerificationStatusResponse> pending(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return verificationRequestRepository
                .findAllByStatus(VerificationRequest.Status.PENDING_REVIEW, PageRequest.of(page, size))
                .map(VerificationStatusResponse::from);
    }

    @PostMapping("/{partnerId}/decision")
    public VerificationStatusResponse decide(@PathVariable Long partnerId, @RequestBody Map<String, Object> body) {
        boolean approve = Boolean.TRUE.equals(body.get("approve"));
        String reason = String.valueOf(body.getOrDefault("reason", ""));
        return VerificationStatusResponse.from(verificationService.decide(partnerId, approve, reason, currentUser.userId()));
    }

    @GetMapping("/{partnerId}/documents/{documentId}/file")
    public ResponseEntity<InputStreamResource> downloadDocument(@PathVariable Long partnerId, @PathVariable Long documentId) {
        IdentityDocument doc = verificationService.requireDocumentOwnedBy(documentId, partnerId);
        var stream = fileStorageService.load(doc.getStorageKey());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .body(new InputStreamResource(stream));
    }
}
