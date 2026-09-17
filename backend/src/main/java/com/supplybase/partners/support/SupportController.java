package com.supplybase.partners.support;

import com.supplybase.partners.identity.CurrentUser;
import com.supplybase.partners.partner.Partner;
import com.supplybase.partners.partner.PartnerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/support")
@PreAuthorize("hasRole('PARTNER')")
public class SupportController {

    private final SupportService supportService;
    private final PartnerService partnerService;
    private final CurrentUser currentUser;

    public SupportController(SupportService supportService, PartnerService partnerService, CurrentUser currentUser) {
        this.supportService = supportService;
        this.partnerService = partnerService;
        this.currentUser = currentUser;
    }

    @GetMapping("/tickets")
    public List<SupportTicket> tickets() {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return supportService.ticketsFor(partner.getId());
    }

    @PostMapping("/tickets")
    public SupportTicket createTicket(@RequestBody Map<String, String> body) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return supportService.createTicket(partner.getId(), body.get("subject"), body.get("description"));
    }

    @GetMapping("/tickets/{ticketId}/messages")
    public List<SupportTicketMessage> messages(@PathVariable Long ticketId) {
        return supportService.messagesFor(ticketId);
    }

    @PostMapping("/tickets/{ticketId}/messages")
    public SupportTicketMessage reply(@PathVariable Long ticketId, @RequestBody Map<String, String> body) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return supportService.addMessage(ticketId, currentUser.userId(), body.get("body"), partner.getId());
    }
}
