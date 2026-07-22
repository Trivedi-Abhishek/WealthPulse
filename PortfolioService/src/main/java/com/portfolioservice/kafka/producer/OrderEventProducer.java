package com.portfolioservice.kafka.producer;

import com.portfolioservice.dal.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Order> kafkaTemplate;

    public void publishOrderExecutedEvent(Order order) {

        kafkaTemplate.send("portfolio.order.executed", String.valueOf(order.getId()), order).whenComplete((res, exc)->{
            if(Objects.isNull(exc)) {
                RecordMetadata recordMetadata = res.getRecordMetadata();
                log.info(
                        "Order {} published successfully. topic={}, partition={}, offset={}",
                        order.getId(),
                        recordMetadata.topic(),
                        recordMetadata.partition(),
                        recordMetadata.offset()
                );
            }
            else {
                //Todo: For now, logging is acceptable, but in your design document mention that this will be replaced with an Outbox Publisher.
                log.error("Exception occurred: ", exc);
            }
        });

    }
}
