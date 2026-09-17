package com.movieticket.scheduler;

import com.movieticket.entity.Booking;
import com.movieticket.entity.enums.BookingStatus;
import com.movieticket.repository.BookingRepository;
import com.movieticket.repository.NotificationRepository;
import com.movieticket.service.BookingService;
import com.movieticket.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Runs the two small background tasks required by the booking flow.
 *
 * <p>Both methods run once per minute. One releases expired seat holds; the other sends a
 * single reminder when a confirmed show is within the configured reminder window.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @Value("${app.notification.reminder-minutes-before}")
    private int reminderMinutesBefore;

    @Scheduled(fixedRate = 60_000) // every 60 seconds
    public void releaseExpiredHolds() {
        bookingService.releaseExpiredHolds();
    }

    @Scheduled(fixedRate = 60_000) // check once per minute for shows starting soon
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusMinutes(reminderMinutesBefore);

        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .filter(b -> {
                    LocalDateTime start = b.getShow().getStartTime();
                    return !start.isBefore(now) && !start.isAfter(windowEnd);
                })
                .filter(b -> !notificationRepository.existsByBookingIdAndType(b.getId(), "REMINDER"))
                .toList();

        for (Booking booking : bookings) {
            notificationService.sendReminder(
                    booking.getUser(),
                    booking.getId(),
                    booking.getBookingCode(),
                    booking.getShow().getMovieTitle(),
                    booking.getShow().getStartTime().toString()
            );
        }

        if (!bookings.isEmpty()) {
            log.info("Queued {} reminder notifications", bookings.size());
        }
    }
}
