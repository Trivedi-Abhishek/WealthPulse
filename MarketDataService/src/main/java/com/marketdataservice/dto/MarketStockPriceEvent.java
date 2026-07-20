package com.marketdataservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MarketStockPriceEvent(String symbol, BigDecimal price, LocalDate tradingDate, Instant eventTime) {
}
