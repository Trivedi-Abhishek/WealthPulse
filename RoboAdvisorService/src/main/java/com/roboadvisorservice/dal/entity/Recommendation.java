package com.roboadvisorservice.dal.entity;

import com.roboadvisorservice.dal.enums.RiskProfileEnum;
import com.roboadvisorservice.dal.enums.StatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "recommendation")
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    @Column(name = "risk_profile", nullable = false)
    @Enumerated(EnumType.STRING)
    private RiskProfileEnum riskProfile;

    @Column(name = "portfolio_allocation", length = 1000, nullable = false)
    private String portfolioAllocation;

    @Column(name = "pnl")
    private BigDecimal pnl;

    @Column(name = "prompt", nullable = false)
    private String prompt;

    @Column(name = "recommendation", nullable = false)
    private String recommendation;

    @Column(name = "trigger_reason", nullable = false)
    private String triggerReason;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusEnum status;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
}
