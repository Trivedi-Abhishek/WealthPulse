package com.pnlconsumerservice.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pnlconsumerservice.dal.dto.HoldingUpdatedEvent;
import com.pnlconsumerservice.dal.dto.MarketStockPriceEvent;
import com.pnlconsumerservice.dal.dto.PortfolioMetricsEvent;
import com.pnlconsumerservice.dal.entity.PortfolioHoldingsSnapshot;
import com.pnlconsumerservice.dal.entity.PortfolioMetrics;
import com.pnlconsumerservice.dal.enums.StatusEnum;
import com.pnlconsumerservice.dal.repository.PortfolioHoldingsSnapshotRepository;
import com.pnlconsumerservice.dal.repository.PortfolioMetricsRepository;
import com.pnlconsumerservice.kafka.producer.PnlDataProducer;
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
    private final PnlDataProducer pnlDataProducer;

    @KafkaListener(topics = "market.price.updated")
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


    @KafkaListener(topics = "portfolio.holdings.updated")
    public void listenPortfolioHoldingsUpdatedEvent(String srcHoldingUpdatedEvent) {

        try {
            HoldingUpdatedEvent holdingUpdatedEvent = objectMapper.readValue(srcHoldingUpdatedEvent, HoldingUpdatedEvent.class);
            Optional<PortfolioHoldingsSnapshot> optionalSnapshot=portfolioHoldingsSnapshotRepository.findByPortfolioIdAndSymbol(holdingUpdatedEvent.portfolioId(), holdingUpdatedEvent.symbol());

            if(holdingUpdatedEvent.quantity()==0L){
                // Position fully sold. The snapshot must be deactivated and the metrics
                // recalculated — leaving it active keeps pricing shares the investor no
                // longer owns on every subsequent market.price.updated for this symbol.
                optionalSnapshot.ifPresent(snapshot -> {
                    snapshot.setStatus(StatusEnum.I);
                    snapshot.setQuantity(0L);
                    portfolioHoldingsSnapshotRepository.save(snapshot);
                });
                recalculatePortfolioMetrics(holdingUpdatedEvent.portfolioId());
                return;
            }

            PortfolioHoldingsSnapshot snapshot =
                    optionalSnapshot.orElseGet(() ->
                            PortfolioHoldingsSnapshot.builder()
                                    .portfolioId(holdingUpdatedEvent.portfolioId())
                                    .symbol(holdingUpdatedEvent.symbol())
                                    .latestMarketPrice(BigDecimal.ZERO)
                                    .build());

            // Set explicitly rather than only on create: a re-buy after a full sell has to
            // reactivate the existing row, since UNIQUE (portfolio_id, symbol) forbids a second one.
            snapshot.setStatus(StatusEnum.A);
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

        BigDecimal currentValue=BigDecimal.ZERO;
        BigDecimal investedAmount=BigDecimal.ZERO;

        // An empty list is a real state, not a no-op: the investor sold everything, and the
        // metrics row has to fall to zero rather than keep its last non-empty value.
        if(!CollectionUtils.isEmpty(portfolioHoldingsSnapshotList)) {
            for(PortfolioHoldingsSnapshot snapshot:portfolioHoldingsSnapshotList) {
                currentValue=currentValue.add(snapshot.getLatestMarketPrice().multiply(BigDecimal.valueOf(snapshot.getQuantity())));
                investedAmount=investedAmount.add(snapshot.getAveragePrice().multiply(BigDecimal.valueOf(snapshot.getQuantity())));
            }
        }

        Optional<PortfolioMetrics> optionalPortfolioMetrics=portfolioMetricsRepository.findByPortfolioId(portfolioId);

        PortfolioMetrics portfolioMetrics=optionalPortfolioMetrics.orElseGet(() -> PortfolioMetrics.builder().
                portfolioId(portfolioId).xirr(BigDecimal.ZERO).build());

        portfolioMetrics.setInvestedAmount(investedAmount);
        portfolioMetrics.setCurrentValue(currentValue);
        portfolioMetrics.setPnl(currentValue.subtract(investedAmount));

        // Without this save the orElseGet branch builds a transient entity that is never
        // persisted, so the lookup above misses on every subsequent event and portfolio_metrics
        // stays permanently empty — the dirty-checking path is never reached.
        portfolioMetrics = portfolioMetricsRepository.save(portfolioMetrics);

        PortfolioMetricsEvent portfolioMetricsEvent = new PortfolioMetricsEvent(portfolioMetrics.getPortfolioId(), portfolioMetrics.getCurrentValue(), portfolioMetrics.getInvestedAmount(), portfolioMetrics.getPnl());
        pnlDataProducer.publishPortfolioMetrics(portfolioMetricsEvent);
    }

}
