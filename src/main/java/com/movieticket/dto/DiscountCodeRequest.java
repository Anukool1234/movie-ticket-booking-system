package com.movieticket.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DiscountCodeRequest {
    @NotBlank
    private String code;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal percentageOff;

    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private boolean active = true;
}
