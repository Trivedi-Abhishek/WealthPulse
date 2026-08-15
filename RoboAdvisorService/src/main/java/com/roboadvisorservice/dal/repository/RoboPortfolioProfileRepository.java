package com.roboadvisorservice.dal.repository;

import com.roboadvisorservice.dal.entity.RoboPortfolioProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoboPortfolioProfileRepository extends JpaRepository<RoboPortfolioProfile, Long> {
    Optional<RoboPortfolioProfile> findByPortfolioId(Long portfolioId);
}
