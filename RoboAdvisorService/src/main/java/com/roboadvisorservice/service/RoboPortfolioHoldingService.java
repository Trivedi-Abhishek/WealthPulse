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

        Optional<RoboPortfolioHolding> optionalRoboPortfolioHoldings = roboPortfolioHoldingRepository.findByPortfolioIdAndSymbolAndStatus(holdingUpdatedEvent.portfolioId(), holdingUpdatedEvent.symbol(), StatusEnum.A);

        if(holdingUpdatedEvent.quantity()==0L) {

            optionalRoboPortfolioHoldings.ifPresent(holdings->{
                holdings.setStatus(StatusEnum.I);
                holdings.setQuantity(0L);
                roboPortfolioHoldingRepository.save(holdings);
            });
            return;
        }

        RoboPortfolioHolding roboPortfolioHolding=optionalRoboPortfolioHoldings.orElse(
                RoboPortfolioHolding.builder()
                        .portfolioId(holdingUpdatedEvent.portfolioId())
                        .symbol(holdingUpdatedEvent.symbol())
                        .latestMarketPrice(BigDecimal.ZERO)
                        .status(StatusEnum.A)
                        .build());

        roboPortfolioHolding.setQuantity(holdingUpdatedEvent.quantity());
        roboPortfolioHolding.setAveragePrice(holdingUpdatedEvent.averagePrice());

        RoboPortfolioProfile profile = roboPortfolioProfileRepository.findByPortfolioId(holdingUpdatedEvent.portfolioId())
                .orElse(RoboPortfolioProfile.builder()
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
