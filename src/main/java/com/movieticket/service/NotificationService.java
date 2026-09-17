package com.movieticket.service;

import com.movieticket.entity.Notification;
import com.movieticket.entity.User;
import com.movieticket.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Sends notifications asynchronously so booking APIs are not blocked.
 * Delivery is simulated with logs + DB records (no real email/SMS).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Async
    public void sendConfirmation(User user, Long bookingId, String bookingCode, String movieTitle) {
        String message = "Booking confirmed! Code: " + bookingCode + " for movie: " + movieTitle;
        saveAndLog(user, bookingId, "CONFIRMATION", message);
    }

    @Async
    public void sendReminder(User user, Long bookingId, String bookingCode, String movieTitle, String showTime) {
        // Avoid duplicate reminders for the same booking
        if (notificationRepository.existsByBookingIdAndType(bookingId, "REMINDER")) {
            return;
        }
        String message = "Reminder: Your show for '" + movieTitle + "' (booking " + bookingCode
                + ") starts at " + showTime;
        saveAndLog(user, bookingId, "REMINDER", message);
    }

    @Async
    public void sendCancellation(User user, Long bookingId, String bookingCode, String refundInfo) {
        String message = "Booking " + bookingCode + " cancelled. " + refundInfo;
        saveAndLog(user, bookingId, "CANCELLATION", message);
    }

    private void saveAndLog(User user, Long bookingId, String type, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .bookingId(bookingId)
                .type(type)
                .message(message)
                .sentAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
        log.info("[NOTIFICATION] to={} type={} message={}", user.getEmail(), type, message);
    }
}
