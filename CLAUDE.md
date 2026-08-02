# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository structure

WealthPulse is a set of **independent Spring Boot microservices**, each with its own `pom.xml`/`mvnw` and living in its own top-level directory:

- `MarketDataService` — polls Alpha Vantage for stock prices and publishes price events.
- `PortfolioService` — REST API for portfolios and buy/sell orders; owns holdings state.
- `PnlConsumerService` — consumes price/order events, maintains holdings snapshots, computes P&L metrics.
- `AlertService` — consumes price/holdings/metrics events and generates alerts.

The **root `pom.xml`, `src/`, and `mvnw`** are leftover scaffolding from the initial project generation (artifact `com:WealthPulse`, empty `WealthPulseApplicationTests`). They are not part of the running system and are not a parent/aggregator for the four services (each service's `pom.xml` uses `spring-boot-starter-parent` directly). Don't build or reason about the system from the root — always work inside the relevant service directory.

There is no API gateway, service registry, or shared library module — services only communicate via Kafka topics (and duplicate the DTO/record definitions they need locally, see below).

## Common commands

All commands are run **from inside the specific service directory** (e.g. `cd PortfolioService`), using that service's own Maven wrapper.

```bash
# Build
./mvnw clean install          # mvnw.cmd on native Windows shells

# Run tests
./mvnw test

# Run a single test class / method
./mvnw test -Dtest=SomeClassTests
./mvnw test -Dtest=SomeClassTests#someMethod

# Run the service locally
./mvnw spring-boot:run
```

Each service currently only has the Spring Boot–generated `*ApplicationTests` context-load smoke test — there is no other test coverage yet.

### Local infrastructure

```bash
docker compose up -d   # from repo root — starts Postgres (5432) and Kafka (9092)
```

- Postgres: `wealth_plus_db`, user/password `user`/`password`.
- Kafka: `apache/kafka` KRaft single-node broker, advertised at `localhost:9092`, auto-topic-creation enabled (topics are also declared explicitly as `NewTopic` beans per service, see below).
- **Redis is not in `docker-compose.yaml`** even though `MarketDataService` depends on `spring-boot-starter-data-redis` and autowires a `RedisTemplate` — a Redis instance must be provisioned separately for that service to start locally.
- No service sets `server.port`, so all four default to `8080`; running more than one at a time locally requires overriding the port on the command line (`--server.port=...`).

## Architecture: event flow across services

The system is event-driven over Kafka. There is no synchronous inter-service HTTP traffic; `PortfolioService` is the only service exposing a REST API (`/orders`, and a `PortfolioController`), consumed by external clients, not by other services.

Topics, producers, and consumers (topic → producer service → consumer service(s)):

```
market.price.updated        MarketDataService  → PnlConsumerService, AlertService
portfolio.order.executed    PortfolioService   → PnlConsumerService
portfolio.holdings.updated  PnlConsumerService → AlertService
portfolio.metrics.updated   PnlConsumerService → AlertService
```

Flow:
1. `MarketDataService.AlphaVantageService` polls Alpha Vantage (`@Scheduled(fixedDelay = 30000)`, wrapped in resilience4j circuit breaker/retry/rate limiter) for a fixed symbol list (`AAPL, MSFT, GOOG, AMZN, TSLA`), diffs against the last price cached in Redis (30s TTL), and publishes `MarketStockPriceEvent` to `market.price.updated`.
2. `PortfolioService.OrderService.buyOrder`/`sellOrder` (invoked via `OrderController`) recompute `Holdings` (weighted-average price, quantity) inside a `@Transactional` method and publish a `HoldingUpdatedEvent` to `portfolio.order.executed`.
3. `PnlConsumerService.PnlDataConsumer` listens to both `market.price.updated` and `portfolio.order.executed`, keeps `PortfolioHoldingsSnapshot` rows current, recalculates `PortfolioMetrics` (current value, invested amount, PnL) per portfolio, and republishes `HoldingUpdatedEvent`/`PortfolioMetricsEvent` to `portfolio.holdings.updated`/`portfolio.metrics.updated`.
4. `AlertService.AlertServiceConsumer` listens to all three downstream topics and drives `PriceAlertService`, `AlertPortfolioHoldingService`, and `PortfolioAlertService`.

Kafka records are keyed by portfolio ID (or symbol for market data) so that per-entity ordering is preserved within a partition; topics are created with 3 partitions / replication factor 1 via `NewTopic` `@Bean`s in each service's `config` package.

**DTO duplication**: event payload records (e.g. `MarketStockPriceEvent`, `HoldingUpdatedEvent`, `PortfolioMetricsEvent`) are hand-duplicated under `dal/dto` in every service that produces or consumes them — there is no shared events module. If you change an event's shape, you must update it in every service's copy (producer and all consumers) to keep wire compatibility, since most consumers deserialize with Jackson via `ObjectMapper.readValue` against a plain `String` Kafka value.

## Per-service package layout

Each service follows the same internal convention:

- `dal/dto` — Kafka event/API payload records (see duplication note above).
- `dal/entity` — JPA entities (`PortfolioService`, `PnlConsumerService`, `AlertService` only; `MarketDataService` is stateless aside from its Redis cache).
- `dal/repository` — Spring Data repositories.
- `dal/enums` or `enums/` — status/type enums (naming is inconsistent between services — check the actual package before adding a new enum).
- `kafka/producer`, `kafka/consumer` — Kafka integration; producers generally log success/failure via `whenComplete` on the `CompletableFuture` returned by `KafkaTemplate.send` rather than blocking.
- `config` — `@Configuration` classes declaring `NewTopic` beans, `ProducerFactory`/`RedisTemplate`/`WebClient` beans, etc.
- `service` — business logic.
- `controller` — REST endpoints (`PortfolioService` only).

All Kafka consumers across services currently share the same `group-id: wealth-plus-service-group` (set in each service's `application.yaml`) — this is a cross-service configuration value, not per-service.

## Known gaps (don't be surprised by these)

- `PortfolioService` and `PnlConsumerService` have empty `src/main/resources/db/migration` directories — Flyway is wired up but no migrations have been written yet, even though JPA entities already exist.
- `MarketDataService.AlphaVantageService.apiKey` is a plain field with no `@Value`/config binding, so it is always `null` — Alpha Vantage calls will not authenticate until this is wired up.
- Validation/error paths in `PortfolioService.OrderService` (portfolio-not-found, holding-not-found, insufficient-quantity) are stubbed as comments (`// throw new ...Exception()`), not implemented.
- Kafka publish failures are currently only logged; producers note this is a placeholder for an eventual outbox-publisher pattern.
