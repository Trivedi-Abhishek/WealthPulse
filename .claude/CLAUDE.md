# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

WealthPulse implements **Wealth+**, a distributed event-driven fintech platform (see `Project Guide.pdf` at the repo root for the original design brief). Investors register, create portfolios, buy/sell stocks, and get near-real-time P&L tracking, price/portfolio alerts, and (planned) AI-powered rebalancing recommendations — built as six independent Spring Boot microservices communicating only through Kafka.

Target architecture (topic → producer → consumer(s)):

```
market.price.updated        MarketDataService  → PnlConsumerService, AlertService
portfolio.order.executed    PortfolioService   → (audit only, no consumer yet)
portfolio.holdings.updated  PortfolioService   → PnlConsumerService, AlertService
portfolio.metrics.updated   PnlConsumerService → AlertService
alert.triggered             AlertService       → NotificationService (not built yet)
portfolio.rebalance.triggered  AlertService    → RoboAdvisorService (not built yet)
```

Build status:
- `PortfolioService`, `MarketDataService`, `PnlConsumerService`, `AlertService` — functionally complete per the target design.
- `NotificationService`, `RoboAdvisorService` — not started.

## Repository structure

Each service has its own `pom.xml`/`mvnw` and lives in its own top-level directory: `PortfolioService`, `MarketDataService`, `PnlConsumerService`, `AlertService` (and eventually `NotificationService`, `RoboAdvisorService`). The root `pom.xml`/`src`/`mvnw` are leftover scaffolding from initial project generation — not a parent/aggregator, not part of the running system. Always work inside the relevant service directory.

There is no API gateway, service registry, or shared library module — services only communicate via Kafka topics and hand-duplicate the DTO/record definitions they need locally (see `rules/kafka-events.md`).

## Common commands

Run from inside the specific service directory (e.g. `cd PortfolioService`):

```bash
./mvnw clean install          # build; mvnw.cmd on native Windows shells
./mvnw test                   # run tests
./mvnw test -Dtest=SomeClassTests
./mvnw test -Dtest=SomeClassTests#someMethod
./mvnw spring-boot:run        # run locally
```

Each service currently only has the Spring Boot–generated `*ApplicationTests` context-load smoke test — no other test coverage yet (planned for the hardening pass).

### Local infrastructure

```bash
docker compose up -d   # from repo root — starts Postgres (5432), Kafka (9092), Redis (6379)
```

