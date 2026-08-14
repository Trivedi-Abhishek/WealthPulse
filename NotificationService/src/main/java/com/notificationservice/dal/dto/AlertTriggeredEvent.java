package com.notificationservice.dal.dto;

import com.notificationservice.dal.enums.AlertTypeEnum;

import java.math.BigDecimal;
import java.time.Instant;

public record AlertTriggeredEvent(
        Long alertConfigurationId,
        Long portfolioId,
        String symbol,
        AlertTypeEnum alertType,
        String message,
        BigDecimal triggeredValue,
        Instant triggeredAt
) {}