package com.portfolioservice.dal.dto;

import com.portfolioservice.enums.OrderTypeEnum;
import java.math.BigDecimal;

public record OrderExecutedEvent(Long orderId, Long portfolioId, String symbol, OrderTypeEnum orderType, BigDecimal avgPrice, Long quantity){}