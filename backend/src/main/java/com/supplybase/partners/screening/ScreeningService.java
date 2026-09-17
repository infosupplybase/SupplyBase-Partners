package com.supplybase.partners.screening;

import com.supplybase.partners.common.domain.BadRequestException;
import com.supplybase.partners.common.domain.ConflictException;
import com.supplybase.partners.common.domain.ForbiddenException;
import com.supplybase.partners.common.domain.NotFoundException;
import com.supplybase.partners.notification.NotificationOutbox;
import com.supplybase.partners.notification.NotificationService;
import com.supplybase.partners.partner.OnboardingStage;
import com.supplybase.partners.partner.Partner;
import com.supplybase.partners.partner.PartnerRepository;
import com.supplybase.partners.partner.PartnerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Slot capacity is enforced by a conditional UPDATE (see
 * ScreeningSlotRepository#tryReserveSeat) rather than read-then-write, so two
 * concurrent booking attempts on the last seat can never both succeed.
 * Reschedule composes release+reserve inside one transaction so a partner is
 * never left without any active booking if the new slot turns out full.
 */
@Service
public class ScreeningService {

    private static final List<ScreeningBooking.Status> ACTIVE_STATUSES =
            List.of(ScreeningBooking.Status.BOOKED, ScreeningBooking.Status.CHECKED_IN);

    private final ScreeningSlotRepository slotRepository;
    private final ScreeningBookingRepository bookingRepository;
    private final ScreeningBookingHistoryRepository historyRepository;
    private final PartnerService partnerService;
    private final PartnerRepository partnerRepository;
    private final NotificationService notificationService;

    public ScreeningService(ScreeningSlotRepository slotRepository, ScreeningBookingRepository bookingRepository,
                             ScreeningBookingHistoryRepository historyRepository, PartnerService partnerService,
                             PartnerRepository partnerRepository, NotificationService notificationService) {
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.historyRepository = historyRepository;
        this.partnerService = partnerService;
        this.partnerRepository = partnerRepository;
        this.notificationService = notificationService;
    }

    public List<ScreeningSlot> availableSlots(Long categoryId, Long cityId) {
        return slotRepository.findAllByCategoryIdAndCityIdAndStartsAtAfterOrderByStartsAt(categoryId, cityId, Instant.now())
                .stream().filter(s -> s.remainingCapacity() > 0).toList();
    }

    public List<ScreeningBooking> bookingsFor(Long partnerId) {
        return bookingRepository.findAllByPartnerIdOrderByCreatedAtDesc(partnerId);
    }

    @Transactional
    public ScreeningBooking book(Long partnerId, Long slotId) {
        bookingRepository.findFirstByPartnerIdAndStatusInOrderByCreatedAtDesc(partnerId, ACTIVE_STATUSES)
                .ifPresent(b -> { throw new BadRequestException("You already have an active screening booking; reschedule or cancel it first."); });

        ScreeningSlot slot = slotRepository.findById(slotId).orElseThrow(() -> new NotFoundException("Slot not found."));
        if (Instant.now().isAfter(slot.getStartsAt())) {
            throw new BadRequestException("This slot has already started.");
        }
        int reserved = slotRepository.tryReserveSeat(slotId);
        if (reserved == 0) {
            throw new ConflictException("This slot just filled up. Please choose another.");
        }

        ScreeningBooking booking = new ScreeningBooking();
        booking.setSlotId(slotId);
        booking.setPartnerId(partnerId);
        booking.setStatus(ScreeningBooking.Status.BOOKED);
        bookingRepository.save(booking);
        historyRepository.save(new ScreeningBookingHistory(booking.getId(), "BOOKED", null, slotId));
        notifyBookingConfirmed(partnerId, slot);
        return booking;
    }

    private void notifyBookingConfirmed(Long partnerId, ScreeningSlot slot) {
        Partner partner = partnerRepository.findById(partnerId).orElse(null);
        if (partner == null) return;
        notificationService.enqueue(partner.getUserId(), NotificationOutbox.Channel.INAPP,
                "screening.booking.confirmed", null,
                java.util.Map.of("slotId", slot.getId(), "startsAt", slot.getStartsAt().toString(), "venueOrLink", slot.getVenueOrLink()));
    }

    @Transactional
    public ScreeningBooking reschedule(Long partnerId, Long bookingId, Long newSlotId) {
        ScreeningBooking booking = requireOwned(bookingId, partnerId);
        if (!ACTIVE_STATUSES.contains(booking.getStatus())) {
            throw new BadRequestException("Only an active booking can be rescheduled.");
        }
        ScreeningSlot newSlot = slotRepository.findById(newSlotId).orElseThrow(() -> new NotFoundException("Slot not found."));
        if (Instant.now().isAfter(newSlot.getStartsAt())) {
            throw new BadRequestException("This slot has already started.");
        }
        int reserved = slotRepository.tryReserveSeat(newSlotId);
        if (reserved == 0) {
            throw new ConflictException("The new slot just filled up. Please choose another.");
        }

        Long oldSlotId = booking.getSlotId();
        booking.setStatus(ScreeningBooking.Status.RESCHEDULED);
        bookingRepository.save(booking);
        slotRepository.releaseSeat(oldSlotId);

        ScreeningBooking newBooking = new ScreeningBooking();
        newBooking.setSlotId(newSlotId);
        newBooking.setPartnerId(partnerId);
        newBooking.setStatus(ScreeningBooking.Status.BOOKED);
        bookingRepository.save(newBooking);
        historyRepository.save(new ScreeningBookingHistory(booking.getId(), "RESCHEDULED", oldSlotId, newSlotId));
        return newBooking;
    }

    @Transactional
    public ScreeningBooking cancel(Long partnerId, Long bookingId) {
        ScreeningBooking booking = requireOwned(bookingId, partnerId);
        if (!ACTIVE_STATUSES.contains(booking.getStatus())) {
            throw new BadRequestException("Only an active booking can be cancelled.");
        }
        booking.setStatus(ScreeningBooking.Status.CANCELLED);
        bookingRepository.save(booking);
        slotRepository.releaseSeat(booking.getSlotId());
        historyRepository.save(new ScreeningBookingHistory(booking.getId(), "CANCELLED", booking.getSlotId(), null));
        return booking;
    }

    @Transactional
    public ScreeningBooking checkIn(Long partnerId, Long bookingId) {
        ScreeningBooking booking = requireOwned(bookingId, partnerId);
        if (booking.getStatus() != ScreeningBooking.Status.BOOKED) {
            throw new BadRequestException("Only a booked (not yet checked in) session can be checked in.");
        }
        ScreeningSlot slot = slotRepository.findById(booking.getSlotId()).orElseThrow();
        Instant now = Instant.now();
        if (now.isBefore(slot.getCheckInOpensAt()) || now.isAfter(slot.getCheckInClosesAt())) {
            throw new BadRequestException("Check-in is only available shortly before your session.");
        }
        booking.setStatus(ScreeningBooking.Status.CHECKED_IN);
        booking.setCheckedInAt(now);
        return bookingRepository.save(booking);
    }

    /** Staff-only: attendance intent (check-in) never implies a pass -- an authorized reviewer decides the outcome. */
    @Transactional
    public ScreeningBooking decideOutcome(Long bookingId, boolean passed, String reason, Long deciderUserId) {
        ScreeningBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found."));
        if (booking.getStatus() != ScreeningBooking.Status.CHECKED_IN) {
            throw new BadRequestException("Only a checked-in booking can be given an outcome.");
        }
        booking.setStatus(ScreeningBooking.Status.COMPLETED);
        booking.setOutcome(passed ? ScreeningBooking.Outcome.PASSED : ScreeningBooking.Outcome.FAILED);
        booking.setOutcomeReason(reason);
        booking.setDecidedByUserId(deciderUserId);
        booking.setDecidedAt(Instant.now());
        bookingRepository.save(booking);

        if (passed) {
            partnerService.advanceStageIfCurrent(booking.getPartnerId(), OnboardingStage.SCREENING, OnboardingStage.STARTER_KIT);
        }
        return booking;
    }

    private ScreeningBooking requireOwned(Long bookingId, Long partnerId) {
        ScreeningBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found."));
        if (!booking.getPartnerId().equals(partnerId)) {
            throw new ForbiddenException("You do not own this booking.");
        }
        return booking;
    }
}
