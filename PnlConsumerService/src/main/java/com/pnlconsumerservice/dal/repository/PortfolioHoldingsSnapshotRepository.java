package com.pnlconsumerservice.dal.repository;

import com.pnlconsumerservice.dal.entity.PortfolioHoldingsSnapshot;
import com.pnlconsumerservice.dal.enums.StatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioHoldingsSnapshotRepository extends JpaRepository<PortfolioHoldingsSnapshot, Long> {
    List<PortfolioHoldingsSnapshot> findBySymbolAndStatus(String symbol, StatusEnum status);

    Optional<PortfolioHoldingsSnapshot> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);

    List<PortfolioHoldingsSnapshot> findByPortfolioIdAndStatus(Long portfolioId, StatusEnum statusEnum);
}
