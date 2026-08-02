package com.portfolioservice.controller;

import com.portfolioservice.dal.entity.Holdings;
import com.portfolioservice.service.HoldingsService;
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
@RequestMapping("/holdings")
@RequiredArgsConstructor
@Validated
public class HoldingsController {

    private final HoldingsService holdingsService;

    @GetMapping
    public ResponseEntity<List<Holdings>> getHoldings(@RequestParam("portfolio_id") @NotNull Long portfolioId) {
        return ResponseEntity.ok(holdingsService.getHoldings(portfolioId));
    }
}
