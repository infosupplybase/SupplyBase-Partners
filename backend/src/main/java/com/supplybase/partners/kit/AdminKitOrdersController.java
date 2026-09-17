package com.supplybase.partners.kit;

import com.supplybase.partners.identity.CurrentUser;
import com.supplybase.partners.kit.dto.KitOrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/kit-orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminKitOrdersController {

    private final KitOrderService kitOrderService;
    private final KitOrderRepository kitOrderRepository;
    private final CurrentUser currentUser;

    public AdminKitOrdersController(KitOrderService kitOrderService, KitOrderRepository kitOrderRepository, CurrentUser currentUser) {
        this.kitOrderService = kitOrderService;
        this.kitOrderRepository = kitOrderRepository;
        this.currentUser = currentUser;
    }

    @GetMapping
    public Page<KitOrderResponse> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return kitOrderRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(KitOrderResponse::from);
    }

    @PatchMapping("/{orderId}/status")
    public KitOrderResponse updateStatus(@PathVariable Long orderId, @RequestBody Map<String, String> body) {
        KitOrder.Status status = KitOrder.Status.valueOf(body.get("status"));
        return KitOrderResponse.from(kitOrderService.adminUpdateStatus(orderId, status, currentUser.userId()));
    }

    @PostMapping("/exempt/{partnerId}")
    public void exempt(@PathVariable Long partnerId, @RequestBody Map<String, String> body) {
        kitOrderService.adminExempt(partnerId, body.getOrDefault("reason", ""), currentUser.userId());
    }
}
