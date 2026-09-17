package com.movieticket.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PricingTierRequest {
    @NotBlank
    private String name;

    @NotNull
    @DecimalMin("1.0")
    private BigDecimal weekendMultiplier;
}
