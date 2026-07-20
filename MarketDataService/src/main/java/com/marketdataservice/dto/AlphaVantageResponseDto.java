package com.marketdataservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AlphaVantageResponseDto {

    @JsonProperty("Global Quote")
    private GlobalQuote globalQuote;
}
