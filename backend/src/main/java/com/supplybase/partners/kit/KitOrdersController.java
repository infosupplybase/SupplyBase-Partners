package com.supplybase.partners.kit;

import com.supplybase.partners.identity.CurrentUser;
import com.supplybase.partners.kit.dto.BookKitOrderRequest;
import com.supplybase.partners.kit.dto.KitOrderResponse;
import com.supplybase.partners.kit.dto.QuoteResponse;
import com.supplybase.partners.partner.Partner;
import com.supplybase.partners.partner.PartnerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kit-orders")
@PreAuthorize("hasRole('PARTNER')")
public class KitOrdersController {

    private final KitOrderService kitOrderService;
    private final PartnerService partnerService;
    private final CurrentUser currentUser;

    public KitOrdersController(KitOrderService kitOrderService, PartnerService partnerService, CurrentUser currentUser) {
        this.kitOrderService = kitOrderService;
        this.partnerService = partnerService;
        this.currentUser = currentUser;
    }

    @GetMapping("/quote")
    public QuoteResponse quote() {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        kitOrderService.skipIfNotRequired(partner);
        return kitOrderService.quoteFor(partner)
                .map(q -> new QuoteResponse(true, q.kit().getId(), q.kit().getName(), q.kit().getDescription(),
                        q.kit().getPricePaise(), q.kit().getFeesPaise(), q.kit().getPricePaise() + q.kit().getFeesPaise(),
                        q.kit().getTermsText(), q.items().stream().map(StarterKitItem::getName).toList()))
                .orElseGet(QuoteResponse::unavailable);
    }

    @GetMapping
    public List<KitOrderResponse> myOrders() {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return kitOrderService.ordersFor(partner.getId()).stream().map(KitOrderResponse::from).toList();
    }

    @PostMapping
    public KitOrderResponse book(@Valid @RequestBody BookKitOrderRequest body) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return KitOrderResponse.from(kitOrderService.book(partner, body.deliveryAddressLine(), body.deliveryCityId(), body.idempotencyKey()));
    }

    @PostMapping("/{orderId}/cancel")
    public KitOrderResponse cancel(@PathVariable Long orderId) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return KitOrderResponse.from(kitOrderService.cancel(partner.getId(), orderId));
    }
}
