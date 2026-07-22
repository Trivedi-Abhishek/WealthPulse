package com.portfolioservice.controller;

import com.portfolioservice.service.PortfolioService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/investors")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @PostMapping("{investor_id}/portfolios")
    public ResponseEntity<Long> createPortfolio(@PathVariable("investor_id") String investorId, @RequestParam("portfolio_mame") @NotBlank String portfolioName) {

        Long portfolioId = portfolioService.createPortfolio(Long.valueOf(investorId), portfolioName);
        return ResponseEntity.ok(portfolioId);
    }
}
