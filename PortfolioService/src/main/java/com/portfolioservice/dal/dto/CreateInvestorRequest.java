package com.portfolioservice.dal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.portfolioservice.enums.RiskProfileEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateInvestorRequest {

    @NotBlank
    @JsonProperty("name")
    private String name;

    @NotBlank
    @Email
    @JsonProperty("email")
    private String email;

    @NotNull
    @JsonProperty("risk_profile")
    private RiskProfileEnum riskProfile;
}
