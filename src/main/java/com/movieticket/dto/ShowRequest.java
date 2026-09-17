package com.movieticket.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShowRequest {
    @NotBlank
    private String movieTitle;

    @NotNull
    private Long theaterId;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal regularPrice;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal premiumPrice;

    private boolean weekendShow;
}
