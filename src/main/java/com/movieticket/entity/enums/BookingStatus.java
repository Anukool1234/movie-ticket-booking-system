package com.movieticket.entity.enums;

/**
 * Lifecycle states of a customer booking.
 */
public enum BookingStatus {
    PENDING_PAYMENT, // Seats are held while the customer completes payment.
    CONFIRMED,       // Payment succeeded and seats are permanently booked.
    CANCELLED,       // Customer cancelled; seats were released.
    EXPIRED          // Payment was not completed before the hold expired.
}
