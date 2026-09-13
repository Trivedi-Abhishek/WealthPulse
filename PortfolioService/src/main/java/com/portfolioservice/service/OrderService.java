package com.portfolioservice.service;

import com.portfolioservice.dal.dto.CreateOrderRequest;
import com.portfolioservice.dal.dto.HoldingUpdatedEvent;
import com.portfolioservice.dal.dto.OrderExecutedEvent;
import com.portfolioservice.dal.entity.Holdings;
import com.portfolioservice.dal.entity.Investor;
import com.portfolioservice.dal.entity.Order;
import com.portfolioservice.dal.entity.Portfolio;
import com.portfolioservice.dal.repository.HoldingsRepository;
import com.portfolioservice.dal.repository.InvestorRepository;
import com.portfolioservice.dal.repository.OrderRepository;
import com.portfolioservice.dal.repository.PortfolioRepository;
import com.portfolioservice.enums.OrderStatusEnum;
import com.portfolioservice.enums.OrderTypeEnum;
import com.portfolioservice.enums.RiskProfileEnum;
import com.portfolioservice.enums.StatusEnum;
import com.portfolioservice.exception.HoldingNotFoundException;
import com.portfolioservice.exception.InsufficientHoldingException;
import com.portfolioservice.exception.InvestorNotFoundException;
import com.portfolioservice.exception.PortfolioNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    // Matches the NUMERIC(19, 4) precision of holdings.average_price. An unscaled divide()
    // throws ArithmeticException whenever the result is a non-terminating decimal.
    private static final int PRICE_SCALE = 4;

    private final OrderRepository orderRepository;
    private final PortfolioRepository portfolioRepository;
    private final InvestorRepository investorRepository;
    private final HoldingsRepository holdingsRepository;
    // Events are handed to Spring rather than to the producer directly; OrderEventProducer
    // listens AFTER_COMMIT so nothing reaches Kafka for a transaction that later rolls back.
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public Long buyOrder(CreateOrderRequest createOrderRequest) {

        Long portfolioId = createOrderRequest.getPortfolioId();
        String symbol = createOrderRequest.getSymbol();
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
        RiskProfileEnum riskProfile = resolveRiskProfile(portfolio);

        Order order = createOrderEntity(createOrderRequest, portfolioId, symbol, OrderTypeEnum.BUY);

        order=orderRepository.save(order);

        // Looked up without the status filter: holdings has a UNIQUE (portfolio_id, symbol)
        // constraint, so a symbol sold down to zero leaves an inactive row that a re-buy must
        // reactivate rather than insert alongside.
        Holdings holdings= holdingsRepository.findByPortfolioIdAndSymbol(portfolioId, symbol).map(h -> {

            if (StatusEnum.I.equals(h.getStatus())) {
                // Position was fully sold previously; this buy re-opens it with a fresh cost basis.
                h.setStatus(StatusEnum.A);
                h.setQuantity(createOrderRequest.getQuantity());
                h.setAveragePrice(createOrderRequest.getPrice());
                return h;
            }

            Long newQuantity = h.getQuantity() + createOrderRequest.getQuantity();
            BigDecimal newAveragePrice= ((h.getAveragePrice().multiply(BigDecimal.valueOf(h.getQuantity())))
                    .add(createOrderRequest.getPrice().multiply(BigDecimal.valueOf(createOrderRequest.getQuantity()))))
                    .divide(BigDecimal.valueOf(newQuantity), PRICE_SCALE, RoundingMode.HALF_UP);

            h.setAveragePrice(newAveragePrice);
            h.setQuantity(newQuantity);

            return h;
        }).orElseGet(() -> createHoldings(createOrderRequest));

        holdingsRepository.save(holdings);

        applicationEventPublisher.publishEvent(new OrderExecutedEvent(order.getId(), portfolioId, symbol, OrderTypeEnum.BUY, createOrderRequest.getPrice(), createOrderRequest.getQuantity()));
        applicationEventPublisher.publishEvent(new HoldingUpdatedEvent(portfolioId, symbol, holdings.getQuantity(), holdings.getAveragePrice(), riskProfile, Instant.now()));
        return order.getId();
    }

    private RiskProfileEnum resolveRiskProfile(Portfolio portfolio) {
        Investor investor = investorRepository.findById(portfolio.getInvestorId())
                .orElseThrow(() -> new InvestorNotFoundException(portfolio.getInvestorId()));
        return investor.getRiskProfile();
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

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
        RiskProfileEnum riskProfile = resolveRiskProfile(portfolio);

        Holdings holdings = holdingsRepository
                .findByPortfolioIdAndSymbolAndStatus(
                        portfolioId,
                        symbol,
                        StatusEnum.A
                )
                .orElseThrow(() -> new HoldingNotFoundException(portfolioId, symbol));

        if (holdings.getQuantity() < request.getQuantity()) {
            throw new InsufficientHoldingException(portfolioId, symbol, holdings.getQuantity(), request.getQuantity());
        }

        Order order = createOrderEntity(
                request,
                portfolioId,
                symbol,
                OrderTypeEnum.SELL
        );


        order = orderRepository.save(order);

        Long remainingQty =
                holdings.getQuantity() - request.getQuantity();

        holdings.setQuantity(remainingQty);

        if (remainingQty == 0) {
            holdings.setStatus(StatusEnum.I);
        }

        holdingsRepository.save(holdings);

        applicationEventPublisher.publishEvent(new OrderExecutedEvent(order.getId(), portfolioId, symbol, OrderTypeEnum.SELL, request.getPrice(), request.getQuantity()));
        applicationEventPublisher.publishEvent(new HoldingUpdatedEvent(portfolioId, symbol, holdings.getQuantity(), holdings.getAveragePrice(), riskProfile, Instant.now()));
        return order.getId();
    }

    public List<Order> getOrders(Long portfolioId) {
        if (!portfolioRepository.existsById(portfolioId)) {
            throw new PortfolioNotFoundException(portfolioId);
        }
        return orderRepository.findByPortfolioIdOrderByCreatedDateDesc(portfolioId);
    }
}
