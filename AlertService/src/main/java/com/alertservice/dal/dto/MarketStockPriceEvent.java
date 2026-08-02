package com.alertservice.dal.dto;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MarketStockPriceEvent(String symbol, BigDecimal price, BigDecimal previousPrice, LocalDate tradingDate, Instant eventTime) {
}
