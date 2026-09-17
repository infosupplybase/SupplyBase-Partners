package com.supplybase.partners.screening;

import com.supplybase.partners.identity.CurrentUser;
import com.supplybase.partners.screening.dto.BookingResponse;
import com.supplybase.partners.screening.dto.SlotResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/screening")
@PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
public class AdminScreeningController {

    private final ScreeningService screeningService;
    private final ScreeningSlotRepository slotRepository;
    private final ScreeningBookingRepository bookingRepository;
    private final CurrentUser currentUser;

    public AdminScreeningController(ScreeningService screeningService, ScreeningSlotRepository slotRepository,
                                     ScreeningBookingRepository bookingRepository, CurrentUser currentUser) {
        this.screeningService = screeningService;
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.currentUser = currentUser;
    }

    @GetMapping("/slots")
    public List<SlotResponse> slots() {
        return slotRepository.findAll().stream()
                .sorted((a, b) -> a.getStartsAt().compareTo(b.getStartsAt()))
                .map(SlotResponse::from).toList();
    }

    @GetMapping("/bookings/checked-in")
    public List<BookingResponse> checkedIn() {
        return bookingRepository.findAllByStatusOrderByCreatedAtAsc(ScreeningBooking.Status.CHECKED_IN).stream()
                .map(BookingResponse::from).toList();
    }

    @PostMapping("/slots")
    @PreAuthorize("hasRole('ADMIN')")
    public SlotResponse createSlot(@RequestBody Map<String, Object> body) {
        ScreeningSlot slot = new ScreeningSlot();
        slot.setCategoryId(Long.valueOf(body.get("categoryId").toString()));
        slot.setCityId(Long.valueOf(body.get("cityId").toString()));
        slot.setMode(ScreeningSlot.Mode.valueOf(body.get("mode").toString()));
        slot.setVenueOrLink(body.get("venueOrLink").toString());
        slot.setDirectionsText((String) body.get("directionsText"));
        slot.setStartsAt(Instant.parse(body.get("startsAt").toString()));
        slot.setEndsAt(Instant.parse(body.get("endsAt").toString()));
        slot.setCheckInOpensAt(Instant.parse(body.get("checkInOpensAt").toString()));
        slot.setCheckInClosesAt(Instant.parse(body.get("checkInClosesAt").toString()));
        slot.setCapacity(Integer.parseInt(body.get("capacity").toString()));
        return SlotResponse.from(slotRepository.save(slot));
    }

    @PostMapping("/bookings/{bookingId}/decision")
    public BookingResponse decide(@PathVariable Long bookingId, @RequestBody Map<String, Object> body) {
        boolean passed = Boolean.TRUE.equals(body.get("passed"));
        String reason = String.valueOf(body.getOrDefault("reason", ""));
        return BookingResponse.from(screeningService.decideOutcome(bookingId, passed, reason, currentUser.userId()));
    }
}
