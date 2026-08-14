package com.alertservice.service.evaluator;

import com.alertservice.dal.entity.AlertConfiguration;
import com.alertservice.dal.entity.AlertPortfolioHoldings;
import com.alertservice.dal.enums.AlertTypeEnum;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// "Drift" is approximated as single-symbol concentration risk — no target-allocation model exists in this domain.
@Component
public class PortfolioDriftAlertEvaluator implements AlertEvaluator {

    @Override
    public AlertTypeEnum getType() {
        return AlertTypeEnum.PORTFOLIO_DRIFT;
    }

    @Override
    public Optional<AlertEvaluationResult> evaluate(AlertConfiguration config, AlertEvaluationInput input) {
        List<AlertPortfolioHoldings> holdings = input.portfolioHoldings();
        if (CollectionUtils.isEmpty(holdings)) {
            return Optional.empty();
        }

        BigDecimal totalValue = holdings.stream()
                .map(this::currentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }

        AlertPortfolioHoldings largestHolding = holdings.stream()
                .max(Comparator.comparing(this::currentValue))
                .orElse(null);

        if (largestHolding == null) {
            return Optional.empty();
        }

        BigDecimal concentrationPercentage = currentValue(largestHolding)
                .divide(totalValue, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        if (!ConditionMatcher.matches(config.getCondition(), concentrationPercentage, config.getThresholdValue())) {
            return Optional.empty();
        }

        String message = "Portfolio %d concentration in %s reached %s%%, exceeding your %s%% drift threshold".formatted(
                config.getPortfolioId(),
                largestHolding.getSymbol(),
                concentrationPercentage.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                config.getThresholdValue().setScale(2, RoundingMode.HALF_UP).toPlainString());
        return Optional.of(new AlertEvaluationResult(concentrationPercentage, message));
    }

    private BigDecimal currentValue(AlertPortfolioHoldings holding) {
        return holding.getLatestMarketPrice().multiply(BigDecimal.valueOf(holding.getQuantity()));
    }
}
