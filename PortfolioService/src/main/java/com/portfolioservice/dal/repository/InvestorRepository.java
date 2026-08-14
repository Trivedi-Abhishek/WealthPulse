package com.portfolioservice.dal.repository;

import com.portfolioservice.dal.entity.Investor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestorRepository extends JpaRepository<Investor, Long> {
    boolean existsByEmail(String email);
}
