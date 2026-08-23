package com.micro_service.order.service.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OrderRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = "[A-Za-z0-9_.-]+") String skuCode,
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal price,
        @NotNull @Min(1) @Max(10_000) Integer quantity) {
}
