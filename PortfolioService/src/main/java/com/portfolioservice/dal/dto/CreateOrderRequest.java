package com.portfolioservice.dal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderRequest {

    @NotNull
    @Positive
    @JsonProperty("portfolio_id")
    private Long portfolioId;

    @NotBlank
    @JsonProperty("symbol")
    private String symbol;

    @NotNull
    @Positive
    @JsonProperty("price")
    private BigDecimal price;

    @NotNull
    @Positive
    @JsonProperty("quantity")
    private Long quantity;
}
