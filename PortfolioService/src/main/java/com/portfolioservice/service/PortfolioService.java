package com.portfolioservice.service;

import com.portfolioservice.dal.entity.Portfolio;
import com.portfolioservice.dal.repository.PortfolioRepository;
import com.portfolioservice.enums.StatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    public Long createPortfolio(Long investorId, String portfolioName) {

        if(portfolioRepository.existsByInvestorIdAndPortfolioName(investorId, portfolioName)) {
            // throw error
        }

        Portfolio portfolio = Portfolio.builder().investorId(investorId).portfolioName(portfolioName)
                .status(StatusEnum.A).build();

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        return savedPortfolio.getId();
    }
}
