package com.marketdataservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketdataservice.dal.dto.FinnhubQuoteDto;
import com.marketdataservice.dal.dto.MarketStockPriceEvent;
import com.marketdataservice.kafka.producer.MarketDataProducer;
import com.marketdataservice.service.utils.FinnhubClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinnhubService {

    private final FinnhubClient finnhubClient;
    private final MarketDataProducer marketDataProducer;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /** Finnhub timestamps are exchange time; deriving the trading date in the JVM's zone would shift it. */
    private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");

    /** Comfortably longer than the poll interval, so a cached price is actually there to read. */
    private static final Duration PRICE_CACHE_TTL = Duration.ofMinutes(5);

    private static final List<String> SYMBOLS = List.of(
            "AAPL",
            "MSFT",
            "GOOG",
            "AMZN",
            "TSLA"
    );

    /**
     * Five symbols every 30s is 10 requests/min, well inside Finnhub's free-tier allowance.
     *
     * <p>Each symbol is isolated: previously one malformed response threw out of the loop and
     * silently skipped every remaining symbol for that cycle. The catch sits outside
     * FinnhubClient's proxy, so the circuit breaker still records the failure before we
     * swallow it here.
     */
    @Scheduled(fixedDelay = 30000)
    public void pollStockPrices() {
        for (String symbol : SYMBOLS) {
            try {
                fetchLatestPrice(symbol);
            } catch (CallNotPermittedException e) {
                log.warn("Finnhub circuit breaker is open, skipping {} for this cycle", symbol);
            } catch (Exception e) {
                log.error("Failed to fetch price for {}, continuing with remaining symbols", symbol, e);
            }
        }
    }

    private void fetchLatestPrice(String symbol) {

        log.info("Fetching latest price for {}", symbol);
        FinnhubQuoteDto quote = finnhubClient.getQuote(symbol);

        if (!isUsable(quote, symbol)) {
            return;
        }

        // previousPrice/absoluteChange/percentageChange all come from Finnhub and are measured
        // against the previous close, the conventional meaning. The earlier implementation
        // derived them by diffing the Redis-cached price from the last poll, which measured
        // something different: movement over the preceding 30 seconds.
        MarketStockPriceEvent marketStockPriceEvent = new MarketStockPriceEvent(
                symbol,
                quote.getCurrentPrice(),
                quote.getPreviousClose(),
                quote.getChange(),
                quote.getPercentChange(),
                Instant.ofEpochSecond(quote.getQuoteTimestamp()).atZone(MARKET_ZONE).toLocalDate(),
                Instant.now());

        marketDataProducer.publishMarketPriceUpdatedEvent(marketStockPriceEvent);
        log.info("Published market update for {}", symbol);
        cachePrice(symbol, marketStockPriceEvent);
    }

    /**
     * Finnhub answers an unknown symbol with HTTP 200 and a body of zeros rather than an error,
     * so a zero price or zero timestamp is the only signal that the symbol did not resolve.
     * Logs the symbol - the previous "Invalid response from Alpha Vantage" named neither the
     * symbol nor the body, which made this exact case undiagnosable from the logs.
     */
    private boolean isUsable(FinnhubQuoteDto quote, String symbol) {
        if (Objects.isNull(quote)) {
            log.warn("Empty response from Finnhub for {}", symbol);
            return false;
        }
        if (Objects.isNull(quote.getCurrentPrice()) || quote.getCurrentPrice().compareTo(BigDecimal.ZERO) <= 0
                || Objects.isNull(quote.getQuoteTimestamp()) || quote.getQuoteTimestamp() == 0L) {
            log.warn("Finnhub returned no quote for {} (unknown symbol or no trading data): {}", symbol, quote);
            return false;
        }
        return true;
    }

    /**
     * Last-known price per symbol. No longer feeds change calculation - Finnhub supplies that -
     * so this is now a read model for operational visibility rather than load-bearing state.
     */
    private void cachePrice(String symbol, MarketStockPriceEvent marketStockPriceEvent) {
        try {
            redisTemplate.opsForValue().set("price:" + symbol,
                    objectMapper.writeValueAsString(marketStockPriceEvent),
                    PRICE_CACHE_TTL);
            log.debug("Cached latest price for {}", symbol);
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize price for {}", symbol, e);
        }
    }
}
