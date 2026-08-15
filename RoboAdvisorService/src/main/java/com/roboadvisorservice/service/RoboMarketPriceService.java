package com.roboadvisorservice.service;

import com.roboadvisorservice.dal.dto.MarketStockPriceEvent;
import com.roboadvisorservice.dal.entity.RoboPortfolioHolding;
import com.roboadvisorservice.dal.enums.StatusEnum;
import com.roboadvisorservice.dal.repository.RoboPortfolioHoldingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoboMarketPriceService {

    private final RoboPortfolioHoldingRepository roboPortfolioHoldingRepository;

    public void processMarketPriceUpdate(MarketStockPriceEvent marketStockPriceEvent) {

        List<RoboPortfolioHolding> roboPortfolioHoldingList = roboPortfolioHoldingRepository.findBySymbolAndStatus(marketStockPriceEvent.symbol(), StatusEnum.A);

        roboPortfolioHoldingList
                .forEach(holding -> {
                    holding.setLatestMarketPrice(marketStockPriceEvent.price());
                });

        roboPortfolioHoldingRepository.saveAll(roboPortfolioHoldingList);
    }
}
