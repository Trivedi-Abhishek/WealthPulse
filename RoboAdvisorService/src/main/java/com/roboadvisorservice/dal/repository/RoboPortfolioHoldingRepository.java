package com.roboadvisorservice.dal.repository;

import com.roboadvisorservice.dal.entity.RoboPortfolioHolding;
import com.roboadvisorservice.dal.enums.StatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoboPortfolioHoldingRepository extends JpaRepository<RoboPortfolioHolding, Long> {
    // Upsert lookup — deliberately not status-filtered, so it finds the inactive row a full
    // sell leaves behind and matches the UNIQUE (portfolio_id, symbol) constraint exactly.
    Optional<RoboPortfolioHolding> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);
    List<RoboPortfolioHolding> findBySymbolAndStatus(String symbol, StatusEnum status);
    List<RoboPortfolioHolding> findByPortfolioIdAndStatus(Long portfolioId, StatusEnum status);
}
