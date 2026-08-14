package com.portfolioservice.controller;

import com.portfolioservice.dal.dto.CreateInvestorRequest;
import com.portfolioservice.service.InvestorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/investors")
@RequiredArgsConstructor
public class InvestorController {

    private final InvestorService investorService;

    @PostMapping
    public ResponseEntity<Long> createInvestor(@RequestBody @Valid CreateInvestorRequest createInvestorRequest) {
        Long investorId = investorService.createInvestor(createInvestorRequest);
        return ResponseEntity.ok(investorId);
    }
}
