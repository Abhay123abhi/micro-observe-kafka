package com.micro_service.inventory_service.service;

import com.micro_service.inventory_service.demo.FailureMode;
import com.micro_service.inventory_service.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private FailureMode failureMode;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void returnsTrueWhenAvailableQuantityMeetsRequest() {
        when(inventoryRepository.existsBySkuCodeAndQuantityGreaterThanEqual("sku-1", 3)).thenReturn(true);

        boolean inStock = inventoryService.isInStock("sku-1", 3);

        assertThat(inStock).isTrue();
        verify(failureMode).apply();
        verify(inventoryRepository).existsBySkuCodeAndQuantityGreaterThanEqual("sku-1", 3);
    }
}
