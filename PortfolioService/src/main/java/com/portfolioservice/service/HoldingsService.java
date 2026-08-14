package com.portfolioservice.service;

import com.portfolioservice.dal.entity.Holdings;
import com.portfolioservice.dal.repository.HoldingsRepository;
import com.portfolioservice.dal.repository.PortfolioRepository;
import com.portfolioservice.enums.StatusEnum;
import com.portfolioservice.exception.PortfolioNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HoldingsService {

    private final HoldingsRepository holdingsRepository;
    private final PortfolioRepository portfolioRepository;

    public List<Holdings> getHoldings(Long portfolioId) {
        if (!portfolioRepository.existsById(portfolioId)) {
            throw new PortfolioNotFoundException(portfolioId);
        }
        return holdingsRepository.findByPortfolioIdAndStatus(portfolioId, StatusEnum.A);
    }
}
