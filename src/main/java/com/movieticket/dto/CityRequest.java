package com.movieticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CityRequest {
    @NotBlank
    private String name;
}
