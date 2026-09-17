package com.supplybase.partners.support;

import com.supplybase.partners.common.domain.ForbiddenException;
import com.supplybase.partners.common.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupportService {

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketMessageRepository messageRepository;

    public SupportService(SupportTicketRepository ticketRepository, SupportTicketMessageRepository messageRepository) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
    }

    public List<SupportTicket> ticketsFor(Long partnerId) {
        return ticketRepository.findAllByPartnerIdOrderByCreatedAtDesc(partnerId);
    }

    public List<SupportTicketMessage> messagesFor(Long ticketId) {
        return messageRepository.findAllByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    @Transactional
    public SupportTicket createTicket(Long partnerId, String subject, String description) {
        SupportTicket ticket = new SupportTicket();
        ticket.setPartnerId(partnerId);
        ticket.setSubject(subject);
        ticket.setDescription(description);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public SupportTicketMessage addMessage(Long ticketId, Long senderUserId, String body, Long requestingPartnerId) {
        SupportTicket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new NotFoundException("Ticket not found."));
        if (requestingPartnerId != null && !ticket.getPartnerId().equals(requestingPartnerId)) {
            throw new ForbiddenException("You do not own this ticket.");
        }
        SupportTicketMessage message = new SupportTicketMessage();
        message.setTicketId(ticketId);
        message.setSenderUserId(senderUserId);
        message.setBody(body);
        return messageRepository.save(message);
    }

    @Transactional
    public SupportTicket updateStatus(Long ticketId, SupportTicket.Status status, Long assignedToUserId) {
        SupportTicket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new NotFoundException("Ticket not found."));
        ticket.setStatus(status);
        if (assignedToUserId != null) {
            ticket.setAssignedToUserId(assignedToUserId);
        }
        return ticketRepository.save(ticket);
    }
}
