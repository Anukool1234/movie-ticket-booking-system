package com.movieticket.config;

import com.movieticket.entity.*;
import com.movieticket.entity.enums.Role;
import com.movieticket.entity.enums.SeatStatus;
import com.movieticket.entity.enums.SeatType;
import com.movieticket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds demo data on startup so the API is ready to try immediately.
 *
 * Demo users:
 *  admin@movie.com / admin123   (ADMIN)
 *  user@movie.com  / user123    (CUSTOMER)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final TheaterRepository theaterRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final PricingTierRepository pricingTierRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final RefundPolicyRepository refundPolicyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // already seeded
        }

        log.info("Seeding demo data...");

        User admin = userRepository.save(User.builder()
                .email("admin@movie.com")
                .password(passwordEncoder.encode("admin123"))
                .fullName("System Admin")
                .role(Role.ADMIN)
                .build());

        userRepository.save(User.builder()
                .email("user@movie.com")
                .password(passwordEncoder.encode("user123"))
                .fullName("Demo Customer")
                .role(Role.CUSTOMER)
                .build());

        pricingTierRepository.save(PricingTier.builder()
                .name("DEFAULT")
                .weekendMultiplier(new BigDecimal("1.20"))
                .build());

        discountCodeRepository.save(DiscountCode.builder()
                .code("SAVE10")
                .percentageOff(new BigDecimal("10"))
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusMonths(1))
                .active(true)
                .build());

        // Refund rules: ordered by hours descending when applied
        refundPolicyRepository.save(RefundPolicy.builder()
                .name("Full refund 24h+")
                .minHoursBeforeShow(24)
                .refundPercentage(new BigDecimal("100"))
                .active(true)
                .build());
        refundPolicyRepository.save(RefundPolicy.builder()
                .name("Half refund 6h+")
                .minHoursBeforeShow(6)
                .refundPercentage(new BigDecimal("50"))
                .active(true)
                .build());
        refundPolicyRepository.save(RefundPolicy.builder()
                .name("No refund under 6h")
                .minHoursBeforeShow(0)
                .refundPercentage(BigDecimal.ZERO)
                .active(true)
                .build());

        City mumbai = cityRepository.save(City.builder().name("Mumbai").build());
        City delhi = cityRepository.save(City.builder().name("Delhi").build());

        Theater pvr = theaterRepository.save(Theater.builder()
                .name("PVR Phoenix")
                .address("Lower Parel")
                .city(mumbai)
                .build());
        Theater inox = theaterRepository.save(Theater.builder()
                .name("INOX Saket")
                .address("Saket Mall")
                .city(delhi)
                .build());

        List<Seat> pvrSeats = createSeats(pvr, 'A', 'D', 5, 'C');
        createSeats(inox, 'A', 'C', 4, 'C');

        Show show1 = showRepository.save(Show.builder()
                .movieTitle("Inception")
                .theater(pvr)
                .startTime(LocalDateTime.now().plusDays(2).withHour(18).withMinute(0).withSecond(0).withNano(0))
                .endTime(LocalDateTime.now().plusDays(2).withHour(20).withMinute(30).withSecond(0).withNano(0))
                .regularPrice(new BigDecimal("250.00"))
                .premiumPrice(new BigDecimal("400.00"))
                .weekendShow(false)
                .build());

        Show show2 = showRepository.save(Show.builder()
                .movieTitle("Interstellar")
                .theater(pvr)
                .startTime(LocalDateTime.now().plusDays(3).withHour(20).withMinute(0).withSecond(0).withNano(0))
                .endTime(LocalDateTime.now().plusDays(3).withHour(22).withMinute(45).withSecond(0).withNano(0))
                .regularPrice(new BigDecimal("300.00"))
                .premiumPrice(new BigDecimal("500.00"))
                .weekendShow(true)
                .build());

        createShowSeats(show1, pvrSeats);
        createShowSeats(show2, pvrSeats);

        log.info("Seed complete. Admin id={}, demo show ids: {}, {}", admin.getId(), show1.getId(), show2.getId());
    }

    private List<Seat> createSeats(Theater theater, char start, char end, int perRow, char premiumFrom) {
        List<Seat> seats = new ArrayList<>();
        for (char row = start; row <= end; row++) {
            SeatType type = row >= premiumFrom ? SeatType.PREMIUM : SeatType.REGULAR;
            for (int n = 1; n <= perRow; n++) {
                seats.add(Seat.builder()
                        .theater(theater)
                        .rowLabel(String.valueOf(row))
                        .seatNumber(n)
                        .seatType(type)
                        .build());
            }
        }
        return seatRepository.saveAll(seats);
    }

    private void createShowSeats(Show show, List<Seat> seats) {
        List<ShowSeat> list = new ArrayList<>();
        for (Seat seat : seats) {
            list.add(ShowSeat.builder()
                    .show(show)
                    .seat(seat)
                    .status(SeatStatus.AVAILABLE)
                    .build());
        }
        showSeatRepository.saveAll(list);
    }
}
