package com.roboadvisorservice.dal.repository;

import com.roboadvisorservice.dal.entity.RoboPortfolioHolding;
import com.roboadvisorservice.dal.enums.StatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoboPortfolioHoldingRepository extends JpaRepository<RoboPortfolioHolding, Long> {
    Optional<RoboPortfolioHolding> findByPortfolioIdAndSymbolAndStatus(Long portfolioId, String symbol, StatusEnum status);
    List<RoboPortfolioHolding> findBySymbolAndStatus(String symbol, StatusEnum status);
    List<RoboPortfolioHolding> findByPortfolioIdAndStatus(Long portfolioId, StatusEnum status);
}
