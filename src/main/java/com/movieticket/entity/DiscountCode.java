package com.movieticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Discount / promo code that reduces booking total.
 * percentageOff: 10 means 10% off.
 */
@Entity
@Table(name = "discount_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentageOff;

    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    @Column(nullable = false)
    private boolean active;
}
