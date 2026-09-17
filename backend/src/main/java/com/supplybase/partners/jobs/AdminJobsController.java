package com.supplybase.partners.jobs;

import com.supplybase.partners.identity.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/jobs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminJobsController {

    private final JobsService jobsService;
    private final ServiceRequestRepository serviceRequestRepository;
    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final CurrentUser currentUser;

    public AdminJobsController(JobsService jobsService, ServiceRequestRepository serviceRequestRepository,
                                CustomerRepository customerRepository, CustomerAddressRepository customerAddressRepository,
                                CurrentUser currentUser) {
        this.jobsService = jobsService;
        this.serviceRequestRepository = serviceRequestRepository;
        this.customerRepository = customerRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.currentUser = currentUser;
    }

    @PostMapping("/customers")
    public Customer createCustomer(@RequestBody Map<String, String> body) {
        Customer c = new Customer();
        c.setName(body.get("name"));
        c.setPhoneE164(body.get("phone"));
        return customerRepository.save(c);
    }

    @PostMapping("/customers/{customerId}/addresses")
    public CustomerAddress createAddress(@PathVariable Long customerId, @RequestBody Map<String, Object> body) {
        CustomerAddress a = new CustomerAddress();
        a.setCustomerId(customerId);
        a.setLine1(body.get("line1").toString());
        a.setLine2((String) body.get("line2"));
        a.setAreaId(Long.valueOf(body.get("areaId").toString()));
        return customerAddressRepository.save(a);
    }

    @PostMapping("/service-requests")
    public ServiceRequest createRequest(@RequestBody Map<String, Object> body) {
        return jobsService.adminCreate(currentUser.userId(),
                Long.valueOf(body.get("customerId").toString()),
                Long.valueOf(body.get("serviceId").toString()),
                Long.valueOf(body.get("addressId").toString()),
                Instant.parse(body.get("scheduledAt").toString()),
                (String) body.get("notes"),
                Long.parseLong(body.get("jobValuePaise").toString()),
                Long.parseLong(body.get("partnerEarningPaise").toString()));
    }

    @GetMapping("/service-requests")
    public List<ServiceRequest> list() {
        return serviceRequestRepository.findAll();
    }

    @GetMapping("/service-requests/{id}/history")
    public List<JobStatusHistory> history(@PathVariable Long id) {
        return jobsService.historyFor(id);
    }

    @PostMapping("/service-requests/{id}/confirm-completion")
    public ServiceRequest confirmCompletion(@PathVariable Long id) {
        return jobsService.confirmCompletion(id, currentUser.userId());
    }

    @PostMapping("/service-requests/{id}/cancel")
    public ServiceRequest cancel(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return jobsService.cancel(id, currentUser.userId(), body.getOrDefault("reason", ""));
    }

    @PostMapping("/service-requests/{id}/dispute")
    public ServiceRequest dispute(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return jobsService.dispute(id, currentUser.userId(), body.getOrDefault("reason", ""));
    }
}
