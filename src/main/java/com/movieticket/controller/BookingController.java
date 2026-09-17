package com.movieticket.controller;

import com.movieticket.dto.BookingResponse;
import com.movieticket.dto.CreateBookingRequest;
import com.movieticket.dto.PaymentRequest;
import com.movieticket.dto.ShowSeatResponse;
import com.movieticket.security.UserPrincipal;
import com.movieticket.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Customer booking APIs (login required).
 */
@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/api/shows/{showId}/seats")
    public List<ShowSeatResponse> showSeats(@PathVariable Long showId) {
        return bookingService.getShowSeats(showId);
    }

    @PostMapping("/api/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@AuthenticationPrincipal UserPrincipal principal,
                                  @Valid @RequestBody CreateBookingRequest request) {
        return bookingService.createBooking(principal.getId(), request);
    }

    @PostMapping("/api/bookings/pay")
    public BookingResponse pay(@AuthenticationPrincipal UserPrincipal principal,
                               @Valid @RequestBody PaymentRequest request) {
        return bookingService.pay(principal.getId(), request);
    }

    @PostMapping("/api/bookings/{id}/cancel")
    public BookingResponse cancel(@AuthenticationPrincipal UserPrincipal principal,
                                  @PathVariable Long id) {
        return bookingService.cancel(principal.getId(), id);
    }

    @GetMapping("/api/bookings")
    public List<BookingResponse> history(@AuthenticationPrincipal UserPrincipal principal) {
        return bookingService.history(principal.getId());
    }

    @GetMapping("/api/bookings/{id}")
    public BookingResponse getOne(@AuthenticationPrincipal UserPrincipal principal,
                                  @PathVariable Long id) {
        return bookingService.getBooking(principal.getId(), id);
    }
}
