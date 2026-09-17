package com.movieticket.dto;

import com.movieticket.entity.enums.BookingStatus;
import com.movieticket.entity.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BookingResponse {
    private Long id;
    private String bookingCode;
    private Long showId;
    private String movieTitle;
    private String theaterName;
    private String cityName;
    private LocalDateTime showStartTime;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String discountCodeUsed;
    private LocalDateTime createdAt;
    private LocalDateTime holdExpiresAt;
    private BigDecimal refundAmount;
    private List<String> seatLabels;
    private PaymentStatus paymentStatus;
}
