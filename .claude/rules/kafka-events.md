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

This same `ObjectMapper`-based pattern is also used for anything else that needs to serialize a domain object to a string payload — e.g. `MarketDataService`'s Redis price cache uses it too, for consistency, instead of a Spring Data Redis Jackson serializer wrapper.

## Consumer pattern

`@KafkaListener(topics = "...", groupId = "wealth-plus-service-group")` methods take a plain `String` parameter and deserialize with `objectMapper.readValue(payload, SomeEvent.class)` inside a try/catch on `JsonProcessingException`. All consumer group IDs are the shared `wealth-plus-service-group` — this is intentional and cross-service, don't give a new service its own group ID without a specific reason.

## Topic ownership

A `NewTopic` `@Bean` for a given topic is declared **only** in the `config` package of the service that produces to it — never in a consuming service. If a topic's producer changes (as happened when `portfolio.holdings.updated` moved from being republished by `PnlConsumerService` to being published directly by `PortfolioService`), the `NewTopic` bean moves with it.

## `application.yaml` gotcha

Kafka config must be nested under `spring.kafka`, not a bare root-level `kafka:` key — a root-level key is silently ignored by Spring Boot's autoconfiguration (this bug existed in `MarketDataService` and was entirely missing in `AlertService`; both needed fixing). If a service's producer/consumer isn't picking up serializer config, check the YAML nesting first.
