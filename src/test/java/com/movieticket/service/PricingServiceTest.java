package com.movieticket.service;

import com.movieticket.entity.DiscountCode;
import com.movieticket.entity.PricingTier;
import com.movieticket.entity.Seat;
import com.movieticket.entity.Show;
import com.movieticket.entity.enums.SeatType;
import com.movieticket.exception.BusinessException;
import com.movieticket.repository.DiscountCodeRepository;
import com.movieticket.repository.PricingTierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for price and discount calculation logic.
 */
@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private PricingTierRepository pricingTierRepository;

    @Mock
    private DiscountCodeRepository discountCodeRepository;

    @InjectMocks
    private PricingService pricingService;

    private Show show;
    private Seat regularSeat;
    private Seat premiumSeat;

    @BeforeEach
    void setUp() {
        show = Show.builder()
                .movieTitle("Test Movie")
                .regularPrice(new BigDecimal("100.00"))
                .premiumPrice(new BigDecimal("200.00"))
                .weekendShow(false)
                .build();
        regularSeat = Seat.builder().rowLabel("A").seatNumber(1).seatType(SeatType.REGULAR).build();
        premiumSeat = Seat.builder().rowLabel("C").seatNumber(1).seatType(SeatType.PREMIUM).build();
    }

    @Test
    void calculatesRegularAndPremiumPrices() {
        assertEquals(new BigDecimal("100.00"), pricingService.calculateSeatPrice(show, regularSeat));
        assertEquals(new BigDecimal("200.00"), pricingService.calculateSeatPrice(show, premiumSeat));
    }

    @Test
    void appliesWeekendMultiplier() {
        show.setWeekendShow(true);
        when(pricingTierRepository.findByName("DEFAULT"))
                .thenReturn(Optional.of(PricingTier.builder()
                        .name("DEFAULT")
                        .weekendMultiplier(new BigDecimal("1.20"))
                        .build()));

        assertEquals(new BigDecimal("120.00"), pricingService.calculateSeatPrice(show, regularSeat));
        assertEquals(new BigDecimal("240.00"), pricingService.calculateSeatPrice(show, premiumSeat));
    }

    @Test
    void appliesValidDiscount() {
        when(discountCodeRepository.findByCodeIgnoreCase("SAVE10"))
                .thenReturn(Optional.of(DiscountCode.builder()
                        .code("SAVE10")
                        .percentageOff(new BigDecimal("10"))
                        .active(true)
                        .validFrom(LocalDateTime.now().minusDays(1))
                        .validUntil(LocalDateTime.now().plusDays(1))
                        .build()));

        BigDecimal percent = pricingService.resolveDiscountPercent("SAVE10");
        assertEquals(new BigDecimal("10"), percent);
        assertEquals(new BigDecimal("50.00"),
                pricingService.applyDiscount(new BigDecimal("500.00"), percent));
    }

    @Test
    void rejectsInvalidDiscount() {
        when(discountCodeRepository.findByCodeIgnoreCase("BAD"))
                .thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> pricingService.resolveDiscountPercent("BAD"));
    }
}
