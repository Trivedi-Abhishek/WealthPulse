package com.portfolioservice.dal.repository;

import com.portfolioservice.dal.entity.Holdings;
import com.portfolioservice.enums.StatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingsRepository  extends JpaRepository<Holdings, Long> {
    Optional<Holdings> findByPortfolioIdAndSymbolAndStatus(Long portfolioId, String symbol, StatusEnum status);
    List<Holdings> findByPortfolioIdAndStatus(Long portfolioId, StatusEnum status);
}
