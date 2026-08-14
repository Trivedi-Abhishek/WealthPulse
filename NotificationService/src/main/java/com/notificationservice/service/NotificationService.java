package com.notificationservice.service;

import com.notificationservice.dal.dto.AlertTriggeredEvent;
import com.notificationservice.dal.entity.NotificationHistory;
import com.notificationservice.dal.enums.StatusEnum;
import com.notificationservice.dal.repository.NotificationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationHistoryRepository notificationHistoryRepository;

    public void recordNotification(AlertTriggeredEvent event) {
        NotificationHistory history = NotificationHistory.builder()
                .alertConfigurationId(event.alertConfigurationId())
                .portfolioId(event.portfolioId())
                .symbol(event.symbol())
                .alertType(event.alertType())
                .message(event.message())
                .triggeredValue(event.triggeredValue())
                .status(StatusEnum.A)
                .triggeredAt(event.triggeredAt())
                .receivedAt(Instant.now())
                .build();

        notificationHistoryRepository.save(history);

        // TODO: replace with a real delivery channel (email/SMS/push) once investor contact
        log.info("Notification recorded for portfolio {}: {}", event.portfolioId(), event.message());
    }

    public List<NotificationHistory> getNotifications(Long portfolioId) {
        return notificationHistoryRepository.findByPortfolioIdOrderByTriggeredAtDesc(portfolioId);
    }
}
