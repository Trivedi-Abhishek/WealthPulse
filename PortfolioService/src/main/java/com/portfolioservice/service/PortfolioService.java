package com.portfolioservice.service;

import com.portfolioservice.dal.entity.Portfolio;
import com.portfolioservice.dal.repository.InvestorRepository;
import com.portfolioservice.dal.repository.PortfolioRepository;
import com.portfolioservice.enums.StatusEnum;
import com.portfolioservice.exception.DuplicatePortfolioException;
import com.portfolioservice.exception.InvestorNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final InvestorRepository investorRepository;

    public Long createPortfolio(Long investorId, String portfolioName) {

        if(!investorRepository.existsById(investorId)) {
            throw new InvestorNotFoundException(investorId);
        }

        if(portfolioRepository.existsByInvestorIdAndPortfolioName(investorId, portfolioName)) {
            throw new DuplicatePortfolioException(investorId, portfolioName);
        }

        Portfolio portfolio = Portfolio.builder().investorId(investorId).portfolioName(portfolioName)
                .status(StatusEnum.A).build();

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        return savedPortfolio.getId();
    }
}
