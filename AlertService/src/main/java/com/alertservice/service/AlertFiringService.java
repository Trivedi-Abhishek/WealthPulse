package com.alertservice.service;

import com.alertservice.dal.dto.AlertTriggeredEvent;
import com.alertservice.dal.dto.PortfolioRebalanceTriggeredEvent;
import com.alertservice.dal.entity.AlertConfiguration;
import com.alertservice.dal.entity.AlertHistory;
import com.alertservice.dal.enums.AlertTypeEnum;
import com.alertservice.dal.repository.AlertHistoryRepository;
import com.alertservice.kafka.producer.AlertEventProducer;
import com.alertservice.service.evaluator.AlertEvaluationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertFiringService {

    private static final Duration DEDUP_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, String> redisTemplate;
    private final AlertHistoryRepository alertHistoryRepository;
    private final AlertEventProducer alertEventProducer;

    public void fire(AlertConfiguration config, AlertEvaluationResult result) {
        String dedupKey = "alert:" + config.getId();
        if (!claimDedupWindow(dedupKey)) {
            log.debug("Alert {} suppressed by dedup window", config.getId());
            return;
        }

        Instant triggeredAt = Instant.now();

        alertHistoryRepository.save(AlertHistory.builder()
                .alertConfigurationId(config.getId())
                .portfolioId(config.getPortfolioId())
                .symbol(config.getSymbol())
                .alertType(config.getAlertType())
                .message(result.message())
                .triggeredValue(result.triggeredValue())
                .triggeredAt(triggeredAt)
                .build());

        alertEventProducer.publishAlertTriggered(new AlertTriggeredEvent(
                config.getId(), config.getPortfolioId(), config.getSymbol(), config.getAlertType(),
                result.message(), result.triggeredValue(), triggeredAt));

        if (config.getAlertType() == AlertTypeEnum.PORTFOLIO_DRIFT) {
            alertEventProducer.publishPortfolioRebalanceTriggered(
                    new PortfolioRebalanceTriggeredEvent(config.getPortfolioId(), result.message(), triggeredAt));
        }

        log.info("Alert fired: configId={}, type={}, message={}", config.getId(), config.getAlertType(), result.message());
    }

    /**
     * Claims the dedup window for this alert config, returning false if another evaluation
     * already holds it.
     *
     * <p>The key is released if the surrounding transaction rolls back. Redis is not
     * transactional, so a key written before a failed save would outlive the alert_history row
     * that never existed and silently suppress the alert for the next 30 minutes.
     */
    private boolean claimDedupWindow(String dedupKey) {
        Boolean firstSeen = redisTemplate.opsForValue().setIfAbsent(dedupKey, Instant.now().toString(), DEDUP_TTL);
        if (!Boolean.TRUE.equals(firstSeen)) {
            return false;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        redisTemplate.delete(dedupKey);
                        log.warn("Released dedup key {} after transaction did not commit", dedupKey);
                    }
                }
            });
        }
        return true;
    }
}
