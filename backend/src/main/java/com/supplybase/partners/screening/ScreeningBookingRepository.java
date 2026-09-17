package com.supplybase.partners.screening;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScreeningBookingRepository extends JpaRepository<ScreeningBooking, Long> {
    List<ScreeningBooking> findAllByPartnerIdOrderByCreatedAtDesc(Long partnerId);

    Optional<ScreeningBooking> findFirstByPartnerIdAndStatusInOrderByCreatedAtDesc(Long partnerId, List<ScreeningBooking.Status> statuses);
}
