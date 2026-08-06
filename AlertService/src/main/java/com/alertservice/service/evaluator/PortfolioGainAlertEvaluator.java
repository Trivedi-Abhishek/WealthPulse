package com.alertservice.service.evaluator;

import com.alertservice.dal.entity.AlertConfiguration;
import com.alertservice.dal.enums.AlertTypeEnum;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

@Component
public class PortfolioGainAlertEvaluator implements AlertEvaluator {

    @Override
    public AlertTypeEnum getType() {
        return AlertTypeEnum.PORTFOLIO_GAIN;
    }

    @Override
    public Optional<AlertEvaluationResult> evaluate(AlertConfiguration config, AlertEvaluationInput input) {
        if (Objects.isNull(input.metricsEvent())) {
            return Optional.empty();
        }

        BigDecimal invested = input.metricsEvent().investedAmount();
        if (invested.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }

        BigDecimal pnlPercentage = input.metricsEvent().pnl()
                .divide(invested, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        if (!ConditionMatcher.matches(config.getCondition(), pnlPercentage, config.getThresholdValue())) {
            return Optional.empty();
        }

        String message = "Portfolio %d profit exceeded %s%% (current: %s%%)".formatted(
                config.getPortfolioId(),
                config.getThresholdValue().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                pnlPercentage.setScale(2, RoundingMode.HALF_UP).toPlainString());
        return Optional.of(new AlertEvaluationResult(pnlPercentage, message));
    }
}
