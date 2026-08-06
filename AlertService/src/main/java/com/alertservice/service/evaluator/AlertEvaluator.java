package com.alertservice.service.evaluator;

import com.alertservice.dal.entity.AlertConfiguration;
import com.alertservice.dal.enums.AlertTypeEnum;

import java.util.Optional;

public interface AlertEvaluator {
    AlertTypeEnum getType();
    Optional<AlertEvaluationResult> evaluate(AlertConfiguration config, AlertEvaluationInput input);
}
