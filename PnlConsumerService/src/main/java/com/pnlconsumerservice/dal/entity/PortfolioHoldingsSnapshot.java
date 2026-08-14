package com.pnlconsumerservice.dal.entity;

import com.pnlconsumerservice.dal.enums.StatusEnum;
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
@Table(name="portfolio_holdings_snapshots")
@EntityListeners(AuditingEntityListener.class)
// this entity is for portfolio/symbol performance
public class PortfolioHoldingsSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    @Column(name = "symbol", length=100, nullable = false)
    private String symbol;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Column(name = "average_price", nullable = false)
    private BigDecimal averagePrice;

    @Column(name = "latest_market_price", nullable = false)
    private BigDecimal latestMarketPrice;

    @Column(name="status", nullable=false)
    @Enumerated(EnumType.STRING)
    private StatusEnum status;
}
