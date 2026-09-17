package com.supplybase.partners.verification;

import com.supplybase.partners.common.domain.BadRequestException;
import com.supplybase.partners.common.storage.FileStorageService;
import com.supplybase.partners.identity.CurrentUser;
import com.supplybase.partners.partner.Partner;
import com.supplybase.partners.partner.PartnerService;
import com.supplybase.partners.verification.dto.IdentityDocumentResponse;
import com.supplybase.partners.verification.dto.VerificationStatusResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/partners/me/verification")
@PreAuthorize("hasRole('PARTNER')")
public class VerificationController {

    private final VerificationService verificationService;
    private final PartnerService partnerService;
    private final CurrentUser currentUser;
    private final FileStorageService fileStorageService;

    public VerificationController(VerificationService verificationService, PartnerService partnerService,
                                   CurrentUser currentUser, FileStorageService fileStorageService) {
        this.verificationService = verificationService;
        this.partnerService = partnerService;
        this.currentUser = currentUser;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public VerificationStatusResponse status() {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return VerificationStatusResponse.from(verificationService.getOrCreate(partner.getId()));
    }

    @PostMapping
    public VerificationStatusResponse submit(@RequestParam("docType") IdentityDocument.DocType docType,
                                              @RequestParam("file") MultipartFile file) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return VerificationStatusResponse.from(verificationService.submit(partner.getId(), docType, file));
    }

    @GetMapping("/documents")
    public List<IdentityDocumentResponse> documents() {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return verificationService.documentsFor(partner.getId()).stream().map(IdentityDocumentResponse::from).toList();
    }

    @GetMapping("/documents/{documentId}/file")
    public ResponseEntity<InputStreamResource> downloadDocument(@PathVariable Long documentId) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        IdentityDocument doc = verificationService.requireDocumentOwnedBy(documentId, partner.getId());
        var stream = fileStorageService.load(doc.getStorageKey());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .header("Content-Disposition", "inline; filename=\"" + sanitize(doc.getOriginalFilename()) + "\"")
                .body(new InputStreamResource(stream));
    }

    private String sanitize(String filename) {
        String cleaned = filename.replaceAll("[\\r\\n\"]", "_");
        if (cleaned.isBlank()) {
            throw new BadRequestException("Invalid filename.");
        }
        return cleaned;
    }
}
