package com.alertservice.kafka.consumer;

import com.alertservice.dal.dto.HoldingUpdatedEvent;
import com.alertservice.dal.dto.MarketStockPriceEvent;
import com.alertservice.dal.dto.PortfolioMetricsEvent;
import com.alertservice.service.AlertPortfolioHoldingService;
import com.alertservice.service.PortfolioAlertService;
import com.alertservice.service.PriceAlertService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Transactional
@Service
public class AlertServiceConsumer {

    private final ObjectMapper objectMapper;
    private final PriceAlertService priceAlertService;
    private final AlertPortfolioHoldingService alertPortfolioHoldingService;
    private final PortfolioAlertService portfolioAlertService;

    @KafkaListener(topics = "market.price.updated", groupId = "wealth-plus-service-group")
    public void listenMarketStockPriceEvent(String srcMarketStockPriceEvent) {

        try {
            MarketStockPriceEvent marketStockPriceEvent = objectMapper.readValue(srcMarketStockPriceEvent, MarketStockPriceEvent.class);

            //generate alert and logs based on the pnl
            priceAlertService.processMarketPriceUpdate(marketStockPriceEvent);

        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize MarketStockPriceEvent {} ", srcMarketStockPriceEvent, e);
        }
    }

    @KafkaListener(topics = "portfolio.holdings.updated", groupId = "wealth-plus-service-group")
    public void listenPortfolioHoldingsUpdated(String srcHoldingUpdatedEvent) {

        try {
            HoldingUpdatedEvent holdingUpdatedEvent = objectMapper.readValue(srcHoldingUpdatedEvent, HoldingUpdatedEvent.class);

            //insert/update AlertPortfolioHoldings
            alertPortfolioHoldingService.updateHolding(holdingUpdatedEvent);

        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize HoldingUpdatedEvent {} ", srcHoldingUpdatedEvent, e);
        }
    }

    @KafkaListener(topics = "portfolio.metrics.updated", groupId = "wealth-plus-service-group")
    public void listenPortfolioMetricsUpdated(String srcPortfolioMetricsEvent) {

        try {
            PortfolioMetricsEvent portfolioMetricsEvent = objectMapper.readValue(srcPortfolioMetricsEvent, PortfolioMetricsEvent.class);

            //generate alert and logs based on the pnl
            portfolioAlertService.processPortfolioMetrics(portfolioMetricsEvent);
        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize PortfolioMetricsEvent {} ", srcPortfolioMetricsEvent, e);
        }
    }
}
