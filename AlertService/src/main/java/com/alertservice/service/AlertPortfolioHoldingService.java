package com.alertservice.service;

import com.alertservice.dal.repository.AlertPortfolioHoldingsRepository;
import com.alertservice.dal.dto.HoldingUpdatedEvent;
import com.alertservice.dal.entity.AlertPortfolioHoldings;
import com.alertservice.dal.enums.StatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AlertPortfolioHoldingService {

    private final AlertPortfolioHoldingsRepository alertPortfolioHoldingsRepository;

    public void updateHolding(HoldingUpdatedEvent holdingUpdatedEvent) {

        Optional<AlertPortfolioHoldings> optionalAlertPortfolioHoldings=alertPortfolioHoldingsRepository.findByPortfolioIdAndSymbolAndStatus(holdingUpdatedEvent.portfolioId(), holdingUpdatedEvent.symbol(), StatusEnum.A);

        if (holdingUpdatedEvent.quantity() == 0L) {

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
                                .portfolioId(holdingUpdatedEvent.portfolioId())
                                .symbol(holdingUpdatedEvent.symbol())
                                .latestMarketPrice(BigDecimal.ZERO)
                                .status(StatusEnum.A)
                                .build());

        holding.setQuantity(holdingUpdatedEvent.quantity());
        holding.setAveragePrice(holdingUpdatedEvent.averagePrice());

        alertPortfolioHoldingsRepository.save(holding);

    }
}
