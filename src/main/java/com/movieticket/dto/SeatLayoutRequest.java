package com.movieticket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Simple seat layout generator request.
 * Creates seats for rows from startRow to endRow, with seatsPerRow each.
 * Example: startRow=A, endRow=C, seatsPerRow=5 → A1..A5, B1..B5, C1..C5
 */
@Data
public class SeatLayoutRequest {
    @NotBlank
    private String startRow; // e.g. "A"

    @NotBlank
    private String endRow;   // e.g. "D"

    @Min(1)
    private int seatsPerRow;

    /** Rows from this letter onward are PREMIUM; earlier rows are REGULAR */
    private String premiumFromRow; // e.g. "C" means C and D are premium
}
