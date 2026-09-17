package com.supplybase.partners.jobs;

import com.supplybase.partners.common.domain.BadRequestException;
import com.supplybase.partners.common.domain.ConflictException;
import com.supplybase.partners.common.domain.ForbiddenException;
import com.supplybase.partners.common.domain.NotFoundException;
import com.supplybase.partners.common.storage.FileStorageService;
import com.supplybase.partners.money.MoneyService;
import com.supplybase.partners.notification.NotificationOutbox;
import com.supplybase.partners.notification.NotificationService;
import com.supplybase.partners.partner.Partner;
import com.supplybase.partners.partner.PartnerCoverageRepository;
import com.supplybase.partners.partner.PartnerRepository;
import com.supplybase.partners.partner.PartnerSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Job matching composes several single-domain repository queries rather than
 * one cross-package JPQL join (this codebase deliberately keeps packages
 * loosely coupled via plain foreign-key Long fields, not JPA associations
 * across package boundaries). Acceptance itself is a single conditional
 * UPDATE (see ServiceRequestRepository#tryAssign) so two concurrent accepts
 * on the same job can never both win.
 */
@Service
public class JobsService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final JobStatusHistoryRepository historyRepository;
    private final JobProofMediaRepository proofMediaRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerCoverageRepository partnerCoverageRepository;
    private final PartnerSkillRepository partnerSkillRepository;
    private final com.supplybase.partners.catalog.ServiceRepository serviceRepository;
    private final MoneyService moneyService;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    public JobsService(ServiceRequestRepository serviceRequestRepository, JobStatusHistoryRepository historyRepository,
                        JobProofMediaRepository proofMediaRepository, CustomerAddressRepository customerAddressRepository,
                        PartnerRepository partnerRepository, PartnerCoverageRepository partnerCoverageRepository,
                        PartnerSkillRepository partnerSkillRepository, com.supplybase.partners.catalog.ServiceRepository serviceRepository,
                        MoneyService moneyService, FileStorageService fileStorageService, NotificationService notificationService) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.historyRepository = historyRepository;
        this.proofMediaRepository = proofMediaRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.partnerRepository = partnerRepository;
        this.partnerCoverageRepository = partnerCoverageRepository;
        this.partnerSkillRepository = partnerSkillRepository;
        this.serviceRepository = serviceRepository;
        this.moneyService = moneyService;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
    }

    public List<ServiceRequest> nearbyJobsFor(Long partnerId) {
        Partner partner = partnerRepository.findById(partnerId).orElseThrow(() -> new NotFoundException("Partner not found."));
        if (partner.getActivationStatus() != Partner.ActivationStatus.ACTIVE) {
            return List.of();
        }
        List<Long> eligibleServiceIds = eligibleServiceIdsFor(partner);
        if (eligibleServiceIds.isEmpty()) {
            return List.of();
        }
        List<Long> coverageAreaIds = partnerCoverageRepository.findAllByPartnerId(partnerId).stream()
                .map(com.supplybase.partners.partner.PartnerCoverage::getAreaId).toList();
        if (coverageAreaIds.isEmpty()) {
            return List.of();
        }
        List<Long> addressIds = customerAddressRepository.findAllByAreaIdIn(coverageAreaIds).stream()
                .map(CustomerAddress::getId).toList();
        if (addressIds.isEmpty()) {
            return List.of();
        }
        return serviceRequestRepository.findAllByStatusAndServiceIdInAndAddressIdInOrderByScheduledAt(
                ServiceRequest.Status.OPEN, eligibleServiceIds, addressIds);
    }

    private List<Long> eligibleServiceIdsFor(Partner partner) {
        List<Long> ids = partner.getPrimaryCategoryId() == null ? List.of() :
                serviceRepository.findAllByCategoryIdAndActiveTrueOrderByDisplayOrder(partner.getPrimaryCategoryId()).stream()
                        .map(com.supplybase.partners.catalog.Service::getId).collect(Collectors.toCollection(java.util.ArrayList::new));
        List<Long> approvedSkillIds = partnerSkillRepository.findAllByPartnerId(partner.getId()).stream()
                .filter(com.supplybase.partners.partner.PartnerSkill::isApproved)
                .map(com.supplybase.partners.partner.PartnerSkill::getServiceId).toList();
        java.util.LinkedHashSet<Long> combined = new java.util.LinkedHashSet<>(ids);
        combined.addAll(approvedSkillIds);
        return List.copyOf(combined);
    }

    public List<ServiceRequest> assignedJobsFor(Long partnerId) {
        return serviceRequestRepository.findAllByAssignedPartnerIdOrderByScheduledAtDesc(partnerId);
    }

    @Transactional
    public ServiceRequest accept(Long partnerId, Long requestId) {
        Partner partner = partnerRepository.findById(partnerId).orElseThrow(() -> new NotFoundException("Partner not found."));
        if (partner.getActivationStatus() != Partner.ActivationStatus.ACTIVE) {
            throw new ForbiddenException("Only active partners can accept jobs.");
        }
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Job not found."));
        if (!eligibleServiceIdsFor(partner).contains(request.getServiceId())) {
            throw new ForbiddenException("This job is not in your approved category/skills.");
        }
        int updated = serviceRequestRepository.tryAssign(requestId, partnerId);
        if (updated == 0) {
            throw new ConflictException("This job was already accepted by another partner.");
        }
        historyRepository.save(new JobStatusHistory(requestId, "OPEN", "ASSIGNED", partner.getUserId(), null));
        notificationService.enqueue(partner.getUserId(), NotificationOutbox.Channel.INAPP, "job.assigned",
                null, "{\"jobId\":" + requestId + "}");
        return serviceRequestRepository.findById(requestId).orElseThrow();
    }

    @Transactional
    public ServiceRequest start(Long partnerId, Long requestId) {
        ServiceRequest request = requireAssignedTo(requestId, partnerId);
        if (request.getStatus() != ServiceRequest.Status.ASSIGNED) {
            throw new BadRequestException("Only an assigned job can be started.");
        }
        request.setStatus(ServiceRequest.Status.IN_PROGRESS);
        serviceRequestRepository.save(request);
        historyRepository.save(new JobStatusHistory(requestId, "ASSIGNED", "IN_PROGRESS", null, null));
        return request;
    }

    @Transactional
    public ServiceRequest submitCompletion(Long partnerId, Long requestId, List<MultipartFile> proofFiles) {
        ServiceRequest request = requireAssignedTo(requestId, partnerId);
        if (request.getStatus() != ServiceRequest.Status.IN_PROGRESS) {
            throw new BadRequestException("Only a job in progress can be submitted for completion.");
        }
        for (MultipartFile file : proofFiles) {
            var stored = fileStorageService.store("job-proof/" + requestId, file);
            JobProofMedia media = new JobProofMedia();
            media.setServiceRequestId(requestId);
            media.setStorageKey(stored.storageKey());
            media.setContentType(stored.contentType());
            media.setUploadedByPartnerId(partnerId);
            proofMediaRepository.save(media);
        }
        request.setStatus(ServiceRequest.Status.COMPLETION_SUBMITTED);
        serviceRequestRepository.save(request);
        historyRepository.save(new JobStatusHistory(requestId, "IN_PROGRESS", "COMPLETION_SUBMITTED", null, null));
        return request;
    }

    /** Simulates the customer/admin confirmation adapter this build does not have a real customer app for. */
    @Transactional
    public ServiceRequest confirmCompletion(Long requestId, Long actorUserId) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Job not found."));
        if (request.getStatus() != ServiceRequest.Status.COMPLETION_SUBMITTED) {
            throw new BadRequestException("Only a submitted completion can be confirmed.");
        }
        request.setStatus(ServiceRequest.Status.COMPLETED);
        serviceRequestRepository.save(request);
        historyRepository.save(new JobStatusHistory(requestId, "COMPLETION_SUBMITTED", "COMPLETED", actorUserId, "Customer/admin confirmed completion"));

        moneyService.recordEarningForCompletedJob(request.getAssignedPartnerId(), requestId,
                request.getJobValuePaise(), request.getPartnerEarningPaise());
        partnerRepository.findById(request.getAssignedPartnerId()).ifPresent(p -> notificationService.enqueue(
                p.getUserId(), NotificationOutbox.Channel.INAPP, "job.completed.earning_posted",
                null, "{\"jobId\":" + requestId + ",\"netPaise\":" + request.getPartnerEarningPaise() + "}"));
        return request;
    }

    @Transactional
    public ServiceRequest cancel(Long requestId, Long actorUserId, String reason) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Job not found."));
        if (request.getStatus() == ServiceRequest.Status.COMPLETED || request.getStatus() == ServiceRequest.Status.CANCELLED) {
            throw new BadRequestException("This job can no longer be cancelled.");
        }
        String from = request.getStatus().name();
        request.setStatus(ServiceRequest.Status.CANCELLED);
        serviceRequestRepository.save(request);
        historyRepository.save(new JobStatusHistory(requestId, from, "CANCELLED", actorUserId, reason));
        return request;
    }

    @Transactional
    public ServiceRequest dispute(Long requestId, Long actorUserId, String reason) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Job not found."));
        if (request.getStatus() != ServiceRequest.Status.COMPLETION_SUBMITTED && request.getStatus() != ServiceRequest.Status.COMPLETED) {
            throw new BadRequestException("Only a submitted or completed job can be disputed.");
        }
        String from = request.getStatus().name();
        request.setStatus(ServiceRequest.Status.DISPUTED);
        serviceRequestRepository.save(request);
        historyRepository.save(new JobStatusHistory(requestId, from, "DISPUTED", actorUserId, reason));
        return request;
    }

    public List<JobStatusHistory> historyFor(Long requestId) {
        return historyRepository.findAllByServiceRequestIdOrderByCreatedAtAsc(requestId);
    }

    @Transactional
    public ServiceRequest adminCreate(Long adminUserId, Long customerId, Long serviceId, Long addressId, Instant scheduledAt,
                                       String notes, long jobValuePaise, long partnerEarningPaise) {
        ServiceRequest request = new ServiceRequest();
        request.setCustomerId(customerId);
        request.setServiceId(serviceId);
        request.setAddressId(addressId);
        request.setScheduledAt(scheduledAt);
        request.setNotes(notes);
        request.setJobValuePaise(jobValuePaise);
        request.setPartnerEarningPaise(partnerEarningPaise);
        request.setStatus(ServiceRequest.Status.OPEN);
        request.setCreatedByAdminId(adminUserId);
        serviceRequestRepository.save(request);
        historyRepository.save(new JobStatusHistory(request.getId(), null, "OPEN", adminUserId, "Created by admin"));
        return request;
    }

    private ServiceRequest requireAssignedTo(Long requestId, Long partnerId) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Job not found."));
        if (request.getAssignedPartnerId() == null || !request.getAssignedPartnerId().equals(partnerId)) {
            throw new ForbiddenException("This job is not assigned to you.");
        }
        return request;
    }
}
