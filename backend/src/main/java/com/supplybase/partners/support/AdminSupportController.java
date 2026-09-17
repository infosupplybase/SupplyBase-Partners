package com.supplybase.partners.support;

import com.supplybase.partners.identity.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/support")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupportController {

    private final SupportService supportService;
    private final SupportTicketRepository ticketRepository;
    private final CurrentUser currentUser;

    public AdminSupportController(SupportService supportService, SupportTicketRepository ticketRepository, CurrentUser currentUser) {
        this.supportService = supportService;
        this.ticketRepository = ticketRepository;
        this.currentUser = currentUser;
    }

    @GetMapping("/tickets")
    public Page<SupportTicket> tickets(@RequestParam(defaultValue = "OPEN") SupportTicket.Status status,
                                        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ticketRepository.findAllByStatusOrderByCreatedAtDesc(status, PageRequest.of(page, size));
    }

    @PostMapping("/tickets/{ticketId}/messages")
    public SupportTicketMessage reply(@PathVariable Long ticketId, @RequestBody Map<String, String> body) {
        return supportService.addMessage(ticketId, currentUser.userId(), body.get("body"), null);
    }

    @PatchMapping("/tickets/{ticketId}/status")
    public SupportTicket updateStatus(@PathVariable Long ticketId, @RequestBody Map<String, String> body) {
        return supportService.updateStatus(ticketId, SupportTicket.Status.valueOf(body.get("status")), currentUser.userId());
    }
}
