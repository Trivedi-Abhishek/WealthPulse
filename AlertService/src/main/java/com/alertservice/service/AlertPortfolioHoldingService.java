package com.alertservice.service;

import com.alertservice.dal.AlertPortfolioHoldingsRepository;
import com.alertservice.dal.dto.HoldingUpdatedEvent;
import com.alertservice.dal.entity.AlertPortfolioHoldings;
import com.alertservice.dal.enums.StatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AlertPortfolioHoldingService {

    private final AlertPortfolioHoldingsRepository alertPortfolioHoldingsRepository;

    public void updateHolding(HoldingUpdatedEvent marketStockPriceEvent) {

        Optional<AlertPortfolioHoldings> optionalAlertPortfolioHoldings=alertPortfolioHoldingsRepository.findByPortfolioIdAndSymbolAndStatus(marketStockPriceEvent.portfolioId(), marketStockPriceEvent.symbol(), StatusEnum.A);

        if (marketStockPriceEvent.quantity() == 0L) {

            optionalAlertPortfolioHoldings.ifPresent(holdings->{
                holdings.setStatus(StatusEnum.I);
                holdings.setQuantity(0L);
                alertPortfolioHoldingsRepository.save(holdings);
            });
            return;

        }

        AlertPortfolioHoldings holding =
                optionalAlertPortfolioHoldings.orElse(
                        AlertPortfolioHoldings.builder()
                                .portfolioId(marketStockPriceEvent.portfolioId())
                                .symbol(marketStockPriceEvent.symbol())
                                .status(StatusEnum.A)
                                .build());

        holding.setQuantity(marketStockPriceEvent.quantity());
        holding.setAveragePrice(marketStockPriceEvent.averagePrice());

        alertPortfolioHoldingsRepository.save(holding);

    }
}
