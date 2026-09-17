package com.supplybase.partners.money;

import com.supplybase.partners.identity.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/money")
@PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
public class AdminMoneyController {

    private final MoneyService moneyService;
    private final CurrentUser currentUser;

    public AdminMoneyController(MoneyService moneyService, CurrentUser currentUser) {
        this.moneyService = moneyService;
        this.currentUser = currentUser;
    }

    @PostMapping("/partners/{partnerId}/adjustments")
    public EarningAdjustment adjust(@PathVariable Long partnerId, @RequestBody Map<String, Object> body) {
        EarningAdjustment.Type type = EarningAdjustment.Type.valueOf(body.get("type").toString());
        long amount = Long.parseLong(body.get("amountPaise").toString());
        String reason = String.valueOf(body.getOrDefault("reason", ""));
        return moneyService.adminAdjust(partnerId, type, amount, reason, currentUser.userId());
    }

    @PostMapping("/earnings/{earningEntryId}/reverse")
    public EarningEntry reverse(@PathVariable Long earningEntryId, @RequestBody Map<String, String> body) {
        return moneyService.reverseEarning(earningEntryId, body.getOrDefault("reason", ""), currentUser.userId());
    }
}
