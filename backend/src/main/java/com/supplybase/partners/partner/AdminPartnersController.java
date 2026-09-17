package com.supplybase.partners.partner;

import com.supplybase.partners.identity.CurrentUser;
import com.supplybase.partners.partner.dto.PartnerMeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/partners")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPartnersController {

    private final PartnerRepository partnerRepository;
    private final PartnerService partnerService;
    private final ActivationService activationService;
    private final CurrentUser currentUser;

    public AdminPartnersController(PartnerRepository partnerRepository, PartnerService partnerService,
                                    ActivationService activationService, CurrentUser currentUser) {
        this.partnerRepository = partnerRepository;
        this.partnerService = partnerService;
        this.activationService = activationService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public Page<PartnerMeResponse> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return partnerRepository.findAll(PageRequest.of(page, size)).map(PartnerMeResponse::from);
    }

    @GetMapping("/{partnerId}")
    public PartnerMeResponse detail(@PathVariable Long partnerId) {
        return PartnerMeResponse.from(partnerService.getByIdOrThrow(partnerId));
    }

    @GetMapping("/{partnerId}/status-history")
    public List<PartnerStatusHistory> statusHistory(@PathVariable Long partnerId) {
        return partnerService.statusHistory(partnerId);
    }

    @GetMapping("/{partnerId}/activation-eligibility")
    public ActivationService.EligibilityResult eligibility(@PathVariable Long partnerId) {
        return activationService.checkEligibility(partnerId);
    }

    @PostMapping("/{partnerId}/activate")
    public PartnerMeResponse activate(@PathVariable Long partnerId) {
        return PartnerMeResponse.from(activationService.activate(partnerId, currentUser.userId()));
    }

    @PostMapping("/{partnerId}/suspend")
    public PartnerMeResponse suspend(@PathVariable Long partnerId, @RequestBody Map<String, String> body) {
        return PartnerMeResponse.from(activationService.suspend(partnerId, body.getOrDefault("reason", ""), currentUser.userId()));
    }

    @PostMapping("/{partnerId}/reactivate")
    public PartnerMeResponse reactivate(@PathVariable Long partnerId, @RequestBody Map<String, String> body) {
        return PartnerMeResponse.from(activationService.reactivate(partnerId, body.getOrDefault("reason", ""), currentUser.userId()));
    }
}
