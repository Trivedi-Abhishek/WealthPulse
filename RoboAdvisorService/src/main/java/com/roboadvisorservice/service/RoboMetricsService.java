package com.roboadvisorservice.service;

import com.roboadvisorservice.dal.dto.PortfolioMetricsEvent;
import com.roboadvisorservice.dal.entity.RoboPortfolioProfile;
import com.roboadvisorservice.dal.repository.RoboPortfolioProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoboMetricsService {

    private final RoboPortfolioProfileRepository roboPortfolioProfileRepository;

    public void processPortfolioMetrics(PortfolioMetricsEvent portfolioMetricsEvent) {

        Optional<RoboPortfolioProfile> optionalRoboPortfolioProfile = roboPortfolioProfileRepository.findByPortfolioId(portfolioMetricsEvent.portfolioId());

        optionalRoboPortfolioProfile.ifPresentOrElse(existingProfile -> {
            existingProfile.setPnl(portfolioMetricsEvent.pnl());
            existingProfile.setCurrentValue(portfolioMetricsEvent.currentValue());
            existingProfile.setInvestedAmount(portfolioMetricsEvent.investedAmount());
            roboPortfolioProfileRepository.save(existingProfile);
        }, () -> log.warn("No RoboPortfolioProfile yet for portfolio {}, skipping metrics update", portfolioMetricsEvent.portfolioId()));
    }
}
