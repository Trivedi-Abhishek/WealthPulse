package com.notificationservice.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationservice.dal.dto.AlertTriggeredEvent;
import com.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class AlertTriggeredEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "alert.triggered", groupId = "wealth-plus-service-group")
    public void consumeAlertTriggeredEvent(String message) {
        try {
            AlertTriggeredEvent alertTriggeredEvent = objectMapper.readValue(message, AlertTriggeredEvent.class);
            notificationService.recordNotification(alertTriggeredEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to process alert triggered event: {}", message, e);
        }
    }
}
