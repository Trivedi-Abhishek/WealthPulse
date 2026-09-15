# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

WealthPulse implements **Wealth+**, a distributed event-driven fintech platform (see `Project Guide.pdf` at the repo root for the original design brief). Investors register, create portfolios, buy/sell stocks, and get near-real-time P&L tracking, price/portfolio alerts, and AI-powered rebalancing recommendations — built as six independent Spring Boot microservices communicating only through Kafka.

Target architecture (topic → producer → consumer(s)):

```
market.price.updated        MarketDataService  → PnlConsumerService, AlertService, RoboAdvisorService
portfolio.order.executed    PortfolioService   → (audit only, no consumer yet)
portfolio.holdings.updated  PortfolioService   → PnlConsumerService, AlertService, RoboAdvisorService
portfolio.metrics.updated   PnlConsumerService → AlertService, RoboAdvisorService
alert.triggered             AlertService       → NotificationService
portfolio.rebalance.triggered  AlertService    → RoboAdvisorService
```

Build status:
- `PortfolioService`, `MarketDataService`, `PnlConsumerService`, `AlertService`, `RoboAdvisorService` — functionally complete per the target design.
- `NotificationService` — in progress: consumes `alert.triggered`, persists to `notification_history`, exposes `GET /notifications?portfolio_id=`. Does not yet resolve investor contact info (log+persist only, no real delivery) — see Known gaps.

Roadmap: `phasewise.docx` covers Phases 1–7 (build) and the original Phase 8 (hardening). `BUILD_PHASES.md` at the repo root supersedes Phase 8 and is the current plan. **Phase 9 (stabilisation)** — boot blockers and data-correctness bugs found by an audit — is code complete, and its verification gate (G1–G11) is the entry criterion for everything after it. Phases 10–16 split hardening into one workstream each: Swagger, Testcontainers, Outbox, DLT, JWT, observability, CI/CD. Phase 17 collects the remaining functional gaps. Check a phase's entry criteria in `BUILD_PHASES.md` before starting its work, and tick items off there as they land.

## Repository structure

Each service has its own `pom.xml`/`mvnw` and lives in its own top-level directory: `PortfolioService`, `MarketDataService`, `PnlConsumerService`, `AlertService`, `NotificationService`, `RoboAdvisorService`.

The root `pom.xml` is an **aggregator** (`<packaging>pom</packaging>`) listing all six as `<modules>`. It is aggregation only, not inheritance: each service declares `spring-boot-starter-parent` as its own `<parent>` and owns its full dependency set, so nothing is inherited from the root. It exists so IDEs import all six as modules (without it IntelliJ reports "java file is located outside of the module source root" for every service source file) and so `./mvnw clean install` at the root builds everything in one reactor pass. The root's own `src/` — a generated empty `WealthPulseApplication` — was removed when the aggregator was introduced.

For day-to-day work still go into the relevant service directory and use its own `mvnw`; each service builds and runs standalone.

There is no API gateway, service registry, or shared library module — services only communicate via Kafka topics and hand-duplicate the DTO/record definitions they need locally (see `rules/kafka-events.md`).

## Common commands

