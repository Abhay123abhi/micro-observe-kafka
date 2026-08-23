package com.micro_service.order.service.client;

import com.micro_service.order.service.api.InventoryResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

public interface InventoryClient {

    Logger log = LoggerFactory.getLogger(InventoryClient.class);

    @GetExchange("/api/inventory")
    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackMethod")
    @Retry(name = "inventory")
    InventoryResponse checkStock(@RequestParam String skuCode, @RequestParam Integer quantity);

    default InventoryResponse fallbackMethod(String code, Integer quantity, Throwable throwable) {
        log.warn("Cannot get inventory for SKU {}", code, throwable);
        return new InventoryResponse(false);
    }
}
