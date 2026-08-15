package com.roboadvisorservice.service;

import com.roboadvisorservice.dal.dto.PortfolioRebalanceTriggeredEvent;
import com.roboadvisorservice.dal.entity.Recommendation;
import com.roboadvisorservice.dal.entity.RoboPortfolioHolding;
import com.roboadvisorservice.dal.entity.RoboPortfolioProfile;
import com.roboadvisorservice.dal.enums.StatusEnum;
import com.roboadvisorservice.dal.repository.RecommendationRepository;
import com.roboadvisorservice.dal.repository.RoboPortfolioHoldingRepository;
import com.roboadvisorservice.dal.repository.RoboPortfolioProfileRepository;
import com.roboadvisorservice.service.utils.OpenAiChatClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final RoboPortfolioHoldingRepository roboPortfolioHoldingRepository;
    private final RoboPortfolioProfileRepository roboPortfolioProfileRepository;
    private final RecommendationRepository recommendationRepository;
    private final OpenAiChatClient openAiChatClient;

//    RoboPortfolioHolding->portfolioId, symbol, quantity, latestMarketPrice
//    RoboPortfolioProfile-> portfolioId, riskProfile, currentValue, investedAmount, pnl

    public void generateRecommendation(PortfolioRebalanceTriggeredEvent event) {

        try {
            List<RoboPortfolioHolding> roboPortfolioHoldingList = roboPortfolioHoldingRepository.findByPortfolioIdAndStatus(event.portfolioId(), StatusEnum.A);
            Optional<RoboPortfolioProfile> optionalRoboPortfolioProfile = roboPortfolioProfileRepository.findByPortfolioId(event.portfolioId());

            Map<String, BigDecimal> holdingWeightageMap = new HashMap<>();
            if(optionalRoboPortfolioProfile.isPresent()) {
                RoboPortfolioProfile roboPortfolioProfile = optionalRoboPortfolioProfile.get();
                roboPortfolioHoldingList.forEach(holding->{
                    holdingWeightageMap.putIfAbsent(holding.getSymbol(), holding.getLatestMarketPrice().multiply(BigDecimal.valueOf(holding.getQuantity())));
                });
                String prompt = "User with risk profile:" + roboPortfolioProfile.getRiskProfile() + " has a holding " +
                        "in symbols and their weightage : " + holdingWeightageMap + " in portfolio: " + event.portfolioId() + "having current value: " + roboPortfolioProfile.getCurrentValue() +
                        ", invested amount: " + roboPortfolioProfile.getInvestedAmount() + " and PNL: " + roboPortfolioProfile.getPnl() + ". " +
                        "Please provide recommendation for this holding.";
                String recommendation = openAiChatClient.generate(prompt);
                if(StringUtils.hasText(recommendation)) {
                    Recommendation recommendationEntity = Recommendation.builder().portfolioId(event.portfolioId()).riskProfile(roboPortfolioProfile.getRiskProfile())
                            .portfolioAllocation(String.valueOf(holdingWeightageMap)).pnl(roboPortfolioProfile.getPnl()).prompt(prompt).recommendation(recommendation)
                            .triggerReason(event.reason()).status(StatusEnum.A).generatedAt(Instant.now()).triggeredAt(event.triggeredAt()).build();
                    recommendationRepository.save(recommendationEntity);
                }
            }
        } catch (Exception e) {
            log.error("Error generating recommendation for portfolio {}: {}", event.portfolioId(), e.getMessage());
        }
    }

    public List<Recommendation> getRecommendations(Long portfolioId) {
        return recommendationRepository.findByPortfolioIdOrderByGeneratedAtDesc(portfolioId);
    }
}
