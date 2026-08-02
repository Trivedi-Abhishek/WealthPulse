package com.alertservice.dal;

import com.alertservice.dal.entity.AlertPortfolioHoldings;
import com.alertservice.dal.enums.StatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlertPortfolioHoldingsRepository extends JpaRepository<AlertPortfolioHoldings, Long> {
    Optional<AlertPortfolioHoldings> findByPortfolioIdAndSymbolAndStatus(Long portfolioId, String symbol, StatusEnum status);
}
