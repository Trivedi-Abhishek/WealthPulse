package com.alertservice.dal.repository;

import com.alertservice.dal.entity.AlertPortfolioHoldings;
import com.alertservice.dal.enums.StatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertPortfolioHoldingsRepository extends JpaRepository<AlertPortfolioHoldings, Long> {
    Optional<AlertPortfolioHoldings> findByPortfolioIdAndSymbolAndStatus(Long portfolioId, String symbol, StatusEnum status);
    List<AlertPortfolioHoldings> findBySymbolAndStatus(String symbol, StatusEnum status);
    List<AlertPortfolioHoldings> findByPortfolioIdAndStatus(Long portfolioId, StatusEnum status);
}
