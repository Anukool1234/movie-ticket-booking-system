package com.movieticket.entity.enums;

/**
 * Availability of one physical seat for one show.
 *
 * <p>AVAILABLE can become HELD while a customer pays. A successful payment changes
 * HELD to BOOKED; an expired hold changes it back to AVAILABLE.</p>
 */
public enum SeatStatus {
    AVAILABLE,
    HELD,
    BOOKED
}
