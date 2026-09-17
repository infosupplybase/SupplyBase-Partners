package com.supplybase.partners.admin;

import com.supplybase.partners.common.domain.AuditLog;
import com.supplybase.partners.common.domain.AuditLogRepository;
import com.supplybase.partners.jobs.ServiceRequest;
import com.supplybase.partners.jobs.ServiceRequestRepository;
import com.supplybase.partners.kit.KitOrder;
import com.supplybase.partners.kit.KitOrderRepository;
import com.supplybase.partners.partner.Partner;
import com.supplybase.partners.partner.PartnerRepository;
import com.supplybase.partners.common.web.PageResponse;
import com.supplybase.partners.verification.VerificationRequest;
import com.supplybase.partners.verification.VerificationRequestRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN','REVIEWER','TRAINER','FINANCE')")
public class AdminDashboardController {

    private final PartnerRepository partnerRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final KitOrderRepository kitOrderRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminDashboardController(PartnerRepository partnerRepository, VerificationRequestRepository verificationRequestRepository,
                                     KitOrderRepository kitOrderRepository, ServiceRequestRepository serviceRequestRepository,
                                     AuditLogRepository auditLogRepository) {
        this.partnerRepository = partnerRepository;
        this.verificationRequestRepository = verificationRequestRepository;
        this.kitOrderRepository = kitOrderRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public record DashboardCounts(long totalPartners, long activePartners, long pendingVerifications,
                                   long openJobs, long kitOrdersProcessing) {}

    @GetMapping("/dashboard")
    public DashboardCounts dashboard() {
        long total = partnerRepository.count();
        long active = partnerRepository.findAll().stream().filter(p -> p.getActivationStatus() == Partner.ActivationStatus.ACTIVE).count();
        long pendingVerifications = verificationRequestRepository
                .findAllByStatus(VerificationRequest.Status.PENDING_REVIEW, PageRequest.of(0, 1)).getTotalElements();
        long openJobs = serviceRequestRepository.findAll().stream().filter(r -> r.getStatus() == ServiceRequest.Status.OPEN).count();
        long kitProcessing = kitOrderRepository.findAll().stream().filter(o -> o.getStatus() == KitOrder.Status.PROCESSING).count();
        return new DashboardCounts(total, active, pendingVerifications, openJobs, kitProcessing);
    }

    @GetMapping("/audit-logs")
    public PageResponse<AuditLog> auditLogs(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return PageResponse.from(auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)));
    }
}
