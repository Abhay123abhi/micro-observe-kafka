package com.micro_service.inventory_service.repository;

import com.micro_service.inventory_service.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    boolean existsBySkuCodeAndQuantity(String skuCode, Integer quantity);
}
