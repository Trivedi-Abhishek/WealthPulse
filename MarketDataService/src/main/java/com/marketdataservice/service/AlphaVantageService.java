package com.marketdataservice.service;

import com.marketdataservice.dto.AlphaVantageResponseDto;
import com.marketdataservice.dto.GlobalQuote;
import com.marketdataservice.dto.MarketStockPriceEvent;
import com.marketdataservice.kafka.producer.MarketDataProducer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
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
    private final RedisTemplate<String, MarketStockPriceEvent> redisTemplate;

    private String apiKey;

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

        GlobalQuote globalQuote = alphaVantageResponseDto.getGlobalQuote();
        MarketStockPriceEvent marketStockPriceEvent=new MarketStockPriceEvent(globalQuote.getSymbol(),
                new BigDecimal(globalQuote.getPrice()), LocalDate.parse(globalQuote.getLatestTradingDay(), DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                Instant.now());
        marketDataProducer.publishMarketPriceUpdatedEvent(marketStockPriceEvent);
        log.info("Published market update for {}", symbol);
        redisTemplate.opsForValue().set("price:"+symbol,
                marketStockPriceEvent,
                Duration.ofSeconds(30));
        log.info("Cached latest price for {}", symbol);
    }
}
