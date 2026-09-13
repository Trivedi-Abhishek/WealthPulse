package com.marketdataservice.service.utils;

import com.marketdataservice.dal.dto.FinnhubQuoteDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Thin wrapper around Finnhub's quote endpoint, mirroring RoboAdvisorService's
 * {@code OpenAiChatClient}: the external call lives in its own bean so the resilience4j
 * aspects actually apply.
 *
 * <p>This separation is load-bearing, not stylistic. The annotations below only take effect
 * when the method is invoked through the Spring proxy. Putting them on a private helper that
 * the scheduler's own class calls directly - as the previous Alpha Vantage code did at batch
 * level - means self-invocation bypasses the proxy entirely.
 *
 * <p>No {@code fallbackMethod} is declared on purpose. Resilience4j orders the aspects
 * Retry(CircuitBreaker(...)), so a fallback on the inner CircuitBreaker would swallow the
 * exception and hand the outer Retry a normal return value, silently disabling retries.
 * The caller catches per symbol instead.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FinnhubClient {

    private final WebClient webClient;

    // Deliberately not final: @RequiredArgsConstructor would move a final field into the
    // generated constructor, and Lombok does not copy @Value onto constructor parameters
    // (there is no lombok.config here), so Spring would look for a String bean and fail.
    @Value("${finnhub.api.key}")
    private String apiKey;

    /**
     * Fetches the latest quote for one symbol.
     *
     * <p>Finnhub signals throttling with a real HTTP 429, which WebClient's {@code retrieve()}
     * turns into a WebClientResponseException. That matters: Alpha Vantage answered 200 with an
     * apology body, so nothing ever threw, the circuit breaker recorded throttled calls as
     * successes, and the retry config was decorative.
     */
    @CircuitBreaker(name = "finnhub")
    @Retry(name = "finnhub")
    @RateLimiter(name = "finnhub")
    public FinnhubQuoteDto getQuote(String symbol) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/quote")
                        .queryParam("symbol", symbol)
                        .queryParam("token", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(FinnhubQuoteDto.class)
                .block();
    }
}
