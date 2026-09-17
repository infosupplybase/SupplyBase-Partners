package com.supplybase.partners.support;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findAllByPartnerIdOrderByCreatedAtDesc(Long partnerId);

    Page<SupportTicket> findAllByStatusOrderByCreatedAtDesc(SupportTicket.Status status, Pageable pageable);
}
