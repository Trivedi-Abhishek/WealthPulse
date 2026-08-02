package com.portfolioservice.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolioservice.dal.dto.HoldingUpdatedEvent;
import com.portfolioservice.dal.dto.OrderExecutedEvent;
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
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishOrderExecutedEvent(OrderExecutedEvent orderExecutedEvent) {
        try {
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send("portfolio.order.executed", String.valueOf(orderExecutedEvent.portfolioId()), objectMapper.writeValueAsString(orderExecutedEvent));
            future.whenComplete((res, exc) -> {
                if (Objects.isNull(exc)) {
                    RecordMetadata recordMetadata = res.getRecordMetadata();
                    log.info(
                            "Order {} published successfully. topic={}, partition={}, offset={}",
                            orderExecutedEvent.orderId(),
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
            log.error("Unable to serialize order {}", orderExecutedEvent.orderId(), e);
        }
    }

    public void publishHoldingUpdatedEvent(HoldingUpdatedEvent holdingUpdatedEvent) {
        try {
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send("portfolio.holdings.updated", String.valueOf(holdingUpdatedEvent.portfolioId()), objectMapper.writeValueAsString(holdingUpdatedEvent));
            future.whenComplete((res, exc) -> {
                if (Objects.isNull(exc)) {
                    RecordMetadata recordMetadata = res.getRecordMetadata();
                    log.info(
                            "Portfolio {} published successfully. topic={}, partition={}, offset={}",
                            holdingUpdatedEvent.portfolioId(),
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
            log.error("Unable to serialize portfolio {}", holdingUpdatedEvent.portfolioId(), e);
        }
    }
}
