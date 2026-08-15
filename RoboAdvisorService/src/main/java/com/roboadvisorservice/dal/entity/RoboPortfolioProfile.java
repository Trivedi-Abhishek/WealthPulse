package com.roboadvisorservice.dal.entity;

import com.roboadvisorservice.dal.enums.RiskProfileEnum;
import com.roboadvisorservice.dal.enums.StatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "robo_portfolio_profile")
@EntityListeners(AuditingEntityListener.class)
public class RoboPortfolioProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", nullable = false, unique = true)
    private Long portfolioId;

    @Column(name = "risk_profile", nullable = false)
    @Enumerated(EnumType.STRING)
    private RiskProfileEnum riskProfile;

    @Column(name = "current_value", nullable = false)
    private BigDecimal currentValue;

    @Column(name = "invested_amount", nullable = false)
    private BigDecimal investedAmount;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusEnum status;

    @Column(name = "pnl")
    private BigDecimal pnl;

    @Column(name = "created_date", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdDate;
}
