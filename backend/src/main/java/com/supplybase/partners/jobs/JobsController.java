package com.supplybase.partners.jobs;

import com.supplybase.partners.catalog.Area;
import com.supplybase.partners.catalog.AreaRepository;
import com.supplybase.partners.catalog.City;
import com.supplybase.partners.catalog.CityRepository;
import com.supplybase.partners.identity.CurrentUser;
import com.supplybase.partners.jobs.dto.AssignedJobResponse;
import com.supplybase.partners.jobs.dto.NearbyJobResponse;
import com.supplybase.partners.partner.Partner;
import com.supplybase.partners.partner.PartnerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@PreAuthorize("hasRole('PARTNER')")
public class JobsController {

    private final JobsService jobsService;
    private final PartnerService partnerService;
    private final CurrentUser currentUser;
    private final CustomerAddressRepository addressRepository;
    private final CustomerRepository customerRepository;
    private final AreaRepository areaRepository;
    private final CityRepository cityRepository;

    public JobsController(JobsService jobsService, PartnerService partnerService, CurrentUser currentUser,
                           CustomerAddressRepository addressRepository, CustomerRepository customerRepository,
                           AreaRepository areaRepository, CityRepository cityRepository) {
        this.jobsService = jobsService;
        this.partnerService = partnerService;
        this.currentUser = currentUser;
        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
        this.areaRepository = areaRepository;
        this.cityRepository = cityRepository;
    }

    @GetMapping("/nearby")
    public List<NearbyJobResponse> nearby() {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return jobsService.nearbyJobsFor(partner.getId()).stream().map(this::toNearby).toList();
    }

    @GetMapping("/assigned")
    public List<AssignedJobResponse> assigned() {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return jobsService.assignedJobsFor(partner.getId()).stream().map(this::toAssigned).toList();
    }

    @GetMapping("/{jobId}")
    public AssignedJobResponse detail(@PathVariable Long jobId) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        ServiceRequest request = jobsService.assignedJobsFor(partner.getId()).stream()
                .filter(r -> r.getId().equals(jobId)).findFirst()
                .orElseThrow(() -> new com.supplybase.partners.common.domain.ForbiddenException("This job is not assigned to you."));
        return toAssigned(request);
    }

    @PostMapping("/{jobId}/accept")
    public AssignedJobResponse accept(@PathVariable Long jobId) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return toAssigned(jobsService.accept(partner.getId(), jobId));
    }

    @PostMapping("/{jobId}/start")
    public AssignedJobResponse start(@PathVariable Long jobId) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return toAssigned(jobsService.start(partner.getId(), jobId));
    }

    @PostMapping("/{jobId}/completion")
    public AssignedJobResponse submitCompletion(@PathVariable Long jobId,
                                                 @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return toAssigned(jobsService.submitCompletion(partner.getId(), jobId, files == null ? List.of() : files));
    }

    private NearbyJobResponse toNearby(ServiceRequest r) {
        CustomerAddress address = addressRepository.findById(r.getAddressId()).orElseThrow();
        Area area = areaRepository.findById(address.getAreaId()).orElseThrow();
        City city = cityRepository.findById(area.getCityId()).orElseThrow();
        return new NearbyJobResponse(r.getId(), r.getServiceId(), r.getScheduledAt(), r.getNotes(),
                r.getPartnerEarningPaise(), area.getName(), city.getName());
    }

    private AssignedJobResponse toAssigned(ServiceRequest r) {
        CustomerAddress address = addressRepository.findById(r.getAddressId()).orElseThrow();
        Customer customer = customerRepository.findById(r.getCustomerId()).orElseThrow();
        return new AssignedJobResponse(r.getId(), r.getServiceId(), r.getScheduledAt(), r.getNotes(),
                r.getPartnerEarningPaise(), r.getStatus().name(), customer.getName(), customer.getPhoneE164(),
                address.getLine1(), address.getLine2());
    }
}
