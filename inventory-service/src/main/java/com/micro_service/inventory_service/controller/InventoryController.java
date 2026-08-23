package com.micro_service.inventory_service.controller;

import com.micro_service.inventory_service.api.InventoryResponse;
import com.micro_service.inventory_service.service.InventoryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@Validated
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public InventoryResponse isInStock(
            @RequestParam @NotBlank @Size(max = 100) @Pattern(regexp = "[A-Za-z0-9_.-]+") String skuCode,
            @RequestParam @Min(1) @Max(10_000) int quantity) {
        return new InventoryResponse(inventoryService.isInStock(skuCode, quantity));
    }
}
