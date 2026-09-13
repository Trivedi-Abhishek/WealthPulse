package com.roboadvisorservice.service;

import com.roboadvisorservice.dal.dto.HoldingUpdatedEvent;
import com.roboadvisorservice.dal.entity.RoboPortfolioHolding;
import com.roboadvisorservice.dal.entity.RoboPortfolioProfile;
import com.roboadvisorservice.dal.enums.StatusEnum;
import com.roboadvisorservice.dal.repository.RoboPortfolioHoldingRepository;
import com.roboadvisorservice.dal.repository.RoboPortfolioProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoboPortfolioHoldingService {

    private final RoboPortfolioHoldingRepository roboPortfolioHoldingRepository;
    private final RoboPortfolioProfileRepository roboPortfolioProfileRepository;

    public void updateHolding(HoldingUpdatedEvent holdingUpdatedEvent) {

        // Looked up without the status filter. The unique constraint is on (portfolio_id, symbol)
        // alone, so a symbol sold to zero leaves an inactive row behind; an A-filtered lookup
        // would miss it on a re-buy and insert a duplicate, failing on the constraint and
        // wedging this listener on that partition.
        Optional<RoboPortfolioHolding> optionalRoboPortfolioHoldings = roboPortfolioHoldingRepository.findByPortfolioIdAndSymbol(holdingUpdatedEvent.portfolioId(), holdingUpdatedEvent.symbol());

        if(holdingUpdatedEvent.quantity()==0L) {

            optionalRoboPortfolioHoldings.ifPresent(holdings->{
                holdings.setStatus(StatusEnum.I);
                holdings.setQuantity(0L);
                roboPortfolioHoldingRepository.save(holdings);
            });
            return;
        }

        RoboPortfolioHolding roboPortfolioHolding=optionalRoboPortfolioHoldings.orElseGet(() ->
                RoboPortfolioHolding.builder()
                        .portfolioId(holdingUpdatedEvent.portfolioId())
                        .symbol(holdingUpdatedEvent.symbol())
                        .latestMarketPrice(BigDecimal.ZERO)
                        .build());

        // Set explicitly rather than only on create, so a re-buy reactivates the existing row.
        roboPortfolioHolding.setStatus(StatusEnum.A);
        roboPortfolioHolding.setQuantity(holdingUpdatedEvent.quantity());
        roboPortfolioHolding.setAveragePrice(holdingUpdatedEvent.averagePrice());

        RoboPortfolioProfile profile = roboPortfolioProfileRepository.findByPortfolioId(holdingUpdatedEvent.portfolioId())
                .orElseGet(() -> RoboPortfolioProfile.builder()
                        .portfolioId(holdingUpdatedEvent.portfolioId())
                        .currentValue(BigDecimal.ZERO)
                        .investedAmount(BigDecimal.ZERO)
                        .status(StatusEnum.A)
                        .build());

        profile.setRiskProfile(holdingUpdatedEvent.riskProfile());
        roboPortfolioProfileRepository.save(profile);
        roboPortfolioHoldingRepository.save(roboPortfolioHolding);
    }
}
