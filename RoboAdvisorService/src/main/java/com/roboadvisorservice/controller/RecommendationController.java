package com.roboadvisorservice.controller;

import com.roboadvisorservice.dal.entity.Recommendation;
import com.roboadvisorservice.service.RecommendationService;
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
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@Validated
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<List<Recommendation>> getRecommendations(
            @RequestParam("portfolio_id") @NotNull Long portfolioId) {
        return ResponseEntity.ok(recommendationService.getRecommendations(portfolioId));
    }
}
