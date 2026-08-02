package com.pnlconsumerservice.kafka.producer;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pnlconsumerservice.dal.dto.PortfolioMetricsEvent;
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
public class PnlDataProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishPortfolioMetrics(PortfolioMetricsEvent portfolioMetricsEvent)  {
        try {
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send("portfolio.metrics.updated", String.valueOf(portfolioMetricsEvent.portfolioId()), objectMapper.writeValueAsString(portfolioMetricsEvent));
            future.whenComplete((res, exc)->{
                if(Objects.isNull(exc)) {
                    RecordMetadata recordMetadata = res.getRecordMetadata();
                    log.info(recordMetadata.topic());
                    log.info(String.valueOf(recordMetadata.partition()));
                    log.info(String.valueOf(recordMetadata.offset()));
                }
                else {
                    log.error("Exception occurred: ", exc);
                }
            });
        } catch (JsonProcessingException e) {
            // not throwing error else successive calls will not be called
            log.error("Unable to serialize PortfolioMetricsEvent {}", portfolioMetricsEvent.portfolioId(), e);
        }
    }

}
