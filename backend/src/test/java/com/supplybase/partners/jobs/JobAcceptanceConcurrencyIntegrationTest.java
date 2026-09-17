package com.supplybase.partners.jobs;

import com.supplybase.partners.catalog.Area;
import com.supplybase.partners.catalog.AreaRepository;
import com.supplybase.partners.catalog.Category;
import com.supplybase.partners.catalog.CategoryRepository;
import com.supplybase.partners.catalog.City;
import com.supplybase.partners.catalog.CityRepository;
import com.supplybase.partners.identity.AppUser;
import com.supplybase.partners.identity.AppUserRepository;
import com.supplybase.partners.identity.Role;
import com.supplybase.partners.partner.Partner;
import com.supplybase.partners.partner.PartnerCoverage;
import com.supplybase.partners.partner.PartnerCoverageRepository;
import com.supplybase.partners.partner.PartnerRepository;
import com.supplybase.partners.testsupport.AbstractIntegrationTest;
import com.supplybase.partners.testsupport.PartnerTestFlows;
import com.supplybase.partners.testsupport.TestSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two active, equally-eligible partners racing to accept the same OPEN job
 * must produce exactly one ASSIGNED winner and one 409 loser -- exercises
 * ServiceRequestRepository#tryAssign's conditional UPDATE.
 */
class JobAcceptanceConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private com.supplybase.partners.catalog.ServiceRepository serviceRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private AreaRepository areaRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private CustomerAddressRepository customerAddressRepository;
    @Autowired private ServiceRequestRepository serviceRequestRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private PartnerCoverageRepository partnerCoverageRepository;

    @Test
    void onlyOneOfTwoSimultaneousAcceptsOnTheSameJobSucceeds() throws Exception {
        Category category = categoryRepository.save(newCategory());
        com.supplybase.partners.catalog.Service service = new com.supplybase.partners.catalog.Service();
        service.setCategoryId(category.getId());
        service.setName("Test Service");
        service.setSlug("test-service-" + System.nanoTime());
        service = serviceRepository.save(service);

        City city = cityRepository.save(newCity());
        Area area = new Area();
        area.setCityId(city.getId());
        area.setName("Test Area");
        area = areaRepository.save(area);

        AppUser adminUser = appUserRepository.save(newSystemUser());

        var customer = customerRepository.save(newCustomer());
        var address = new CustomerAddress();
        address.setCustomerId(customer.getId());
        address.setLine1("1 Test Street");
        address.setAreaId(area.getId());
        address = customerAddressRepository.save(address);

        ServiceRequest request = new ServiceRequest();
        request.setCustomerId(customer.getId());
        request.setServiceId(service.getId());
        request.setAddressId(address.getId());
        request.setScheduledAt(Instant.now().plusSeconds(3600));
        request.setJobValuePaise(100000);
        request.setPartnerEarningPaise(70000);
        request.setStatus(ServiceRequest.Status.OPEN);
        request.setCreatedByAdminId(adminUser.getId());
        request = serviceRequestRepository.save(request);
        Long jobId = request.getId();

        PartnerTestFlows flows = new PartnerTestFlows(restTemplate);
        TestSession partnerA = flows.registerAndLogin("9722200001");
        TestSession partnerB = flows.registerAndLogin("9722200002");
        activateForJob(partnerA, category.getId(), area.getId());
        activateForJob(partnerB, category.getId(), area.getId());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        List<Future<ResponseEntity<Map>>> futures = List.of(
                pool.submit(() -> acceptConcurrently(partnerA, jobId, ready, go)),
                pool.submit(() -> acceptConcurrently(partnerB, jobId, ready, go)));

        ready.await(5, TimeUnit.SECONDS);
        go.countDown();

        List<HttpStatus> statuses = futures.stream()
                .map(f -> {
                    try {
                        return (HttpStatus) f.get(10, TimeUnit.SECONDS).getStatusCode();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
        pool.shutdown();

        assertThat(statuses).containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.CONFLICT);

        ServiceRequest reloaded = serviceRequestRepository.findById(jobId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ServiceRequest.Status.ASSIGNED);
        assertThat(reloaded.getAssignedPartnerId()).isNotNull();
    }

    /** Bypasses the full onboarding wizard: directly activates the freshly-registered test partner. */
    private void activateForJob(TestSession session, Long categoryId, Long areaId) {
        ResponseEntity<Map> me = session.get("/api/v1/auth/session", Map.class);
        Long userId = ((Number) me.getBody().get("userId")).longValue();
        Partner partner = partnerRepository.findByUserId(userId).orElseThrow();
        partner.setPrimaryCategoryId(categoryId);
        partner.setActivationStatus(Partner.ActivationStatus.ACTIVE);
        partnerRepository.save(partner);

        PartnerCoverage coverage = new PartnerCoverage();
        coverage.setPartnerId(partner.getId());
        coverage.setAreaId(areaId);
        partnerCoverageRepository.save(coverage);
    }

    private ResponseEntity<Map> acceptConcurrently(TestSession session, Long jobId, CountDownLatch ready, CountDownLatch go) {
        try {
            ready.countDown();
            go.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return session.post("/api/v1/jobs/" + jobId + "/accept", null, Map.class);
    }

    private Category newCategory() {
        Category c = new Category();
        c.setName("Job Test Category " + System.nanoTime());
        c.setSlug("job-test-category-" + System.nanoTime());
        return c;
    }

    private City newCity() {
        City c = new City();
        c.setName("Job Test City " + System.nanoTime());
        c.setState("Test State");
        return c;
    }

    private com.supplybase.partners.jobs.Customer newCustomer() {
        var customer = new com.supplybase.partners.jobs.Customer();
        customer.setName("Test Customer");
        customer.setPhoneE164("+919700000000");
        return customer;
    }

    private AppUser newSystemUser() {
        AppUser user = new AppUser();
        user.setPhoneE164("+919700000999");
        user.setName("Test System Admin");
        user.getRoles().add(Role.ADMIN);
        return user;
    }
}
