package com.micro_service.inventory_service.service;

import com.micro_service.inventory_service.repository.InventoryRepository;
import com.micro_service.inventory_service.demo.FailureMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final FailureMode failureMode;

    public InventoryService(InventoryRepository inventoryRepository, FailureMode failureMode) {
        this.inventoryRepository = inventoryRepository;
        this.failureMode = failureMode;
    }

    @Transactional(readOnly = true)
    public boolean isInStock(String skuCode, int quantity) {
        failureMode.apply();
        return inventoryRepository.existsBySkuCodeAndQuantityGreaterThanEqual(skuCode, quantity);
    }
}
