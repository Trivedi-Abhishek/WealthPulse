package com.pnlconsumerservice.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pnlconsumerservice.dal.dto.HoldingUpdatedEvent;
import com.pnlconsumerservice.dal.dto.MarketStockPriceEvent;
import com.pnlconsumerservice.dal.entity.PortfolioHoldingsSnapshot;
import com.pnlconsumerservice.dal.entity.PortfolioMetrics;
import com.pnlconsumerservice.dal.enums.StatusEnum;
import com.pnlconsumerservice.dal.repository.PortfolioHoldingsSnapshotRepository;
import com.pnlconsumerservice.dal.repository.PortfolioMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

@RequiredArgsConstructor
@Slf4j
@Transactional
@Service
public class PnlDataConsumer {

    private final ObjectMapper objectMapper;
    private final PortfolioHoldingsSnapshotRepository portfolioHoldingsSnapshotRepository;
    private final PortfolioMetricsRepository portfolioMetricsRepository;

    @KafkaListener(topics = "market.price.updated", groupId = "wealth-plus-service-group")
    public void listenMarketStockPriceEvent(String srcMarketStockPriceEvent) {

        try {
            MarketStockPriceEvent marketStockPriceEvent = objectMapper.readValue(srcMarketStockPriceEvent, MarketStockPriceEvent.class);
            List<PortfolioHoldingsSnapshot> snapshotList = portfolioHoldingsSnapshotRepository.findBySymbolAndStatus(marketStockPriceEvent.symbol(), StatusEnum.A);

            for(PortfolioHoldingsSnapshot portfolioHoldingsSnapshot: snapshotList) {
                portfolioHoldingsSnapshot.setLatestMarketPrice(marketStockPriceEvent.price());
                portfolioHoldingsSnapshotRepository.save(portfolioHoldingsSnapshot);
                recalculatePortfolioMetrics(portfolioHoldingsSnapshot.getPortfolioId());
            }

        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize MarketStockPriceEvent {} ", srcMarketStockPriceEvent, e);
        }
    }


    @KafkaListener(topics = "portfolio.order.executed", groupId = "wealth-plus-service-group")
    public void listenPortfolioOrderEvent(String srcHoldingUpdatedEvent) {

        try {
            HoldingUpdatedEvent holdingUpdatedEvent = objectMapper.readValue(srcHoldingUpdatedEvent, HoldingUpdatedEvent.class);

            Optional<PortfolioHoldingsSnapshot> optionalSnapshot=portfolioHoldingsSnapshotRepository.findByPortfolioIdAndSymbol(holdingUpdatedEvent.portfolioId(), holdingUpdatedEvent.symbol());
            if(holdingUpdatedEvent.quantity()==0L){
                return;
            }
            PortfolioHoldingsSnapshot snapshot =
                    optionalSnapshot.orElse(
                            PortfolioHoldingsSnapshot.builder()
                                    .portfolioId(holdingUpdatedEvent.portfolioId())
                                    .symbol(holdingUpdatedEvent.symbol())
                                    .latestMarketPrice(BigDecimal.ZERO)
                                    .status(StatusEnum.A)
                                    .build());

            snapshot.setQuantity(holdingUpdatedEvent.quantity());
            snapshot.setAveragePrice(holdingUpdatedEvent.averagePrice());

            portfolioHoldingsSnapshotRepository.save(snapshot);

            recalculatePortfolioMetrics(holdingUpdatedEvent.portfolioId());
        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize holdingUpdatedEvent {} ", srcHoldingUpdatedEvent, e);
        }
    }

    private void recalculatePortfolioMetrics(Long portfolioId) {
        // collect snapshot list by portfolio id
        List<PortfolioHoldingsSnapshot> portfolioHoldingsSnapshotList=portfolioHoldingsSnapshotRepository.findByPortfolioIdAndStatus(portfolioId, StatusEnum.A);

        if(CollectionUtils.isEmpty(portfolioHoldingsSnapshotList)) {
            return;
        }

        BigDecimal currentValue=BigDecimal.ZERO;
        BigDecimal investedAmount=BigDecimal.ZERO;

        for(PortfolioHoldingsSnapshot snapshot:portfolioHoldingsSnapshotList) {
            currentValue=currentValue.add(snapshot.getLatestMarketPrice().multiply(BigDecimal.valueOf(snapshot.getQuantity())));
            investedAmount=investedAmount.add(snapshot.getAveragePrice().multiply(BigDecimal.valueOf(snapshot.getQuantity())));
        }

        Optional<PortfolioMetrics> optionalPortfolioMetrics=portfolioMetricsRepository.findByPortfolioId(portfolioId);

        PortfolioMetrics portfolioMetrics=optionalPortfolioMetrics.orElse(PortfolioMetrics.builder().
                portfolioId(portfolioId).xirr(BigDecimal.ZERO).build());

        portfolioMetrics.setInvestedAmount(investedAmount);
        portfolioMetrics.setCurrentValue(currentValue);
        portfolioMetrics.setPnl(currentValue.subtract(investedAmount));


    }

}
