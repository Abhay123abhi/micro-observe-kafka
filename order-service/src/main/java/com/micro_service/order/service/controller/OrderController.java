package com.micro_service.order.service.controller;

import com.micro_service.order.service.api.OrderRequest;
import com.micro_service.order.service.api.OrderResponse;
import com.micro_service.order.service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@Valid @RequestBody OrderRequest orderRequest) {
        return orderService.placeOrder(orderRequest);
    }
}
