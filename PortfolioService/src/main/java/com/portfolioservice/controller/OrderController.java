package com.portfolioservice.controller;

import com.portfolioservice.dal.dto.CreateOrderRequest;
import com.portfolioservice.enums.OrderTypeEnum;
import com.portfolioservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Long> processOrder(@RequestBody @Valid CreateOrderRequest createOrderRequest) {
        Long orderId = OrderTypeEnum.BUY.equals(createOrderRequest.getOrderType()) ? orderService.buyOrder(createOrderRequest):orderService.sellOrder(createOrderRequest);
        return ResponseEntity.ok(orderId);
    }

}
