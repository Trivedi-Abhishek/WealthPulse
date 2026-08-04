package com.marketdataservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketdataservice.dal.dto.AlphaVantageResponseDto;
import com.marketdataservice.dal.dto.GlobalQuote;
import com.marketdataservice.dal.dto.MarketStockPriceEvent;
import com.marketdataservice.kafka.producer.MarketDataProducer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlphaVantageService {

    private final WebClient webClient;
    private final MarketDataProducer marketDataProducer;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${alphavantage.api.key}")
    private final String apiKey;

    private static final List<String> SYMBOLS = List.of(
            "AAPL",
            "MSFT",
            "GOOG",
            "AMZN",
            "TSLA"
    );

    @Scheduled(fixedDelay = 30000)
    @CircuitBreaker(name = "alpha-vantage")
    @Retry(name = "alpha-vantage")
    @RateLimiter(name = "alpha-vantage")
    public void pollStockPrices() {
        for(String symbol:SYMBOLS) {
            fetchLatestPrice(symbol);
        }
    }

    private void fetchLatestPrice(String symbol) {

        log.info("Fetching latest price for {}", symbol);
        AlphaVantageResponseDto alphaVantageResponseDto= webClient.get().uri(uriBuilder -> uriBuilder.path("/query")
                        .queryParam("symbol", symbol).queryParam("apikey", apiKey).queryParam("function", "GLOBAL_QUOTE").build())
                .retrieve().bodyToMono(AlphaVantageResponseDto.class).block();

        if(Objects.isNull(alphaVantageResponseDto) ||
                Objects.isNull(alphaVantageResponseDto.getGlobalQuote()) ||
                Objects.isNull(alphaVantageResponseDto.getGlobalQuote().getPrice())){
            log.warn("Invalid response from Alpha Vantage");
            return;
        }

        BigDecimal previousPrice = readCachedPrice(symbol);

        GlobalQuote globalQuote = alphaVantageResponseDto.getGlobalQuote();
        BigDecimal currentPrice = new BigDecimal(globalQuote.getPrice());

        BigDecimal absoluteChange = null;
        BigDecimal percentageChange = null;
        if (Objects.nonNull(previousPrice) && previousPrice.compareTo(BigDecimal.ZERO) != 0) {
            absoluteChange = currentPrice.subtract(previousPrice);
            percentageChange = absoluteChange.divide(previousPrice, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }

        MarketStockPriceEvent marketStockPriceEvent=new MarketStockPriceEvent(globalQuote.getSymbol(),
                currentPrice, previousPrice, absoluteChange, percentageChange,
                LocalDate.parse(globalQuote.getLatestTradingDay(), DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                Instant.now());

        marketDataProducer.publishMarketPriceUpdatedEvent(marketStockPriceEvent);
        log.info("Published market update for {}", symbol);
        cachePrice(symbol, marketStockPriceEvent);
    }

    private BigDecimal readCachedPrice(String symbol) {
        String cached = redisTemplate.opsForValue().get("price:" + symbol);
        if (Objects.isNull(cached)) {
            return null;
        }
        try {
            return objectMapper.readValue(cached, MarketStockPriceEvent.class).price();
        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize cached price for {}", symbol, e);
            return null;
        }
    }

    private void cachePrice(String symbol, MarketStockPriceEvent marketStockPriceEvent) {
        try {
            redisTemplate.opsForValue().set("price:" + symbol,
                    objectMapper.writeValueAsString(marketStockPriceEvent),
                    Duration.ofSeconds(30));
            log.info("Cached latest price for {}", symbol);
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize price for {}", symbol, e);
        }
    }
}
