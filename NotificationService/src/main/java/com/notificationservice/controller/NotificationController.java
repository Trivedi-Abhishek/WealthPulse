package com.notificationservice.controller;

import com.notificationservice.dal.entity.NotificationHistory;
import com.notificationservice.service.NotificationService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationHistory>> getNotifications(
            @RequestParam("portfolio_id") @NotNull Long portfolioId) {
        return ResponseEntity.ok(notificationService.getNotifications(portfolioId));
    }
}