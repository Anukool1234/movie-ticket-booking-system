package com.movieticket.service;

import com.movieticket.dto.*;
import com.movieticket.entity.*;
import com.movieticket.entity.enums.SeatStatus;
import com.movieticket.entity.enums.SeatType;
import com.movieticket.exception.BusinessException;
import com.movieticket.exception.ResourceNotFoundException;
import com.movieticket.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for administrator-only catalog operations.
 *
 * <p>The controller checks the ADMIN role, while this service validates business rules and
 * saves data. Keeping those responsibilities separate makes each class easier to understand:
 * controllers handle HTTP and services handle application behavior.</p>
 *
 * <p>The catalog hierarchy is: City → Theater → physical Seat. A Show belongs to a theater,
 * and each physical seat is copied into a ShowSeat record so its availability can be tracked
 * separately for every show.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final CityRepository cityRepository;
    private final TheaterRepository theaterRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final PricingTierRepository pricingTierRepository;
    private final RefundPolicyRepository refundPolicyRepository;

    // ---- City management ----
    public City createCity(CityRequest request) {
        if (cityRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException("City already exists");
        }
        return cityRepository.save(City.builder().name(request.getName().trim()).build());
    }

    public List<City> listCities() {
        return cityRepository.findAll();
    }

    // ---- Theater management ----
    public Theater createTheater(TheaterRequest request) {
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found"));
        Theater theater = Theater.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(city)
                .build();
        return theaterRepository.save(theater);
    }

    public List<Theater> listTheaters(Long cityId) {
        if (cityId != null) {
            return theaterRepository.findByCityId(cityId);
        }
        return theaterRepository.findAll();
    }

    public Theater getTheater(Long id) {
        return theaterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theater not found"));
    }

    // ---- Physical seat-layout management ----
    /**
     * Generates a simple rectangular seat layout.
     *
     * <p>Example: rows A-C with 5 seats creates A1...A5, B1...B5 and C1...C5.
     * {@code premiumFromRow=C} marks row C and every following row as PREMIUM.</p>
     *
     * <p>This project allows one layout per theater to keep the take-home design simple.
     * The transaction prevents a partially generated layout if saving any seat fails.</p>
     */
    @Transactional
    public List<Seat> createSeatLayout(Long theaterId, SeatLayoutRequest request) {
        Theater theater = getTheater(theaterId);

        if (!seatRepository.findByTheaterId(theaterId).isEmpty()) {
            throw new BusinessException("Seat layout already exists for this theater");
        }

        // Only the first character is used because row labels are intentionally A, B, C, etc.
        char start = request.getStartRow().toUpperCase().charAt(0);
        char end = request.getEndRow().toUpperCase().charAt(0);
        Character premiumFrom = request.getPremiumFromRow() == null
                ? null
                : request.getPremiumFromRow().toUpperCase().charAt(0);

        if (start > end) {
            throw new BusinessException("startRow must be before or equal to endRow");
        }

        List<Seat> seats = new ArrayList<>();
        for (char row = start; row <= end; row++) {
            SeatType type = (premiumFrom != null && row >= premiumFrom)
                    ? SeatType.PREMIUM
                    : SeatType.REGULAR;
            for (int n = 1; n <= request.getSeatsPerRow(); n++) {
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

    public List<Seat> getSeats(Long theaterId) {
        getTheater(theaterId);
        return seatRepository.findByTheaterId(theaterId);
    }

    // ---- Show management ----
    /**
     * Creates a show and initializes one AVAILABLE ShowSeat for every physical theater seat.
     *
     * <p>Seat describes a location such as A1. ShowSeat describes whether A1 is available,
     * held, or booked for this particular show.</p>
     */
    @Transactional
    public Show createShow(ShowRequest request) {
        Theater theater = getTheater(request.getTheaterId());
        List<Seat> seats = seatRepository.findByTheaterId(theater.getId());
        if (seats.isEmpty()) {
            throw new BusinessException("Create seat layout before creating a show");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("endTime must be after startTime");
        }

        Show show = Show.builder()
                .movieTitle(request.getMovieTitle())
                .theater(theater)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .regularPrice(request.getRegularPrice())
                .premiumPrice(request.getPremiumPrice())
                .weekendShow(request.isWeekendShow())
                .build();
        show = showRepository.save(show);

        // These rows are the inventory that BookingService later holds and books.
        List<ShowSeat> showSeats = new ArrayList<>();
        for (Seat seat : seats) {
            showSeats.add(ShowSeat.builder()
                    .show(show)
                    .seat(seat)
                    .status(SeatStatus.AVAILABLE)
                    .build());
        }
        showSeatRepository.saveAll(showSeats);
        return show;
    }

    public List<Show> listShows(Long cityId, Long theaterId) {
        if (theaterId != null) {
            return showRepository.findByTheaterId(theaterId);
        }
        if (cityId != null) {
            return showRepository.findByTheaterCityId(cityId);
        }
        return showRepository.findAll();
    }

    public Show getShow(Long id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found"));
    }

    // ---- Discount-code management ----
    public DiscountCode createDiscount(DiscountCodeRequest request) {
        if (discountCodeRepository.findByCodeIgnoreCase(request.getCode()).isPresent()) {
            throw new BusinessException("Discount code already exists");
        }
        return discountCodeRepository.save(DiscountCode.builder()
                .code(request.getCode().toUpperCase())
                .percentageOff(request.getPercentageOff())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .active(request.isActive())
                .build());
    }

    public List<DiscountCode> listDiscounts() {
        return discountCodeRepository.findAll();
    }

    // ---- Weekend pricing management ----
    /**
     * Updates a tier when its name exists, otherwise creates it.
     * "Upsert" is short for update-or-insert.
     */
    public PricingTier upsertPricingTier(PricingTierRequest request) {
        PricingTier tier = pricingTierRepository.findByName(request.getName())
                .orElse(PricingTier.builder().name(request.getName()).build());
        tier.setWeekendMultiplier(request.getWeekendMultiplier());
        return pricingTierRepository.save(tier);
    }

    public List<PricingTier> listPricingTiers() {
        return pricingTierRepository.findAll();
    }

    // ---- Refund-policy management ----
    public RefundPolicy createRefundPolicy(RefundPolicyRequest request) {
        return refundPolicyRepository.save(RefundPolicy.builder()
                .name(request.getName())
                .minHoursBeforeShow(request.getMinHoursBeforeShow())
                .refundPercentage(request.getRefundPercentage())
                .active(request.isActive())
                .build());
    }

    public List<RefundPolicy> listRefundPolicies() {
        return refundPolicyRepository.findAll();
    }
}
