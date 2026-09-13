package com.marketdataservice.dal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Response of Finnhub's GET /quote.
 *
 * <p>Unlike Alpha Vantage's nested {@code "Global Quote"} wrapper with numbered string keys,
 * this is flat JSON with real numbers, so the values bind straight to BigDecimal with no
 * string parsing.
 *
 * <p>Unknown-property tolerance is deliberate here and is NOT the convention for the Kafka
 * event records in this package: this is an external vendor payload we do not control, so a
 * field Finnhub adds later must not break polling. Event DTOs keep Jackson's strict default
 * on purpose - see rules/kafka-events.md.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinnhubQuoteDto {

    @JsonProperty("c")
    private BigDecimal currentPrice;

    @JsonProperty("d")
    private BigDecimal change;

    @JsonProperty("dp")
    private BigDecimal percentChange;

    @JsonProperty("h")
    private BigDecimal dayHigh;

    @JsonProperty("l")
    private BigDecimal dayLow;

    @JsonProperty("o")
    private BigDecimal openPrice;

    @JsonProperty("pc")
    private BigDecimal previousClose;

    /** Unix epoch seconds of the quote. Zero when the symbol is unknown. */
    @JsonProperty("t")
    private Long quoteTimestamp;
}
