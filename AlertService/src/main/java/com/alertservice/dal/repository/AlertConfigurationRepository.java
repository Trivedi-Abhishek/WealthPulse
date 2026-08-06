package com.alertservice.dal.repository;

import com.alertservice.dal.entity.AlertConfiguration;
import com.alertservice.dal.enums.AlertTypeEnum;
import com.alertservice.dal.enums.StatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertConfigurationRepository extends JpaRepository<AlertConfiguration, Long> {
    List<AlertConfiguration> findBySymbolAndAlertTypeAndStatus(String symbol, AlertTypeEnum alertType, StatusEnum status);
    List<AlertConfiguration> findByPortfolioIdAndAlertTypeAndStatus(Long portfolioId, AlertTypeEnum alertType, StatusEnum status);
}
