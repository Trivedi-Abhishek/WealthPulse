package com.alertservice.service;

import com.alertservice.dal.dto.MarketStockPriceEvent;
import com.alertservice.dal.entity.AlertConfiguration;
import com.alertservice.dal.entity.AlertPortfolioHoldings;
import com.alertservice.dal.enums.AlertTypeEnum;
import com.alertservice.dal.enums.StatusEnum;
import com.alertservice.dal.repository.AlertConfigurationRepository;
import com.alertservice.dal.repository.AlertPortfolioHoldingsRepository;
import com.alertservice.service.evaluator.AlertEvaluationInput;
import com.alertservice.service.evaluator.AlertEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PriceAlertService {

    private final AlertPortfolioHoldingsRepository alertPortfolioHoldingsRepository;
    private final AlertConfigurationRepository alertConfigurationRepository;
    private final Map<AlertTypeEnum, AlertEvaluator> alertEvaluators;
    private final AlertFiringService alertFiringService;

    public void processMarketPriceUpdate(MarketStockPriceEvent marketStockPriceEvent) {

        List<AlertPortfolioHoldings> holdings = alertPortfolioHoldingsRepository
                .findBySymbolAndStatus(marketStockPriceEvent.symbol(), StatusEnum.A);

        for (AlertPortfolioHoldings holding : holdings) {
            holding.setLatestMarketPrice(marketStockPriceEvent.price());
        }
        alertPortfolioHoldingsRepository.saveAll(holdings);

        evaluateSymbolAlerts(AlertTypeEnum.PRICE_TARGET, marketStockPriceEvent);
        evaluateSymbolAlerts(AlertTypeEnum.STOP_LOSS, marketStockPriceEvent);

        Set<Long> affectedPortfolios = holdings.stream()
                .map(AlertPortfolioHoldings::getPortfolioId)
                .collect(Collectors.toSet());
        affectedPortfolios.forEach(this::evaluateDrift);
    }

    private void evaluateSymbolAlerts(AlertTypeEnum type, MarketStockPriceEvent event) {
        AlertEvaluator evaluator = alertEvaluators.get(type);
        if (Objects.isNull(evaluator)) {
            return;
        }

        List<AlertConfiguration> configs = alertConfigurationRepository
                .findBySymbolAndAlertTypeAndStatus(event.symbol(), type, StatusEnum.A);

        AlertEvaluationInput input = AlertEvaluationInput.ofPrice(event);
        for (AlertConfiguration config : configs) {
            evaluator.evaluate(config, input).ifPresent(result -> alertFiringService.fire(config, result));
        }
    }

    private void evaluateDrift(Long portfolioId) {
        AlertEvaluator evaluator = alertEvaluators.get(AlertTypeEnum.PORTFOLIO_DRIFT);
        if (Objects.isNull(evaluator)) {
            return;
        }

        List<AlertConfiguration> configs = alertConfigurationRepository
                .findByPortfolioIdAndAlertTypeAndStatus(portfolioId, AlertTypeEnum.PORTFOLIO_DRIFT, StatusEnum.A);
        if (configs.isEmpty()) {
            return;
        }

        List<AlertPortfolioHoldings> portfolioHoldings = alertPortfolioHoldingsRepository
                .findByPortfolioIdAndStatus(portfolioId, StatusEnum.A);
        AlertEvaluationInput input = AlertEvaluationInput.ofHoldings(portfolioHoldings);

        for (AlertConfiguration config : configs) {
            evaluator.evaluate(config, input).ifPresent(result -> alertFiringService.fire(config, result));
        }
    }
}
