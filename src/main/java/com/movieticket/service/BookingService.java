package com.movieticket.service;

import com.movieticket.dto.BookingResponse;
import com.movieticket.dto.CreateBookingRequest;
import com.movieticket.dto.PaymentRequest;
import com.movieticket.dto.ShowSeatResponse;
import com.movieticket.entity.*;
import com.movieticket.entity.enums.BookingStatus;
import com.movieticket.entity.enums.PaymentStatus;
import com.movieticket.entity.enums.SeatStatus;
import com.movieticket.exception.BusinessException;
import com.movieticket.exception.ResourceNotFoundException;
import com.movieticket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Contains the complete customer booking workflow:
 *
 * <ol>
 *   <li>{@link #createBooking(Long, CreateBookingRequest)} temporarily holds seats.</li>
 *   <li>{@link #pay(Long, PaymentRequest)} converts those holds into booked seats.</li>
 *   <li>{@link #cancel(Long, Long)} releases booked seats and calculates a refund.</li>
 * </ol>
 *
 * <p>The methods that change booking data are transactional. This means all related changes
 * succeed together or are rolled back together. For example, a booking cannot be saved if
 * updating one of its requested seats fails.</p>
 *
 * <p>Concurrent safety comes from the {@code @Version} field on {@link ShowSeat}. If two
 * customers read the same available seat, only the first update can use the current version.
 * The second update fails instead of allocating the same seat twice.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundPolicyRepository refundPolicyRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;
    private final NotificationService notificationService;

    @Value("${app.seat-hold.duration-minutes}")
    private int holdDurationMinutes;

    /**
     * Builds the seat map shown to a customer.
     *
     * <p>{@link #effectiveStatus(ShowSeat)} makes an expired hold appear available immediately,
     * even if the scheduled cleanup job has not run yet.</p>
     */
    public List<ShowSeatResponse> getShowSeats(Long showId) {
        Show show = getShow(showId);
        return showSeatRepository.findByShowId(showId).stream()
                .map(ss -> ShowSeatResponse.builder()
                        .showSeatId(ss.getId())
                        .seatLabel(ss.getSeat().getLabel())
                        .seatType(ss.getSeat().getSeatType())
                        .status(effectiveStatus(ss))
                        .price(pricingService.calculateSeatPrice(show, ss.getSeat()))
                        .build())
                .toList();
    }

    /**
     * Starts a booking by holding every requested seat for a limited time.
     *
     * <p>The returned booking is {@link BookingStatus#PENDING_PAYMENT}. The customer must pay
     * before {@code holdExpiresAt}; otherwise the seats are released for other customers.</p>
     *
     * @param userId authenticated customer's database id
     * @param request show, requested show-seat ids, and optional discount code
     * @return pending booking with its calculated price and hold expiry time
     */
    @Transactional
    public BookingResponse createBooking(Long userId, CreateBookingRequest request) {
        User user = getUser(userId);
        Show show = getShow(request.getShowId());

        if (show.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Cannot book a show that already started");
        }

        // Query by both show id and seat ids. This prevents a client from mixing seat ids
        // belonging to different shows in one booking.
        List<ShowSeat> seats = showSeatRepository.findByShowIdAndIdIn(show.getId(), request.getShowSeatIds());
        if (seats.size() != request.getShowSeatIds().size()) {
            throw new BusinessException("One or more seats do not belong to this show");
        }

        LocalDateTime holdExpiresAt = LocalDateTime.now().plusMinutes(holdDurationMinutes);
        BigDecimal total = BigDecimal.ZERO;
        List<BookingSeat> bookingSeats = new ArrayList<>();

        try {
            for (ShowSeat showSeat : seats) {
                // A seat can only move from AVAILABLE to HELD here.
                if (effectiveStatus(showSeat) != SeatStatus.AVAILABLE) {
                    throw new BusinessException("Seat " + showSeat.getSeat().getLabel() + " is not available");
                }

                showSeat.setStatus(SeatStatus.HELD);
                showSeat.setHeldBy(user);
                showSeat.setHoldExpiresAt(holdExpiresAt);
                // Saving checks ShowSeat.version. A competing update therefore fails here.
                showSeatRepository.save(showSeat);

                BigDecimal price = pricingService.calculateSeatPrice(show, showSeat.getSeat());
                total = total.add(price);
                bookingSeats.add(BookingSeat.builder().showSeat(showSeat).price(price).build());
            }
        } catch (OptimisticLockingFailureException | org.hibernate.StaleObjectStateException ex) {
            throw new BusinessException("Seat was just taken by another user. Please try again.");
        }

        // Calculate money only after every requested seat has passed availability checks.
        // Any exception still rolls back earlier seat updates because this method is transactional.
        BigDecimal discountPercent = pricingService.resolveDiscountPercent(request.getDiscountCode());
        BigDecimal discountAmount = pricingService.applyDiscount(total, discountPercent);
        BigDecimal finalAmount = total.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);

        Booking booking = Booking.builder()
                .bookingCode(generateCode())
                .user(user)
                .show(show)
                .status(BookingStatus.PENDING_PAYMENT)
                .totalAmount(total)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .discountCodeUsed(request.getDiscountCode() == null ? null : request.getDiscountCode().toUpperCase())
                .createdAt(LocalDateTime.now())
                .holdExpiresAt(holdExpiresAt)
                .build();

        for (BookingSeat bs : bookingSeats) {
            bs.setBooking(booking);
            booking.getSeats().add(bs);
        }

        // Keep a payment record from the start so payment status can be tracked independently.
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(finalAmount)
                .status(PaymentStatus.PENDING)
                .build();
        booking.setPayment(payment);

        booking = bookingRepository.save(booking);
        return toResponse(booking);
    }

    /**
     * Simulates payment and confirms a booking.
     *
     * <p>A successful payment changes every seat from HELD to BOOKED and clears its expiry.
     * If the hold has expired, the booking is expired and payment is rejected.</p>
     */
    @Transactional
    public BookingResponse pay(Long userId, PaymentRequest request) {
        Booking booking = getOwnedBooking(userId, request.getBookingId());

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessException("Booking is not awaiting payment");
        }
        if (booking.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            expireBooking(booking);
            throw new BusinessException("Seat hold expired. Please book again.");
        }

        Payment payment = booking.getPayment();
        if (!request.isSuccess()) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new BusinessException("Payment failed");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setPaidAt(LocalDateTime.now());

        // BOOKED seats no longer need a hold expiry; they remain unavailable until cancellation.
        for (BookingSeat bs : booking.getSeats()) {
            ShowSeat ss = bs.getShowSeat();
            ss.setStatus(SeatStatus.BOOKED);
            ss.setHoldExpiresAt(null);
            showSeatRepository.save(ss);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // @Async notification work runs on another thread and does not delay this API response.
        notificationService.sendConfirmation(
                booking.getUser(), booking.getId(), booking.getBookingCode(), booking.getShow().getMovieTitle());

        return toResponse(booking);
    }

    /**
     * Cancels a confirmed booking, frees its seats, and applies the matching refund policy.
     *
     * <p>Only the booking owner can reach this point because {@link #getOwnedBooking(Long, Long)}
     * verifies ownership before any data is changed.</p>
     */
    @Transactional
    public BookingResponse cancel(Long userId, Long bookingId) {
        Booking booking = getOwnedBooking(userId, bookingId);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Only confirmed bookings can be cancelled");
        }
        if (booking.getShow().getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Cannot cancel after show has started");
        }

        BigDecimal refund = calculateRefund(booking);
        booking.setRefundAmount(refund);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());

        // Cancellation makes all seats available for a new customer.
        for (BookingSeat bs : booking.getSeats()) {
            ShowSeat ss = bs.getShowSeat();
            ss.setStatus(SeatStatus.AVAILABLE);
            ss.setHeldBy(null);
            ss.setHoldExpiresAt(null);
            showSeatRepository.save(ss);
        }

        Payment payment = booking.getPayment();
        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS && refund.compareTo(BigDecimal.ZERO) > 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now());
        }

        bookingRepository.save(booking);

        String refundInfo = "Refund amount: " + refund;
        notificationService.sendCancellation(booking.getUser(), booking.getId(), booking.getBookingCode(), refundInfo);

        return toResponse(booking);
    }

    public List<BookingResponse> history(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public BookingResponse getBooking(Long userId, Long bookingId) {
        return toResponse(getOwnedBooking(userId, bookingId));
    }

    /**
     * Releases unpaid holds whose expiry time has passed.
     *
     * <p>The scheduler calls this method every minute. Seat rows and their parent booking rows
     * are updated in the same transaction so their states remain consistent.</p>
     */
    @Transactional
    public void releaseExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();

        List<ShowSeat> expiredSeats = showSeatRepository.findExpiredHolds(SeatStatus.HELD, now);
        for (ShowSeat ss : expiredSeats) {
            ss.setStatus(SeatStatus.AVAILABLE);
            ss.setHeldBy(null);
            ss.setHoldExpiresAt(null);
        }
        showSeatRepository.saveAll(expiredSeats);

        List<Booking> expiredBookings =
                bookingRepository.findByStatusAndHoldExpiresAtBefore(BookingStatus.PENDING_PAYMENT, now);
        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.EXPIRED);
        }
        bookingRepository.saveAll(expiredBookings);

        if (!expiredSeats.isEmpty() || !expiredBookings.isEmpty()) {
            log.info("Released {} expired holds and expired {} bookings",
                    expiredSeats.size(), expiredBookings.size());
        }
    }

    // ---- Private helper methods: kept here to keep the public workflow easy to follow. ----

    private void expireBooking(Booking booking) {
        for (BookingSeat bs : booking.getSeats()) {
            ShowSeat ss = bs.getShowSeat();
            if (ss.getStatus() == SeatStatus.HELD) {
                ss.setStatus(SeatStatus.AVAILABLE);
                ss.setHeldBy(null);
                ss.setHoldExpiresAt(null);
                showSeatRepository.save(ss);
            }
        }
        booking.setStatus(BookingStatus.EXPIRED);
        bookingRepository.save(booking);
    }

    private BigDecimal calculateRefund(Booking booking) {
        long hoursLeft = Duration.between(LocalDateTime.now(), booking.getShow().getStartTime()).toHours();
        // Policies are sorted from the largest threshold to the smallest. Therefore, the first
        // matching policy is the most specific/best rule for the remaining number of hours.
        List<RefundPolicy> policies = refundPolicyRepository.findByActiveTrueOrderByMinHoursBeforeShowDesc();

        for (RefundPolicy policy : policies) {
            if (hoursLeft >= policy.getMinHoursBeforeShow()) {
                return booking.getFinalAmount()
                        .multiply(policy.getRefundPercentage())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * If a seat is HELD but hold expired, treat it as AVAILABLE for browsing.
     * Actual DB cleanup is done by the scheduler.
     */
    private SeatStatus effectiveStatus(ShowSeat ss) {
        if (ss.getStatus() == SeatStatus.HELD
                && ss.getHoldExpiresAt() != null
                && ss.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            return SeatStatus.AVAILABLE;
        }
        return ss.getStatus();
    }

    private Booking getOwnedBooking(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        // Never trust a booking id supplied by the client without checking its owner.
        if (!booking.getUser().getId().equals(userId)) {
            throw new BusinessException("You can only access your own bookings");
        }
        return booking;
    }

    private Show getShow(Long id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found"));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String generateCode() {
        return "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BookingResponse toResponse(Booking booking) {
        List<String> labels = booking.getSeats().stream()
                .map(bs -> bs.getShowSeat().getSeat().getLabel())
                .toList();
        PaymentStatus payStatus = booking.getPayment() == null ? null : booking.getPayment().getStatus();

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .showId(booking.getShow().getId())
                .movieTitle(booking.getShow().getMovieTitle())
                .theaterName(booking.getShow().getTheater().getName())
                .cityName(booking.getShow().getTheater().getCity().getName())
                .showStartTime(booking.getShow().getStartTime())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .discountAmount(booking.getDiscountAmount())
                .finalAmount(booking.getFinalAmount())
                .discountCodeUsed(booking.getDiscountCodeUsed())
                .createdAt(booking.getCreatedAt())
                .holdExpiresAt(booking.getHoldExpiresAt())
                .refundAmount(booking.getRefundAmount())
                .seatLabels(labels)
                .paymentStatus(payStatus)
                .build();
    }
}
