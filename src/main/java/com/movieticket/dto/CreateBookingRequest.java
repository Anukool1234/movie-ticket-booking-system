package com.movieticket.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Start a booking by holding seats for a show.
 */
@Data
public class CreateBookingRequest {
    @NotNull
    private Long showId;

    /** List of ShowSeat IDs to hold */
    @NotEmpty
    private List<Long> showSeatIds;

    /** Optional discount / promo code */
    private String discountCode;
}
