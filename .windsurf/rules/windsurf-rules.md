---
trigger: manual
description: 
globs: 
---

# WindSurf Workspace Rules for Java & Spring Boot

## 1. Java Code Style & Best Practices

-   Prefer Java 21+ features such as records, sealed classes, switch
    expressions.
-   Use Lombok for boilerplate (Getter, Setter, Builder,
    RequiredArgsConstructor).
-   Use Optional only for return types, not fields or method parameters.
-   Follow effective Java naming conventions for methods, classes, and
    variables.
-   Always format code using standard Google Java Format.
-   Prefer immutability where possible.

## 2. Spring Boot REST + Service Layer Structure

-   For every controller, ensure there is a matching service layer
    interface + implementation.
-   Controllers must not contain business logic; they should delegate
    everything to services.
-   DTOs must be used for all external API interactions---never expose
    entities directly.
-   Each API response must be wrapped in a common Response wrapper
    (e.g. ApiResponse`<T>`{=html}).
-   Use @Validated on controllers and @NotNull/@NotBlank on DTO fields.

## 3. Error Handling Rules

-   Always generate a GlobalExceptionHandler using
    @RestControllerAdvice.
-   Handle NotFound, BadRequest, ValidationError, and
    InternalServerError separately.
-   Never expose stack traces or internal error messages in controllers.

## 4. Persistence Layer Rules (JPA / R2DBC)

- Use jakarta persistance
-   Use Spring Data repositories for basic operations.
-   For complex queries, create custom repository interfaces +
    implementations.
-   Prefer @Entity with @Builder, @NoArgsConstructor,
    @AllArgsConstructor from Lombok.
-   Always include createdAt, updatedAt fields with @CreationTimestamp
    and @UpdateTimestamp OR R2DBC audit columns.

## 5. Dependency Injection & Architecture

-   Always use constructor injection, never field injection.
-   Services must be stateless.
-   Avoid circular dependencies; AI should warn when code hints at them.

## 6. Testing Rules

-   For each service, generate JUnit 5 tests with Mockito.
-   For each controller, generate WebMvcTest or WebFluxTest depending on
    stack.
-   Avoid static mocking unless strictly necessary.

## 7. Build & Dependencies

-   Use Gradle Kotlin DSL or Maven with dependency management.
-   Prefer Spring Boot starters over manually adding dependencies.
-   Use MapStruct for DTO \<-\> Entity mapping.
-   Prefer Flyway or Liquibase for DB migrations.

## 8. Performance & Reactive (if WebFlux)

-   Never block inside reactive code (avoid .block() / .subscribe()).
-   Use flatMap/concatMap carefully depending on ordering.
-   Limit concurrency where needed with parallelism parameters.
-   Use caching and connection pooling best practices.

## 9. Code Generation Preferences

-   When asked to generate code, include imports.
-   Organize imports automatically.
-   Always generate clean package structures: controller, service,
    service.impl, dto, entity, repository, config, exception, util.
-   Never generate placeholder or unused code.

## Optional: Automatic Responses Rules

-   Always use @Slf4j for logging.
-   Generate response examples using JSON.
-   Include curl command for any REST endpoint generated.
