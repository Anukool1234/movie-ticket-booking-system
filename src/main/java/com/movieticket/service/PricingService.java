package com.movieticket.service;

import com.movieticket.entity.*;
import com.movieticket.entity.enums.SeatType;
import com.movieticket.exception.BusinessException;
import com.movieticket.repository.DiscountCodeRepository;
import com.movieticket.repository.PricingTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Calculates ticket prices using:
 * - show regular/premium base price
 * - weekend multiplier from PricingTier (if show is weekend)
 * - optional discount code percentage
 */
@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingTierRepository pricingTierRepository;
    private final DiscountCodeRepository discountCodeRepository;

    /** Price for one seat in a show (before discount). */
    public BigDecimal calculateSeatPrice(Show show, Seat seat) {
        BigDecimal base = seat.getSeatType() == SeatType.PREMIUM
                ? show.getPremiumPrice()
                : show.getRegularPrice();

        if (show.isWeekendShow()) {
            BigDecimal multiplier = pricingTierRepository.findByName("DEFAULT")
                    .map(PricingTier::getWeekendMultiplier)
                    .orElse(BigDecimal.ONE);
            base = base.multiply(multiplier);
        }
        return base.setScale(2, RoundingMode.HALF_UP);
    }

    /** Validate discount and return percentage off (0 if none). */
    public BigDecimal resolveDiscountPercent(String code) {
        if (code == null || code.isBlank()) {
            return BigDecimal.ZERO;
        }
        DiscountCode discount = discountCodeRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BusinessException("Invalid discount code"));

        if (!discount.isActive()) {
            throw new BusinessException("Discount code is inactive");
        }
        LocalDateTime now = LocalDateTime.now();
        if (discount.getValidFrom() != null && now.isBefore(discount.getValidFrom())) {
            throw new BusinessException("Discount code is not yet valid");
        }
        if (discount.getValidUntil() != null && now.isAfter(discount.getValidUntil())) {
            throw new BusinessException("Discount code has expired");
        }
        return discount.getPercentageOff();
    }

    public BigDecimal applyDiscount(BigDecimal total, BigDecimal percentageOff) {
        if (percentageOff == null || percentageOff.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return total.multiply(percentageOff)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
