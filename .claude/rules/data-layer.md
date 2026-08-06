# Data layer conventions

Applies to any service using `spring-boot-starter-data-jpa` + `spring-boot-starter-flyway` (currently `PortfolioService`, `PnlConsumerService`, `AlertService`).

## Every JPA+Flyway service needs this boilerplate, or it silently doesn't work

None of it is inherited automatically from the starters. Missing pieces don't fail loudly at compile time — they fail at boot (missing datasource) or, worse, get silently swallowed by a `catch (JsonProcessingException e) { log.error(...) }` block elsewhere. Checklist for any new JPA-backed service:

1. **`pom.xml`**: explicit `org.postgresql:postgresql` dependency, `<scope>runtime</scope>`. `spring-boot-starter-data-jpa` does not pull a driver.
2. **`application.yaml`**:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/wealth_plus_db
       username: user
       password: password
     jpa:
       open-in-view: false
       hibernate:
         ddl-auto: validate   # schema comes from Flyway, never from Hibernate auto-DDL
     flyway:
       enabled: true
   ```
3. **A `V1__init.sql`** under `src/main/resources/db/migration` — the directory exists from project generation but starts empty. Write it before writing any repository query against a table that doesn't exist yet.

`ddl-auto: validate` (not `update`) is deliberate: Flyway migrations are the single source of truth for schema, and `validate` makes a schema/entity mismatch a loud boot-time failure instead of Hibernate silently altering the table.

## jackson-datatype-jsr310 is not optional if any DTO has a `java.time` field

Every event record in this project has an `Instant` and/or `LocalDate` field. Plain Jackson (`com.fasterxml.jackson.databind.ObjectMapper`) cannot serialize/deserialize `java.time` types without `jackson-datatype-jsr310` on the classpath — and `spring-boot-starter-webmvc`/`webclient` do **not** reliably pull it in as a transitive dependency in this project's dependency tree (confirmed missing in `MarketDataService`, `PnlConsumerService`, and `AlertService` — each needed it added explicitly). Without it, `ObjectMapper.writeValueAsString`/`readValue` throws on every call involving one of these DTOs — and because every producer/consumer in this codebase catches `JsonProcessingException` and just logs it, **the failure is invisible unless you check the logs**. `MarketDataService`'s price-publishing path had been silently broken this way since day one.

Checklist: any service that serializes or deserializes a Kafka event DTO needs
```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```
declared explicitly. Don't assume it's transitively present — verify with `./mvnw dependency:tree -Dincludes=com.fasterxml.jackson.datatype:jackson-datatype-jsr310` if unsure.

## Entity conventions

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "snake_case_table_name")
@EntityListeners(AuditingEntityListener.class)   // only if the entity actually has @CreatedDate/@LastModifiedDate fields
public class Thing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // ...
    @Column(name = "created_date", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdDate;
}
```

- Soft-delete/active-flag pattern: a `StatusEnum { A, I }` column (`A`ctive/`I`nactive), never hard deletes. Each service defines its own copy of `StatusEnum` — check the actual package (`dal/enums` vs `enums/`) before adding to it, the convention is inconsistent across services.
- `@Version` for optimistic locking on any entity mutated by concurrent operations (see `Holdings.version` in `PortfolioService`) — required wherever more than one request could race on the same row (buy/sell orders against the same holding, for example).
- Every repository lives under `dal/repository`, implements `JpaRepository<Entity, Long>`, and is annotated `@Repository`. Don't derive query methods you don't need yet — add them when the calling code needs them, not speculatively.
- Give every table a migration-level index/unique-constraint that matches what the repository's derived-query methods assume about uniqueness (e.g. if `findByPortfolioIdAndSymbol` is used as an upsert lookup, the table needs a unique constraint on `(portfolio_id, symbol)` — enforce at the DB, don't rely on application-level race-free assumptions).
