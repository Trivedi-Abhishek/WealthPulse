package com.roboadvisorservice.dal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MarketStockPriceEvent(String symbol, BigDecimal price, BigDecimal previousPrice,
                                    BigDecimal absoluteChange, BigDecimal percentageChange,
                                    LocalDate tradingDate, Instant eventTime) {
}
