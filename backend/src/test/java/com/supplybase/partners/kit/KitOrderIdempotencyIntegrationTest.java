package com.supplybase.partners.kit;

import com.supplybase.partners.catalog.Category;
import com.supplybase.partners.catalog.CategoryRepository;
import com.supplybase.partners.catalog.City;
import com.supplybase.partners.catalog.CityRepository;
import com.supplybase.partners.partner.OnboardingStage;
import com.supplybase.partners.partner.Partner;
import com.supplybase.partners.partner.PartnerRepository;
import com.supplybase.partners.testsupport.AbstractIntegrationTest;
import com.supplybase.partners.testsupport.PartnerTestFlows;
import com.supplybase.partners.testsupport.TestSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Booking a starter kit twice with the same idempotency key must return the same order, never create a second. */
class KitOrderIdempotencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private StarterKitRepository starterKitRepository;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private KitOrderRepository kitOrderRepository;

    @Test
    void repeatingTheSameBookingRequestReturnsTheOriginalOrder() {
        Category category = categoryRepository.save(newCategory());
        City city = cityRepository.save(newCity());

        StarterKit kit = new StarterKit();
        kit.setCategoryId(category.getId());
        kit.setName("Test Kit");
        kit.setPricePaise(100000);
        kit.setFeesPaise(5000);
        kit.setTermsText("Test terms.");
        starterKitRepository.save(kit);

        PartnerTestFlows flows = new PartnerTestFlows(restTemplate);
        TestSession session = flows.registerAndLogin("9733300001");

        ResponseEntity<Map> me = session.get("/api/v1/auth/session", Map.class);
        Long userId = ((Number) me.getBody().get("userId")).longValue();
        Partner partner = partnerRepository.findByUserId(userId).orElseThrow();
        partner.setPrimaryCategoryId(category.getId());
        partner.setResidenceCityId(city.getId());
        partner.setOnboardingStage(OnboardingStage.STARTER_KIT);
        partnerRepository.save(partner);

        String idempotencyKey = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
                "deliveryAddressLine", "1 Test Street",
                "deliveryCityId", city.getId(),
                "idempotencyKey", idempotencyKey);

        ResponseEntity<Map> first = session.post("/api/v1/kit-orders", body, Map.class);
        ResponseEntity<Map> second = session.post("/api/v1/kit-orders", body, Map.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(first.getBody().get("id")).isEqualTo(second.getBody().get("id"));

        List<KitOrder> orders = kitOrderRepository.findAllByPartnerIdOrderByCreatedAtDesc(partner.getId());
        assertThat(orders).hasSize(1);
    }

    private Category newCategory() {
        Category c = new Category();
        c.setName("Kit Test Category " + System.nanoTime());
        c.setSlug("kit-test-category-" + System.nanoTime());
        return c;
    }

    private City newCity() {
        City c = new City();
        c.setName("Kit Test City " + System.nanoTime());
        c.setState("Test State");
        return c;
    }
}
