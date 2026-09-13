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

        // Looked up without the status filter. The unique constraint is on (portfolio_id, symbol)
        // alone, so a symbol sold to zero leaves an inactive row behind; an A-filtered lookup
        // would miss it on a re-buy and insert a duplicate, failing on the constraint and
        // wedging this listener on that partition.
        Optional<AlertPortfolioHoldings> optionalAlertPortfolioHoldings=alertPortfolioHoldingsRepository.findByPortfolioIdAndSymbol(holdingUpdatedEvent.portfolioId(), holdingUpdatedEvent.symbol());

        if (holdingUpdatedEvent.quantity() == 0L) {

            optionalAlertPortfolioHoldings.ifPresent(holdings->{
                holdings.setStatus(StatusEnum.I);
                holdings.setQuantity(0L);
                alertPortfolioHoldingsRepository.save(holdings);
            });
            return;

        }

        AlertPortfolioHoldings holding =
                optionalAlertPortfolioHoldings.orElseGet(() ->
                        AlertPortfolioHoldings.builder()
                                .portfolioId(holdingUpdatedEvent.portfolioId())
                                .symbol(holdingUpdatedEvent.symbol())
                                .latestMarketPrice(BigDecimal.ZERO)
                                .build());

        // Set explicitly rather than only on create, so a re-buy reactivates the existing row.
        holding.setStatus(StatusEnum.A);
        holding.setQuantity(holdingUpdatedEvent.quantity());
        holding.setAveragePrice(holdingUpdatedEvent.averagePrice());

        alertPortfolioHoldingsRepository.save(holding);

    }
}
