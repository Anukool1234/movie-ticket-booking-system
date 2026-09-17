package com.movieticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Configurable refund policy based on how many hours before the show
 * the customer cancels.
 *
 * Example rows:
 *  hoursBeforeShow >= 24 → refund 100%
 *  hoursBeforeShow >= 6  → refund 50%
 *  hoursBeforeShow >= 0  → refund 0%
 */
@Entity
@Table(name = "refund_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Minimum hours before show start for this rule to apply */
    @Column(nullable = false)
    private int minHoursBeforeShow;

    /** Percentage of paid amount to refund (0–100) */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal refundPercentage;

    @Column(nullable = false)
    private boolean active;
}