Stack: Java 21, Spring Boot 4.1.0 (every service pins the same parent version — bump them together). `./mvnw clean install` at the repo root builds all six in one reactor pass. Otherwise, run from inside the specific service directory (e.g. `cd PortfolioService`):

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
- **One Postgres schema per service** inside the shared database: `portfolio`, `pnl`, `alert`, `notification`, `robo`. Each sets both `spring.jpa.properties.hibernate.default_schema` and `spring.flyway.schemas`/`default-schema`, which gives it its own `flyway_schema_history`. Without this, all five services claim `V1` in `public.flyway_schema_history` and only the first to boot passes the checksum check. Query tables schema-qualified (`SELECT * FROM pnl.portfolio_metrics`).
- Kafka: `apache/kafka` KRaft single-node broker at `localhost:9092`, auto-topic-creation enabled (topics are also declared explicitly as `NewTopic` beans in the *producing* service's `config` package).
- Redis: used by `MarketDataService` (last-published price per symbol, 5 min TTL — observability only, not load-bearing since Finnhub supplies the change fields) and `AlertService` (alert dedup, 30 min TTL — load-bearing).
- Ports are fixed per service: Portfolio 8081, MarketData 8082, Pnl 8083, Notification 8085, RoboAdvisor 8086. `AlertService` declares no web starter, so it binds **no port at all** — a failed port probe is expected; check its Kafka consumer group (`alert-service`) to confirm it is running.
- Every service pins the JVM to UTC as the first line of `main()`. The Postgres JDBC driver sends the JVM's default zone in its connection startup packet; a Windows host resolves to the legacy alias `Asia/Calcutta`, which the Postgres 14 image rejects (`FATAL: invalid value for parameter "TimeZone"`), killing every JPA service before Flyway runs. Keep that line in any new service.
- `MarketDataService` needs `FINNHUB_API_KEY` set as an environment variable (e.g. in your IDE run configuration) — never hardcode it in `application.yaml`, which only holds `${FINNHUB_API_KEY:}`. Finnhub's free tier allows ~60 calls/min; polling 5 symbols every 30s uses 10. (It replaced Alpha Vantage, whose free tier dropped to ~25 calls/**day** — roughly 2.5 minutes of polling — and which signalled throttling with HTTP 200 plus an apology body, so nothing ever threw and the circuit breaker recorded throttled calls as successes.)
- `RoboAdvisorService` needs `OPENAI_API_KEY` set the same way (`spring.ai.openai.api-key: ${OPENAI_API_KEY:}`).

## Architecture: event flow across services

`PortfolioService`, `NotificationService`, and `RoboAdvisorService` are the only services exposing REST APIs (full read/write on `PortfolioService`; read-only history/recommendation lookups on `NotificationService`/`RoboAdvisorService`), all consumed by external clients — no synchronous inter-service HTTP calls exist anywhere.

1. `MarketDataService.FinnhubService` polls Finnhub's `GET /quote` (`@Scheduled(fixedDelay = 30000)`) for `AAPL, MSFT, GOOG, AMZN, TSLA` and publishes `MarketStockPriceEvent` to `market.price.updated`. `previousPrice`/`absoluteChange`/`percentageChange` are taken from Finnhub's `pc`/`d`/`dp`, all measured **against the previous close** — not, as before, by diffing the Redis-cached price from the last poll, which measured movement over the preceding 30 seconds instead. Redis still caches the last published price per symbol (5 min TTL) but no longer feeds the change calculation. The HTTP call itself lives in `service/utils/FinnhubClient`, which carries the resilience4j circuit breaker/retry/rate limiter — that separation is load-bearing, since the aspects only apply through the Spring proxy and would be bypassed by self-invocation from the scheduler's own class.
2. `PortfolioService.OrderService.buyOrder`/`sellOrder` (via `OrderController`'s `POST /orders/buy`/`POST /orders/sell`) update `Holdings` (weighted-average price scaled to 4 dp to match `NUMERIC(19,4)`, quantity, `@Version` optimistic locking) inside a `@Transactional` method. `holdings` has `UNIQUE (portfolio_id, symbol)` (`V2__holdings_unique_constraint.sql`). A sell to zero marks the row `I`, and a later re-buy reactivates that row with a fresh cost basis instead of inserting a second one. Both events are produced: `OrderExecutedEvent`→`portfolio.order.executed` (audit trail) and `HoldingUpdatedEvent`→`portfolio.holdings.updated` (canonical, consumed downstream). `OrderService` hands them to Spring's `ApplicationEventPublisher`, and `OrderEventProducer` sends them from `@TransactionalEventListener(AFTER_COMMIT)`. Nothing reaches Kafka for a transaction that fails at commit (optimistic-lock or unique-constraint failure). Publishing inline would leave three read models holding a change this service rolled back. `HoldingUpdatedEvent` also carries the investor's `riskProfile` (looked up via `Portfolio.investorId` → `Investor.riskProfile`, captured at `POST /investors` time) so downstream consumers don't need a synchronous call back to `PortfolioService` to learn it — currently only `RoboAdvisorService` uses this field, but `PnlConsumerService`/`AlertService` still carry it on their own copies of the DTO per the duplication rule in `rules/kafka-events.md`. Also exposes `POST /investors`, `POST /investors/{id}/portfolios`, `GET /orders`, `GET /holdings`.
3. `PnlConsumerService.PnlDataConsumer` listens to `portfolio.holdings.updated` and `market.price.updated` independently, keeps `PortfolioHoldingsSnapshot` current, recalculates `PortfolioMetrics` (current value, invested amount, PnL), and publishes `PortfolioMetricsEvent` to `portfolio.metrics.updated`. It does **not** re-publish holdings — that would make `AlertService`'s holdings copy depend on `PnlConsumerService` being alive, which defeats the point of independent per-service read models.
4. `AlertService.AlertServiceConsumer` listens to all three downstream topics, independently of `PnlConsumerService`. `market.price.updated` → `PriceAlertService` (updates `AlertPortfolioHoldings.latestMarketPrice`, evaluates `PRICE_TARGET`/`STOP_LOSS` configs, then re-evaluates `PORTFOLIO_DRIFT` for every portfolio holding that symbol). `portfolio.metrics.updated` → `PortfolioAlertService` (evaluates `PORTFOLIO_GAIN`/`PORTFOLIO_LOSS`). `portfolio.holdings.updated` → `AlertPortfolioHoldingService` (keeps the local holdings read model current). Alert evaluation is a Strategy pattern: `service/evaluator/AlertEvaluator` has one implementation per `AlertTypeEnum` value, assembled into a `Map<AlertTypeEnum, AlertEvaluator>` bean — add a new alert type by adding a new `@Component` evaluator, not by editing a dispatch switch. `PORTFOLIO_DRIFT` is approximated as single-symbol concentration risk (largest holding's % of portfolio value) since the domain has no explicit target-allocation model. A matched evaluation goes through `AlertFiringService`, which dedups via Redis (`alert:{alertConfigId}`, atomic `SETNX`, 30 min TTL; the key is deleted by a `TransactionSynchronization` if the surrounding transaction does not commit, so a rollback can't suppress the alert for 30 minutes) before writing `AlertHistory` and publishing `alert.triggered` (and, for drift, `portfolio.rebalance.triggered` too).
5. `NotificationService.AlertTriggeredEventConsumer` listens to `alert.triggered` independently, persists each event to `NotificationHistory` (an immutable audit log, mirroring `AlertService`'s `AlertHistory`), and logs it — a stand-in for real delivery (email/SMS/push) until investor contact resolution exists (see Known gaps). Exposes `GET /notifications?portfolio_id=` for read access, following `PortfolioService`'s REST conventions.
6. `RoboAdvisorService.RoboAdvisorEventConsumer` independently consumes `portfolio.holdings.updated`, `market.price.updated`, and `portfolio.metrics.updated` to maintain its own local read model (`RoboPortfolioHolding`/`RoboPortfolioProfile`) — the same per-service-read-model pattern `AlertService` uses, not a shared one. On `portfolio.rebalance.triggered`, `RecommendationService` computes each active holding's weightage, builds a prompt from risk profile/allocation/PnL, and calls OpenAI via `OpenAiChatClient`'s Spring AI `ChatClient` (wrapped in resilience4j circuit breaker/retry, the same external-call-wrapper pattern as `MarketDataService.FinnhubClient`) — on failure the fallback logs and skips persistence rather than writing a partial `Recommendation` row. Exposes `GET /recommendations?portfolio_id=`.

Kafka records are keyed by portfolio ID (or symbol for market data, or symbol as a fallback key for portfolio-less alert configs) for per-entity ordering within a partition; topics have 3 partitions / replication factor 1.

## Per-service package layout

`dal/dto` (event/API payload records, hand-duplicated per service — see `rules/kafka-events.md`), `dal/entity` (JPA entities), `dal/repository` (Spring Data repositories — every repository lives here, no exceptions), `dal/enums` or `enums/` (naming inconsistent between services, check before adding), `kafka/producer`, `kafka/consumer`, `config` (`NewTopic`/`ProducerFactory`/`RedisTemplate`/`WebClient`/`ChatClient` beans), `service`, `controller` (`PortfolioService`, `NotificationService`, `RoboAdvisorService`). `RoboAdvisorService` and `MarketDataService` each have a `service/utils` subpackage holding their external-call wrapper (`OpenAiChatClient`, `FinnhubClient`). Any outbound third-party call belongs in one of these: resilience4j aspects only apply through the Spring proxy, so annotating a private method that the calling class invokes directly silently does nothing.

Each consuming service has its **own** consumer group, set once as `spring.kafka.consumer.group-id` in its `application.yaml` (`pnl-consumer-service`, `alert-service`, `notification-service`, `robo-advisor-service`); `@KafkaListener` annotations deliberately carry no `groupId`. This is what makes the fan-out above work: Kafka assigns each partition to only one member of a group, so the previous single shared `wealth-plus-service-group` split every topic *between* services instead of delivering it to each — verified live, where one buy order reached only RoboAdvisorService and never PnlConsumerService or AlertService.

## Coding standards

See `rules/` for the conventions established while building this out — read the relevant file before adding a new service, entity, Kafka producer/consumer, or REST endpoint:

- `rules/data-layer.md` — entity/repository conventions, the `application.yaml`/`pom.xml` boilerplate every JPA+Flyway service needs (and silently breaks without).
- `rules/kafka-events.md` — event DTO duplication discipline, producer/consumer serialization pattern, topic ownership.
- `rules/api-and-validation.md` — REST controller/DTO/exception-handling conventions.

## Known gaps (don't be surprised by these)

- `NotificationService` consumes `alert.triggered` but cannot resolve which investor/email to notify — no event in the system carries investor identity or contact info past `PortfolioService`'s own DB (`Portfolio.investorId` and `Investor.email` are never published to Kafka). Currently just logs + persists to `notification_history`. Resolving this for real requires threading `investorId` through `HoldingUpdatedEvent` → `AlertPortfolioHoldings`/`AlertTriggeredEvent`, plus a new `investor.registered` event from `PortfolioService` — a multi-service follow-up, not a `NotificationService`-only change.
- No `AlertConfiguration` rows are seeded anywhere (no admin API to create them yet) — `AlertService`'s evaluators are fully wired but will never fire until some rows exist in that table, which transitively means `RoboAdvisorService` never receives a `portfolio.rebalance.triggered` event to act on either, even though it's fully built and wired.
- `RoboAdvisorService`'s AI prompt omits "Investment Horizon" (one of the inputs named in the original `Project Guide.pdf` brief, alongside risk profile/allocation/PnL) — no field for it exists anywhere in the data model, so it isn't fabricated. Threading it through would need the same kind of `Investor`-field-plus-event-field change `riskProfile` just got.
- Kafka publish failures are only logged. `OrderEventProducer` (×2) and `AlertEventProducer` carry an outbox `Todo` comment; the after-commit listener is an interim fix. The real fix is a transactional outbox for the three DB-backed producers — `MarketDataService` is deliberately excluded (Phase 12).
- Consumers swallow `JsonProcessingException`, so a malformed or drifted payload is logged and dropped — there is no dead-letter topic yet (Phase 13).
- `RoboAdvisorService`: the "weightage" map holds raw market value (`price × quantity`), never divided by portfolio value, but the prompt labels it weightage. `OpenAiChatClient` puts `fallbackMethod` on the inner `@CircuitBreaker`, so the outer `@Retry` always sees a success and never retries. Both errors log `e.getMessage()` without a stack trace (Phase 17).
- Alert direction is unvalidated: `PRICE_TARGET`/`STOP_LOSS` and `PORTFOLIO_GAIN`/`PORTFOLIO_LOSS` evaluators differ only in message text, so a misconfigured `STOP_LOSS … GREATER_THAN` row fires with a misleading "dropped below" message (Phase 17).
- `PortfolioController`: the query param is misspelled `portfolio_mame` (clients must send that spelling until fixed), and the `String` path variable goes through `Long.valueOf`, so a non-numeric id is a 500 not a 400 (Phase 10).
- No tests beyond the generated `*ApplicationTests` smoke test in any service (Phase 11).
- No Swagger, JWT/auth, metrics/tracing, or CI/CD — each is its own phase in `BUILD_PHASES.md` (10, 14, 15, 16).
