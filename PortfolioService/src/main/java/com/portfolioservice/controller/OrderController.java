package com.portfolioservice.controller;

import com.portfolioservice.dal.dto.CreateOrderRequest;
import com.portfolioservice.dal.entity.Order;
import com.portfolioservice.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/buy")
    public ResponseEntity<Long> buyOrder(@RequestBody @Valid CreateOrderRequest createOrderRequest) {
        return ResponseEntity.ok(orderService.buyOrder(createOrderRequest));
    }

    @PostMapping("/sell")
    public ResponseEntity<Long> sellOrder(@RequestBody @Valid CreateOrderRequest createOrderRequest) {
        return ResponseEntity.ok(orderService.sellOrder(createOrderRequest));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(@RequestParam("portfolio_id") @NotNull Long portfolioId) {
        return ResponseEntity.ok(orderService.getOrders(portfolioId));
    }
}
