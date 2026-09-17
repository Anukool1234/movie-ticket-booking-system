package com.movieticket.dto;

import com.movieticket.entity.enums.SeatStatus;
import com.movieticket.entity.enums.SeatType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ShowSeatResponse {
    private Long showSeatId;
    private String seatLabel;
    private SeatType seatType;
    private SeatStatus status;
    private BigDecimal price;
}
