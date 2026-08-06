package com.alertservice.service.evaluator;

import com.alertservice.dal.enums.AlertConditionEnum;

import java.math.BigDecimal;

public final class ConditionMatcher {

    private ConditionMatcher() {
    }

    public static boolean matches(AlertConditionEnum condition, BigDecimal actual, BigDecimal threshold) {
        return switch (condition) {
            case GREATER_THAN -> actual.compareTo(threshold) > 0;
            case LESS_THAN -> actual.compareTo(threshold) < 0;
        };
    }
}
