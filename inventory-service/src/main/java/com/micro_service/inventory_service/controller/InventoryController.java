package com.micro_service.inventory_service.controller;

import com.micro_service.inventory_service.api.InventoryResponse;
import com.micro_service.inventory_service.service.InventoryService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Validated
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public InventoryResponse isInStock(@RequestParam @NotBlank String skuCode,
                                       @RequestParam @Min(1) int quantity) {
        return new InventoryResponse(inventoryService.isInStock(skuCode, quantity));
    }
}
