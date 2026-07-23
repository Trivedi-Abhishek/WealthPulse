package com.pnlconsumerservice.dal.repository;

import com.pnlconsumerservice.dal.entity.PortfolioMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PortfolioMetricsRepository extends JpaRepository<PortfolioMetrics, Long> {

    Optional<PortfolioMetrics> findByPortfolioId(Long portfolioId);
}
