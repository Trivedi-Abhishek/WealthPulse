package com.alertservice.kafka.producer;

import com.alertservice.dal.dto.AlertTriggeredEvent;
import com.alertservice.dal.dto.PortfolioRebalanceTriggeredEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishAlertTriggered(AlertTriggeredEvent event) {
        String key = Objects.nonNull(event.portfolioId()) ? String.valueOf(event.portfolioId()) : event.symbol();
        try {
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send("alert.triggered", key, objectMapper.writeValueAsString(event));
            future.whenComplete((res, exc) -> {
                if (Objects.isNull(exc)) {
                    RecordMetadata recordMetadata = res.getRecordMetadata();
                    log.info(
                            "Alert {} published successfully. topic={}, partition={}, offset={}",
                            event.alertConfigurationId(),
                            recordMetadata.topic(),
                            recordMetadata.partition(),
                            recordMetadata.offset()
                    );
                } else {
                    //Todo: For now, logging is acceptable, but in your design document mention that this will be replaced with an Outbox Publisher.
                    log.error("Exception occurred: ", exc);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize alert {}", event.alertConfigurationId(), e);
        }
    }

    public void publishPortfolioRebalanceTriggered(PortfolioRebalanceTriggeredEvent event) {
        try {
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send("portfolio.rebalance.triggered", String.valueOf(event.portfolioId()), objectMapper.writeValueAsString(event));
            future.whenComplete((res, exc) -> {
                if (Objects.isNull(exc)) {
                    RecordMetadata recordMetadata = res.getRecordMetadata();
                    log.info(
                            "Rebalance trigger for portfolio {} published successfully. topic={}, partition={}, offset={}",
                            event.portfolioId(),
                            recordMetadata.topic(),
                            recordMetadata.partition(),
                            recordMetadata.offset()
                    );
                } else {
                    log.error("Exception occurred: ", exc);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize rebalance trigger for portfolio {}", event.portfolioId(), e);
        }
    }
}
