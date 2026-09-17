package com.movieticket.entity;

import com.movieticket.entity.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Physical seat in a theater layout (e.g. A1, A2, B1).
 * Seat type decides pricing (REGULAR / PREMIUM).
 */
@Entity
@Table(name = "seats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"theater_id", "rowLabel", "seatNumber"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "theater_id")
    private Theater theater;

    /** Row label like A, B, C */
    @Column(nullable = false)
    private String rowLabel;

    /** Seat number within the row */
    @Column(nullable = false)
    private Integer seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatType seatType;

    /** Helper for display: A1, B5, etc. */
    public String getLabel() {
        return rowLabel + seatNumber;
    }
}
