package com.pnlconsumerservice.dal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name="portfolio_metrics")
@EntityListeners(AuditingEntityListener.class)
public class PortfolioMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    @Column(name = "current_value", nullable = false)
    private BigDecimal currentValue;

    @Column(name = "invested_amount", nullable = false)
    private BigDecimal investedAmount;

    @Column(name = "pnl", nullable = false)
    private BigDecimal pnl;

    @Column(name = "xirr", nullable = false)
    private BigDecimal xirr;

}
