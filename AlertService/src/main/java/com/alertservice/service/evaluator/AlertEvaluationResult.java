package com.alertservice.service.evaluator;

import java.math.BigDecimal;

public record AlertEvaluationResult(BigDecimal triggeredValue, String message) {
}
