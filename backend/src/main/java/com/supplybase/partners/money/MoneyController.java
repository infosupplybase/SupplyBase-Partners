package com.supplybase.partners.money;

import com.supplybase.partners.identity.CurrentUser;
import com.supplybase.partners.money.dto.PayoutRequest;
import com.supplybase.partners.partner.Partner;
import com.supplybase.partners.partner.PartnerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/money")
@PreAuthorize("hasRole('PARTNER')")
public class MoneyController {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final MoneyService moneyService;
    private final PartnerService partnerService;
    private final CurrentUser currentUser;

    public MoneyController(MoneyService moneyService, PartnerService partnerService, CurrentUser currentUser) {
        this.moneyService = moneyService;
        this.partnerService = partnerService;
        this.currentUser = currentUser;
    }

    @GetMapping("/summary")
    public MoneyService.Summary summary() {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        ZonedDateTime monthStart = ZonedDateTime.now(BUSINESS_ZONE).withDayOfMonth(1).toLocalDate().atStartOfDay(BUSINESS_ZONE);
        return moneyService.summary(partner.getId(), monthStart.toInstant(), Instant.now());
    }

    @GetMapping("/ledger")
    public List<EarningEntry> ledger() {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return moneyService.ledger(partner.getId());
    }

    @GetMapping("/payouts")
    public List<Payout> payouts() {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return moneyService.payouts(partner.getId());
    }

    @PostMapping("/payouts")
    public Payout requestPayout(@Valid @RequestBody PayoutRequest body) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return moneyService.requestPayout(partner.getId(), body.amountPaise());
    }
}
