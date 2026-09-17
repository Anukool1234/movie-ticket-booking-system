package com.movieticket.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundPolicyRequest {
    @NotBlank
    private String name;

    @Min(0)
    private int minHoursBeforeShow;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal refundPercentage;

    private boolean active = true;
}
