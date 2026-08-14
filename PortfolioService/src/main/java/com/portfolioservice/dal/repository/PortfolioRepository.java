package com.portfolioservice.dal.repository;

import com.portfolioservice.dal.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    boolean existsByInvestorIdAndPortfolioName(Long investorId, String portfolioName);
}
