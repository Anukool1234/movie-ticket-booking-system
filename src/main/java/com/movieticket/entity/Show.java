package com.movieticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A movie show in a theater at a specific time.
 * Pricing can be marked as weekend for weekend surcharge.
 */
@Entity
@Table(name = "shows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String movieTitle;

    @ManyToOne(optional = false)
    @JoinColumn(name = "theater_id")
    private Theater theater;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    /** Base price for REGULAR seats */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal regularPrice;

    /** Base price for PREMIUM seats */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal premiumPrice;

    /**
     * If true, weekend multiplier from PricingTier is applied.
     * Kept as a flag so admin can control it explicitly.
     */
    @Column(nullable = false)
    private boolean weekendShow;
}
