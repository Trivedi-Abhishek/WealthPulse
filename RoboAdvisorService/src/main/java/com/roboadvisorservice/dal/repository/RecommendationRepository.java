package com.roboadvisorservice.dal.repository;

import com.roboadvisorservice.dal.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByPortfolioIdOrderByGeneratedAtDesc(Long portfolioId);
}
