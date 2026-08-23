package com.microservice.product.service.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String name,
        @NotBlank String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price) {
}
