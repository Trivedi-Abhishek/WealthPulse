# Kafka & event conventions

## DTO duplication is real and it will bite you if you're not careful

There is no shared events module. Every event record (`MarketStockPriceEvent`, `HoldingUpdatedEvent`, `PortfolioMetricsEvent`, ...) is hand-copied under `dal/dto` into every service that produces **or** consumes it. Jackson's default `FAIL_ON_UNKNOWN_PROPERTIES` is `true` and nothing in this codebase turns it off — so if the producer's copy of a record gains a field and a consumer's copy doesn't, deserialization throws on every single message from that point on. It fails silently (caught by the same `catch (JsonProcessingException e) { log.error(...) }` pattern used everywhere), not loudly — this already happened once in this codebase (`MarketStockPriceEvent` gained `absoluteChange`/`percentageChange` in `MarketDataService` without the downstream copies being updated in the same change).

**Rule: any change to an event record's shape must be applied, in the same change, to every copy of that DTO across every producing and consuming service.** Grep for the record name across all service directories before considering the change done.

## Producer pattern (standardized)

Every producer uses `KafkaTemplate<String, String>` (not a typed `KafkaTemplate<String, T>` with a custom serializer — that was tried once, in `PortfolioService.OrderEventProducer`, and reverted for consistency) plus a plain `ObjectMapper`:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SomeProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishSomeEvent(SomeEvent event) {
        try {
            CompletableFuture<SendResult<String, String>> future =
                kafkaTemplate.send("some.topic", String.valueOf(event.entityId()), objectMapper.writeValueAsString(event));
            future.whenComplete((res, exc) -> {
                if (Objects.isNull(exc)) {
                    log.info("Published successfully. topic={}, partition={}, offset={}",
                        res.getRecordMetadata().topic(), res.getRecordMetadata().partition(), res.getRecordMetadata().offset());
                } else {
                    // TODO: logging only for now — replace with an Outbox Publisher (see rules/, hardening phase)
                    log.error("Exception occurred: ", exc);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize {}", event.entityId(), e);
        }
    }
}
```

### Publishing from a `@Transactional` method: after commit only

`KafkaTemplate.send` hands the record to the sender thread immediately, so it is not rolled back with the database. If the event is sent inline from a `@Transactional` method, a commit that fails afterwards leaves every downstream read model holding a change that never happened. Examples: an optimistic-lock conflict or a unique-constraint violation.

When a producer is triggered by a DB mutation, the service calls `ApplicationEventPublisher.publishEvent(event)` inside the transaction. The producer method is annotated `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` instead of being called directly. See `PortfolioService.OrderService` → `OrderEventProducer`. Caveat: `@TransactionalEventListener` **silently drops** events published outside an active transaction, so only use it on paths that are always transactional.

This is an interim measure. A crash between commit and send still loses the event. The transactional outbox (Phase 12 in `BUILD_PHASES.md`) replaces it for the DB-backed producers.

This same `ObjectMapper`-based pattern is also used for anything else that needs to serialize a domain object to a string payload — e.g. `MarketDataService`'s Redis price cache uses it too, for consistency, instead of a Spring Data Redis Jackson serializer wrapper.

## Consumer pattern

`@KafkaListener(topics = "...")` methods take a plain `String` parameter and deserialize with `objectMapper.readValue(payload, SomeEvent.class)` inside a try/catch on `JsonProcessingException`.

**Every consuming service gets its own consumer group**, declared once as `spring.kafka.consumer.group-id` in that service's `application.yaml`. Do not put `groupId` on the annotation — the yaml value is the single source per service.

This rule reverses an earlier one. The codebase originally required every service to share `wealth-plus-service-group`, described as intentional. It was wrong, and it silently broke the architecture: Kafka assigns each partition to exactly one member of a group, so services sharing a group *split* a topic's partitions between them rather than each receiving every message. Verified live with all six services running — one buy order's `HoldingUpdatedEvent` reached only RoboAdvisorService, leaving PnlConsumerService and AlertService with empty read models, while a price partition carrying no symbols sat idle on another service. A new consumer must pick a new, unique group id.

Changing an existing service's group id resets its offsets: with `auto-offset-reset: earliest` it replays the topic from the beginning on next start. That is safe here because the read-model upserts are idempotent, but do it deliberately.

**Don't set `spring.kafka.listener.ack-mode: manual`** unless the listener takes an `Acknowledgment` parameter and calls `acknowledge()`. Four services once had it set with no such parameter, so offsets never committed and every restart replayed the whole topic. The default `BATCH` mode is what the listener code in this repo assumes.

Delivery is at-least-once, so every consumer must be idempotent. Read-model writes are upserts on the table's unique key. `NotificationService`'s audit-log insert is the one append-only consumer; it relies on committed offsets to avoid duplicates.

## Topic ownership

A `NewTopic` `@Bean` for a given topic is declared **only** in the `config` package of the service that produces to it — never in a consuming service. If a topic's producer changes (as happened when `portfolio.holdings.updated` moved from being republished by `PnlConsumerService` to being published directly by `PortfolioService`), the `NewTopic` bean moves with it.

## `application.yaml` gotcha

Kafka config must be nested under `spring.kafka`, not a bare root-level `kafka:` key — a root-level key is silently ignored by Spring Boot's autoconfiguration (this bug existed in `MarketDataService` and was entirely missing in `AlertService`; both needed fixing). If a service's producer/consumer isn't picking up serializer config, check the YAML nesting first.
