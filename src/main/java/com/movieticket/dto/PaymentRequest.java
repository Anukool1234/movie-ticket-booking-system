package com.movieticket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Simulate payment for a pending booking.
 * success=true → payment succeeds; false → payment fails (demo control).
 */
@Data
public class PaymentRequest {
    @NotNull
    private Long bookingId;

    /** Demo flag to simulate success or failure */
    private boolean success = true;
}