- Postgres: `wealth_plus_db`, user/password `user`/`password`. Every JPA-backed service needs `spring.datasource`/`spring.jpa`/`spring.flyway` config and the `org.postgresql:postgresql` runtime driver explicitly declared — neither is inherited automatically from `spring-boot-starter-data-jpa`/`spring-boot-starter-flyway` (see `rules/data-layer.md`).
- Kafka: `apache/kafka` KRaft single-node broker at `localhost:9092`, auto-topic-creation enabled (topics are also declared explicitly as `NewTopic` beans in the *producing* service's `config` package).
- Redis: used by `MarketDataService` (price cache, 30s TTL) and `AlertService` (alert dedup, 30 min TTL).
- No service sets `server.port`; all default to `8080` — running more than one locally at once needs `--server.port=...`.
- `MarketDataService` needs `ALPHA_VANTAGE_API_KEY` set as an environment variable (e.g. in your IDE run configuration) — never hardcode it in `application.yaml`, which only holds `${ALPHA_VANTAGE_API_KEY:}`.

## Architecture: event flow across services

`PortfolioService` is the only service exposing a REST API, consumed by external clients — no synchronous inter-service HTTP calls exist anywhere.

1. `MarketDataService.AlphaVantageService` polls Alpha Vantage (`@Scheduled(fixedDelay = 30000)`, wrapped in resilience4j circuit breaker/retry/rate limiter) for `AAPL, MSFT, GOOG, AMZN, TSLA`, diffs against the price cached in Redis, computes `absoluteChange`/`percentageChange`, and publishes `MarketStockPriceEvent` to `market.price.updated`.
2. `PortfolioService.OrderService.buyOrder`/`sellOrder` (via `OrderController`'s `POST /orders/buy`/`POST /orders/sell`) update `Holdings` (weighted-average price, quantity, `@Version` optimistic locking) inside a `@Transactional` method, then publish **both** `OrderExecutedEvent`→`portfolio.order.executed` (audit trail) and `HoldingUpdatedEvent`→`portfolio.holdings.updated` (canonical, consumed downstream) — itself, directly. Also exposes `POST /investors`, `POST /investors/{id}/portfolios`, `GET /orders`, `GET /holdings`.
3. `PnlConsumerService.PnlDataConsumer` listens to `portfolio.holdings.updated` and `market.price.updated` independently, keeps `PortfolioHoldingsSnapshot` current, recalculates `PortfolioMetrics` (current value, invested amount, PnL), and publishes `PortfolioMetricsEvent` to `portfolio.metrics.updated`. It does **not** re-publish holdings — that would make `AlertService`'s holdings copy depend on `PnlConsumerService` being alive, which defeats the point of independent per-service read models.
4. `AlertService.AlertServiceConsumer` listens to all three downstream topics, independently of `PnlConsumerService`. `market.price.updated` → `PriceAlertService` (updates `AlertPortfolioHoldings.latestMarketPrice`, evaluates `PRICE_TARGET`/`STOP_LOSS` configs, then re-evaluates `PORTFOLIO_DRIFT` for every portfolio holding that symbol). `portfolio.metrics.updated` → `PortfolioAlertService` (evaluates `PORTFOLIO_GAIN`/`PORTFOLIO_LOSS`). `portfolio.holdings.updated` → `AlertPortfolioHoldingService` (keeps the local holdings read model current). Alert evaluation is a Strategy pattern: `service/evaluator/AlertEvaluator` has one implementation per `AlertTypeEnum` value, assembled into a `Map<AlertTypeEnum, AlertEvaluator>` bean — add a new alert type by adding a new `@Component` evaluator, not by editing a dispatch switch. `PORTFOLIO_DRIFT` is approximated as single-symbol concentration risk (largest holding's % of portfolio value) since the domain has no explicit target-allocation model. A matched evaluation goes through `AlertFiringService`, which dedups via Redis (`alert:{alertConfigId}`, atomic `SETNX`, 30 min TTL) before writing `AlertHistory` and publishing `alert.triggered` (and, for drift, `portfolio.rebalance.triggered` too).

Kafka records are keyed by portfolio ID (or symbol for market data, or symbol as a fallback key for portfolio-less alert configs) for per-entity ordering within a partition; topics have 3 partitions / replication factor 1.

## Per-service package layout

`dal/dto` (event/API payload records, hand-duplicated per service — see `rules/kafka-events.md`), `dal/entity` (JPA entities), `dal/repository` (Spring Data repositories — every repository lives here, no exceptions), `dal/enums` or `enums/` (naming inconsistent between services, check before adding), `kafka/producer`, `kafka/consumer`, `config` (`NewTopic`/`ProducerFactory`/`RedisTemplate`/`WebClient` beans), `service`, `controller` (`PortfolioService` only).

All Kafka consumers share `group-id: wealth-plus-service-group` across services — a cross-service value, not per-service.

## Coding standards

See `rules/` for the conventions established while building this out — read the relevant file before adding a new service, entity, Kafka producer/consumer, or REST endpoint:

- `rules/data-layer.md` — entity/repository conventions, the `application.yaml`/`pom.xml` boilerplate every JPA+Flyway service needs (and silently breaks without).
- `rules/kafka-events.md` — event DTO duplication discipline, producer/consumer serialization pattern, topic ownership.
- `rules/api-and-validation.md` — REST controller/DTO/exception-handling conventions.

## Known gaps (don't be surprised by these)

- `NotificationService` and `RoboAdvisorService` don't exist yet — nothing currently consumes `alert.triggered` or `portfolio.rebalance.triggered`.
- No `AlertConfiguration` rows are seeded anywhere (no admin API to create them yet) — `AlertService`'s evaluators are fully wired but will never fire until some rows exist in that table.
- Kafka publish failures are only logged; every producer has a `// TODO: outbox publisher` comment marking this as a known placeholder.
- No tests beyond the generated `*ApplicationTests` smoke test in any service.
- No JWT/auth, no distributed tracing, no CI/CD — all deliberately deferred to a final hardening phase once all six services are functionally complete.
