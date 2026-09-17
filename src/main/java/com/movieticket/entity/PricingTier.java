package com.movieticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Global pricing multipliers (regular / premium / weekend).
 * Admin can tune these without changing each show.
 *
 * Example:
 *  - regularMultiplier  = 1.0
 *  - premiumMultiplier  = 1.5  (premium = base * 1.5) — we use show-level prices instead
 *  - weekendMultiplier  = 1.2  (weekend shows charge 20% extra)
 *
 * Note: We keep this simple — weekendMultiplier is the main dynamic tier.
 * Regular and Premium base prices live on the Show entity.
 */
@Entity
@Table(name = "pricing_tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g. "DEFAULT"

    /** Applied when show.weekendShow = true */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weekendMultiplier;
}
