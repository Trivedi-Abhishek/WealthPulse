package com.alertservice.service.evaluator;

import com.alertservice.dal.dto.MarketStockPriceEvent;
import com.alertservice.dal.dto.PortfolioMetricsEvent;
import com.alertservice.dal.entity.AlertPortfolioHoldings;

import java.util.List;

public record AlertEvaluationInput(
        MarketStockPriceEvent priceEvent,
        PortfolioMetricsEvent metricsEvent,
        List<AlertPortfolioHoldings> portfolioHoldings
) {
    public static AlertEvaluationInput ofPrice(MarketStockPriceEvent priceEvent) {
        return new AlertEvaluationInput(priceEvent, null, null);
    }

    public static AlertEvaluationInput ofMetrics(PortfolioMetricsEvent metricsEvent) {
        return new AlertEvaluationInput(null, metricsEvent, null);
    }

    public static AlertEvaluationInput ofHoldings(List<AlertPortfolioHoldings> portfolioHoldings) {
        return new AlertEvaluationInput(null, null, portfolioHoldings);
    }
}
