package com.alertservice.service;

import com.alertservice.dal.dto.PortfolioMetricsEvent;
import com.alertservice.dal.entity.AlertConfiguration;
import com.alertservice.dal.enums.AlertTypeEnum;
import com.alertservice.dal.enums.StatusEnum;
import com.alertservice.dal.repository.AlertConfigurationRepository;
import com.alertservice.service.evaluator.AlertEvaluationInput;
import com.alertservice.service.evaluator.AlertEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PortfolioAlertService {

    private final AlertConfigurationRepository alertConfigurationRepository;
    private final Map<AlertTypeEnum, AlertEvaluator> alertEvaluators;
    private final AlertFiringService alertFiringService;

    public void processPortfolioMetrics(PortfolioMetricsEvent portfolioMetricsEvent) {
        evaluatePortfolioAlerts(AlertTypeEnum.PORTFOLIO_GAIN, portfolioMetricsEvent);
        evaluatePortfolioAlerts(AlertTypeEnum.PORTFOLIO_LOSS, portfolioMetricsEvent);
    }

    private void evaluatePortfolioAlerts(AlertTypeEnum type, PortfolioMetricsEvent event) {
        AlertEvaluator evaluator = alertEvaluators.get(type);
        if (Objects.isNull(evaluator)) {
            return;
        }

        List<AlertConfiguration> configs = alertConfigurationRepository
                .findByPortfolioIdAndAlertTypeAndStatus(event.portfolioId(), type, StatusEnum.A);

        AlertEvaluationInput input = AlertEvaluationInput.ofMetrics(event);
        for (AlertConfiguration config : configs) {
            evaluator.evaluate(config, input).ifPresent(result -> alertFiringService.fire(config, result));
        }
    }
}
