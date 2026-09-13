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
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Both publish methods fire only AFTER_COMMIT. KafkaTemplate.send hands the record to the
 * sender thread immediately, so publishing inline from OrderService's @Transactional method
 * would emit events for a transaction that can still fail at commit — an optimistic-lock
 * failure on the @Version row, or the UNIQUE (portfolio_id, symbol) constraint — leaving all
 * three downstream read models holding a holdings update this service rolled back.
 *
 * Note these are only reached from within a transaction; @TransactionalEventListener silently
 * drops events published outside one.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
