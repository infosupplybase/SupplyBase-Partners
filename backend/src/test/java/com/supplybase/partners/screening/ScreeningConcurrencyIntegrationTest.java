package com.supplybase.partners.screening;

import com.supplybase.partners.catalog.Category;
import com.supplybase.partners.catalog.CategoryRepository;
import com.supplybase.partners.catalog.City;
import com.supplybase.partners.catalog.CityRepository;
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
 * Two partners racing for the last seat of a screening slot must produce
 * exactly one winner. This exercises ScreeningSlotRepository#tryReserveSeat's
 * conditional UPDATE under real concurrent MySQL transactions, which an H2
 * substitute would not reliably reproduce.
 */
class ScreeningConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private ScreeningSlotRepository slotRepository;

    @Test
    void onlyOneOfTwoSimultaneousBookingsOnATheLastSeatSucceeds() throws Exception {
        Category category = categoryRepository.save(newCategory());
        City city = cityRepository.save(newCity());

        ScreeningSlot slot = new ScreeningSlot();
        slot.setCategoryId(category.getId());
        slot.setCityId(city.getId());
        slot.setMode(ScreeningSlot.Mode.VIRTUAL);
        slot.setVenueOrLink("https://meet.example/test");
        slot.setStartsAt(Instant.now().plusSeconds(3600));
        slot.setEndsAt(Instant.now().plusSeconds(7200));
        slot.setCheckInOpensAt(Instant.now().plusSeconds(3500));
        slot.setCheckInClosesAt(Instant.now().plusSeconds(3700));
        slot.setCapacity(1);
        slot.setBookedCount(0);
        slot = slotRepository.save(slot);
        Long slotId = slot.getId();

        PartnerTestFlows flows = new PartnerTestFlows(restTemplate);
        TestSession partnerA = flows.registerAndLogin("9711100001");
        TestSession partnerB = flows.registerAndLogin("9711100002");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        List<Future<ResponseEntity<Map>>> futures = List.of(
                pool.submit(() -> bookConcurrently(partnerA, slotId, ready, go)),
                pool.submit(() -> bookConcurrently(partnerB, slotId, ready, go)));

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

        ScreeningSlot reloaded = slotRepository.findById(slotId).orElseThrow();
        assertThat(reloaded.getBookedCount()).isEqualTo(1);
    }

    private ResponseEntity<Map> bookConcurrently(TestSession session, Long slotId, CountDownLatch ready, CountDownLatch go) {
        try {
            ready.countDown();
            go.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return session.post("/api/v1/screening/bookings", Map.of("slotId", slotId), Map.class);
    }

    private Category newCategory() {
        Category c = new Category();
        c.setName("Test Category " + System.nanoTime());
        c.setSlug("test-category-" + System.nanoTime());
        return c;
    }

    private City newCity() {
        City c = new City();
        c.setName("Test City");
        c.setState("Test State");
        return c;
    }
}
