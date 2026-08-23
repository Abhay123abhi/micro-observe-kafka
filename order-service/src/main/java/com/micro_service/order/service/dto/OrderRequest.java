package com.micro_service.order.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderRequest(
        @NotBlank String skuCode,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @NotNull @Min(1) Integer quantity,
        @NotNull @Valid UserDetails userDetails) {

    public record UserDetails(
            @NotBlank @Email String email,
            @NotBlank String firstName,
            @NotBlank String lastName) {
    }
}
