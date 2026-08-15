package com.roboadvisorservice.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roboadvisorservice.dal.dto.HoldingUpdatedEvent;
import com.roboadvisorservice.dal.dto.MarketStockPriceEvent;
import com.roboadvisorservice.dal.dto.PortfolioMetricsEvent;
import com.roboadvisorservice.dal.dto.PortfolioRebalanceTriggeredEvent;
import com.roboadvisorservice.service.RecommendationService;
import com.roboadvisorservice.service.RoboMarketPriceService;
import com.roboadvisorservice.service.RoboMetricsService;
import com.roboadvisorservice.service.RoboPortfolioHoldingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class RoboAdvisorEventConsumer {

    private final ObjectMapper objectMapper;
    private final RoboPortfolioHoldingService roboPortfolioHoldingService;
    private final RoboMarketPriceService roboMarketPriceService;
    private final RoboMetricsService roboMetricsService;
    private final RecommendationService recommendationService;

    @KafkaListener(topics = "portfolio.holdings.updated", groupId = "wealth-plus-service-group")
    public void listenPortfolioHoldingsUpdated(String srcHoldingUpdatedEvent) {
        try {
            HoldingUpdatedEvent holdingUpdatedEvent = objectMapper.readValue(srcHoldingUpdatedEvent, HoldingUpdatedEvent.class);

            roboPortfolioHoldingService.updateHolding(holdingUpdatedEvent);
        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize HoldingUpdatedEvent {}", srcHoldingUpdatedEvent, e);
        }
    }

    @KafkaListener(topics = "market.price.updated", groupId = "wealth-plus-service-group")
    public void listenMarketStockPriceEvent(String srcMarketStockPriceEvent) {
        try {
            MarketStockPriceEvent marketStockPriceEvent = objectMapper.readValue(srcMarketStockPriceEvent, MarketStockPriceEvent.class);

            roboMarketPriceService.processMarketPriceUpdate(marketStockPriceEvent);
        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize MarketStockPriceEvent {}", srcMarketStockPriceEvent, e);
        }
    }

    @KafkaListener(topics = "portfolio.metrics.updated", groupId = "wealth-plus-service-group")
    public void listenPortfolioMetricsUpdated(String srcPortfolioMetricsEvent) {
        try {
            PortfolioMetricsEvent portfolioMetricsEvent = objectMapper.readValue(srcPortfolioMetricsEvent, PortfolioMetricsEvent.class);

            roboMetricsService.processPortfolioMetrics(portfolioMetricsEvent);
        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize PortfolioMetricsEvent {}", srcPortfolioMetricsEvent, e);
        }
    }

    @KafkaListener(topics = "portfolio.rebalance.triggered", groupId = "wealth-plus-service-group")
    public void listenPortfolioRebalanceTriggered(String srcPortfolioRebalanceTriggeredEvent) {
        try {
            PortfolioRebalanceTriggeredEvent portfolioRebalanceTriggeredEvent =
                    objectMapper.readValue(srcPortfolioRebalanceTriggeredEvent, PortfolioRebalanceTriggeredEvent.class);

            recommendationService.generateRecommendation(portfolioRebalanceTriggeredEvent);
        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize PortfolioRebalanceTriggeredEvent {}", srcPortfolioRebalanceTriggeredEvent, e);
        }
    }
}
