package com.movieticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TheaterRequest {
    @NotBlank
    private String name;

    private String address;

    @NotNull
    private Long cityId;
}
