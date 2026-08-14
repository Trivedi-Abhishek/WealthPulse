# REST API, DTO, and validation conventions

Currently applies to `PortfolioService` — the only service with a REST API — but follow this convention if any other service ever grows one (e.g. an admin API on `AlertService`).

## Request DTOs

Plain `@Data` classes (not records — Jakarta Bean Validation annotations need mutability-friendly getters/setters, and Spring's `@RequestBody` binding is simplest with them), `@JsonProperty("snake_case")` on every field (the wire format is snake_case even though Java fields are camelCase), `jakarta.validation.constraints` annotations matching the field's actual semantics:

```java
@Data
public class CreateThingRequest {
    @NotNull
    @Positive
    @JsonProperty("portfolio_id")
    private Long portfolioId;

    @NotBlank
    @JsonProperty("symbol")
    private String symbol;
}
```

**Only apply a constraint annotation to a type it actually supports.** `@Positive` only works on numeric types — it was mistakenly applied to an `OrderTypeEnum` field once in this codebase, which would have thrown `UnexpectedTypeException` from Hibernate Validator at request-validation time (a bug that would have taken down every order-creation request). Check the annotation's supported-types list before adding it, not just its name.

Controllers take `@RequestBody @Valid SomeRequest` — the `@Valid` is not optional, Spring won't validate without it.

## Response shape

Create endpoints return `ResponseEntity<Long>` with just the created entity's ID — consistent across `POST /investors`, `POST /investors/{id}/portfolios`, `POST /orders/buy`, `POST /orders/sell`. Read endpoints return `ResponseEntity<List<Entity>>` of the JPA entity directly (no separate response DTO layer yet — fine at this scale, revisit if entities start needing to hide fields from the API).

## Exception handling

Every domain failure gets its own exception class under `exception/` (e.g. `PortfolioNotFoundException`, `InsufficientHoldingException`) constructed with enough context to produce a useful message — never a bare `// throw error` comment left as a TODO. A single `@RestControllerAdvice` `GlobalExceptionHandler` per service maps exception types to HTTP status:

- "not found" exceptions → 404
- "already exists" / duplicate exceptions → 409
- business-rule violations (insufficient holding, etc.) → 422 (`HttpStatus.UNPROCESSABLE_CONTENT` — note `UNPROCESSABLE_ENTITY` is deprecated in this Spring version)
- `MethodArgumentNotValidException` (bean validation failures) → 400, with field-level messages joined into one string

Response body is a small `ErrorResponse` record: `(Instant timestamp, int status, String error, String message)`. Don't leak stack traces or entity internals into the error response.

## Validate at the service layer, not just the controller

Bean validation (`@Valid`) only checks the shape of the request. Business invariants (does this portfolio exist, does this investor already have a portfolio with this name, is there enough holding quantity to sell) are checked in the `@Service` class itself, before any mutation, and throw the domain exceptions above. Check-then-throw at the top of the method, before any repository writes — don't let a partial write happen before validation fails.
