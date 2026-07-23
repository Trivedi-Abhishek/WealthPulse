package com.portfolioservice.service;

import com.portfolioservice.dal.dto.CreateOrderRequest;
import com.portfolioservice.dal.dto.HoldingUpdatedEvent;
import com.portfolioservice.dal.entity.Holdings;
import com.portfolioservice.dal.entity.Order;
import com.portfolioservice.dal.repository.HoldingsRepository;
import com.portfolioservice.dal.repository.OrderRepository;
import com.portfolioservice.dal.repository.PortfolioRepository;
import com.portfolioservice.enums.OrderStatusEnum;
import com.portfolioservice.enums.OrderTypeEnum;
import com.portfolioservice.enums.StatusEnum;
import com.portfolioservice.kafka.producer.OrderEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PortfolioRepository portfolioRepository;
    private final HoldingsRepository holdingsRepository;
    private final OrderEventProducer orderEventProducer;

    @Transactional
    public Long buyOrder(CreateOrderRequest createOrderRequest) {

        Long portfolioId = createOrderRequest.getPortfolioId();
        String symbol = createOrderRequest.getSymbol();
        if(!portfolioRepository.existsById(portfolioId)) {
            // throw error
        }

        Order order = createOrderEntity(createOrderRequest, portfolioId, symbol, OrderTypeEnum.BUY);

        order=orderRepository.save(order);

        Holdings holdings= holdingsRepository.findByPortfolioIdAndSymbolAndStatus(portfolioId, symbol, StatusEnum.A).map(h -> {

            BigDecimal newAveragePrice= ((h.getAveragePrice().multiply(BigDecimal.valueOf(h.getQuantity())))
                    .add(createOrderRequest.getPrice().multiply(BigDecimal.valueOf(createOrderRequest.getQuantity())))).divide(BigDecimal.valueOf(h.getQuantity()+createOrderRequest.getQuantity()));

            h.setAveragePrice(newAveragePrice);
            h.setQuantity(h.getQuantity()+createOrderRequest.getQuantity());

            return h;
        }).orElse(createHoldings(createOrderRequest));

        holdingsRepository.save(holdings);
        HoldingUpdatedEvent holdingUpdatedEvent=new HoldingUpdatedEvent(portfolioId, symbol, holdings.getQuantity(), holdings.getAveragePrice(), Instant.now());
        orderEventProducer.publishHoldingUpdatedEvent(holdingUpdatedEvent);
        return order.getId();
    }

    private static Order createOrderEntity(CreateOrderRequest createOrderRequest, Long portfolioId, String symbol, OrderTypeEnum orderType) {
        return Order.builder().portfolioId(portfolioId).symbol(symbol)
                .price(createOrderRequest.getPrice()).quantity(createOrderRequest.getQuantity())
                .status(StatusEnum.A).orderType(orderType).orderStatus(OrderStatusEnum.EXECUTED).build();
    }

    private Holdings createHoldings(CreateOrderRequest createOrderRequest) {

        return Holdings.builder().portfolioId(createOrderRequest.getPortfolioId()).symbol(createOrderRequest.getSymbol())
                .averagePrice(createOrderRequest.getPrice()).quantity(createOrderRequest.getQuantity())
                .status(StatusEnum.A).build();
    }

    @Transactional
    public Long sellOrder(CreateOrderRequest request) {

        Long portfolioId = request.getPortfolioId();

        String symbol = request.getSymbol();

        if (!portfolioRepository.existsById(portfolioId)) {
//            throw new PortfolioNotFoundException();
        }


        Optional<Holdings> optionalHoldings = holdingsRepository
                .findByPortfolioIdAndSymbolAndStatus(
                        portfolioId,
                        symbol,
                        StatusEnum.A
                );

        if(optionalHoldings.isEmpty()) {
//            throw new HoldingNotFoundException();
        }
        Holdings holdings = optionalHoldings.get();
        if (holdings.getQuantity() < request.getQuantity()) {
//            throw new InsufficientHoldingException();
        }

        Order order = createOrderEntity(
                request,
                portfolioId,
                symbol,
                OrderTypeEnum.SELL
        );


        orderRepository.save(order);

        Long remainingQty =
                holdings.getQuantity() - request.getQuantity();

        holdings.setQuantity(remainingQty);

        if (remainingQty == 0) {
            holdings.setStatus(StatusEnum.I);
        }

        holdingsRepository.save(holdings);
        HoldingUpdatedEvent holdingUpdatedEvent=new HoldingUpdatedEvent(portfolioId, symbol, holdings.getQuantity(), holdings.getAveragePrice(), Instant.now());
        orderEventProducer.publishHoldingUpdatedEvent(holdingUpdatedEvent);
        return order.getId();
    }
}
