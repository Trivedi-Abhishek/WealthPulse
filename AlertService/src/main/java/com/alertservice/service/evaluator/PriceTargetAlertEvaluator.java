package com.alertservice.service.evaluator;

import com.alertservice.dal.entity.AlertConfiguration;
import com.alertservice.dal.enums.AlertTypeEnum;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

@Component
public class PriceTargetAlertEvaluator implements AlertEvaluator {

    @Override
    public AlertTypeEnum getType() {
        return AlertTypeEnum.PRICE_TARGET;
    }

    @Override
    public Optional<AlertEvaluationResult> evaluate(AlertConfiguration config, AlertEvaluationInput input) {
        if (Objects.isNull(input.priceEvent())) {
            return Optional.empty();
        }

        BigDecimal price = input.priceEvent().price();
        if (!ConditionMatcher.matches(config.getCondition(), price, config.getThresholdValue())) {
            return Optional.empty();
        }

        String message = "%s crossed %s (current: %s)".formatted(
                config.getSymbol(),
                config.getThresholdValue().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                price.setScale(2, RoundingMode.HALF_UP).toPlainString());
        return Optional.of(new AlertEvaluationResult(price, message));
    }
}
