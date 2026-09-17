package com.supplybase.partners.screening;

import com.supplybase.partners.identity.CurrentUser;
import com.supplybase.partners.partner.Partner;
import com.supplybase.partners.partner.PartnerService;
import com.supplybase.partners.screening.dto.BookingResponse;
import com.supplybase.partners.screening.dto.SlotResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/screening")
@PreAuthorize("hasRole('PARTNER')")
public class ScreeningController {

    private final ScreeningService screeningService;
    private final PartnerService partnerService;
    private final CurrentUser currentUser;

    public ScreeningController(ScreeningService screeningService, PartnerService partnerService, CurrentUser currentUser) {
        this.screeningService = screeningService;
        this.partnerService = partnerService;
        this.currentUser = currentUser;
    }

    @GetMapping("/slots")
    public List<SlotResponse> slots(@RequestParam Long categoryId, @RequestParam Long cityId) {
        return screeningService.availableSlots(categoryId, cityId).stream().map(SlotResponse::from).toList();
    }

    @GetMapping("/bookings")
    public List<BookingResponse> bookings() {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return screeningService.bookingsFor(partner.getId()).stream().map(BookingResponse::from).toList();
    }

    @PostMapping("/bookings")
    public BookingResponse book(@RequestBody Map<String, Long> body) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return BookingResponse.from(screeningService.book(partner.getId(), body.get("slotId")));
    }

    @PostMapping("/bookings/{bookingId}/reschedule")
    public BookingResponse reschedule(@PathVariable Long bookingId, @RequestBody Map<String, Long> body) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return BookingResponse.from(screeningService.reschedule(partner.getId(), bookingId, body.get("newSlotId")));
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public BookingResponse cancel(@PathVariable Long bookingId) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return BookingResponse.from(screeningService.cancel(partner.getId(), bookingId));
    }

    @PostMapping("/bookings/{bookingId}/check-in")
    public BookingResponse checkIn(@PathVariable Long bookingId) {
        Partner partner = partnerService.getByUserId(currentUser.userId());
        return BookingResponse.from(screeningService.checkIn(partner.getId(), bookingId));
    }
}
