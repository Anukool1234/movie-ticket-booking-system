package com.movieticket.repository;

import com.movieticket.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderBySentAtDesc(Long userId);
    boolean existsByBookingIdAndType(Long bookingId, String type);
}
