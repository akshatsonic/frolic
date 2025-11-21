# Frolic Gamification System - Simplified Multi-Module Implementation Plan

## Project Overview
A horizontally scalable gamification platform for campaigns, games, and probabilistic reward allocations under massive concurrent traffic using event-driven architecture.

---

## Technology Stack

- **Language:** Java 21 (with Virtual Threads)
- **Framework:** Spring Boot 3.3.x
- **Build Tool:** Maven (Multi-module)
- **Persistence:** Jakarta Persistence API (JPA)
- **Database:** PostgreSQL
- **Migration:** Liquibase
- **Cache:** Redis (with Lua scripting)
- **Message Bus:** Apache Kafka
- **WebSocket:** Spring WebSocket with STOMP
- **Metrics:** Micrometer + Prometheus
- **Testing:** JUnit 5, Mockito, Testcontainers

---

## Simplified 2-Module Architecture

```
frolic/ (parent)
├── pom.xml                    # Parent POM with dependency management
├── frolic-core/               # Core library with all shared logic
│   ├── common/                # DTOs, enums, utilities, constants
│   ├── domain/                # Domain models & business interfaces
│   ├── repository/            # JPA entities, repositories, Liquibase
│   ├── cache/                 # Redis integration & Lua scripts
│   ├── messaging/             # Kafka producers & consumers
│   ├── engine/                # Probability & concurrency control engines
│   └── monitoring/            # Metrics, health checks, observability
└── frolic-services/           # All executable services in one module
    ├── ingestion/             # Play ingestion REST API
    ├── allocator/             # Reward allocation worker
    ├── websocket/             # WebSocket server
    ├── coupon/                # Coupon management
    └── admin/                 # Admin API
```

---

## Module 1: frolic-core

**Type:** Library  
**Packaging:** JAR  
**Purpose:** Shared library containing all business logic, data access, and infrastructure

### Package Structure

```
com.frolic.core
├── common/
│   ├── dto/           # PlayEventDto, PlayResultDto, CouponDto, etc.
│   ├── enums/         # GameStatus, CampaignStatus, CouponStatus, etc.
│   ├── exception/     # BusinessException, TechnicalException, etc.
│   ├── util/          # IdGenerator, TimeUtils, JsonUtils
│   └── constant/      # RedisKeys, KafkaTopics, ConfigKeys
│
├── domain/
│   ├── model/         # Campaign, Game, Brand, Coupon, etc.
│   ├── service/       # ProbabilityCalculator, BudgetManager, etc.
│   └── event/         # GameStartedEvent, CouponAllocatedEvent, etc.
│
├── repository/
│   ├── entity/        # CampaignEntity, GameEntity, BrandEntity, etc.
│   ├── jpa/           # Spring Data JPA repositories
│   └── config/        # DatabaseConfig
│
├── cache/
│   ├── config/        # RedisConfig
│   ├── store/         # RedisResultStore, RedisBudgetStore, etc.
│   ├── key/           # CacheKeyBuilder
│   └── script/        # Lua scripts (budget_decrement.lua, etc.)
│
├── messaging/
│   ├── config/        # KafkaProducerConfig, KafkaConsumerConfig
│   ├── producer/      # PlayEventProducer, AllocationResultProducer
│   ├── consumer/      # AbstractKafkaConsumer
│   └── serializer/    # JsonSerializer, JsonDeserializer
│
├── engine/
│   ├── probability/   # ProbabilityCalculatorImpl, SlotCalculator
│   └── concurrency/   # AtomicBudgetDecrementer, IdempotencyHandler
│
└── monitoring/
    ├── metrics/       # Custom metrics (AllocationMetrics, etc.)
    ├── health/        # Health indicators
    └── config/        # MonitoringConfig
```

### Key Features

#### Database Schema (Liquibase)
```
campaigns           # Campaign metadata
games               # Game details with start/end times
brands              # Brand information
game_brand_budgets  # Budget allocation per game-brand
coupons             # Coupon pool with status tracking
play_events         # Play event audit log
users               # User information
```

#### Redis Keys
```
budget:game:{gameId}:brand:{brandId}  => INT (remaining coupons)
result:{playId}                        => JSON (winner, couponId, etc.)
slots:game:{gameId}                    => INT (cached, TTL 1s)
play_processed:{playId}                => BOOL (idempotency, TTL 24h)
```

#### Kafka Topics
```
play-events          # Play event stream (partitioned by gameId)
allocation-results   # Allocation audit trail
coupon-issued        # Coupon issuance events
game-lifecycle       # Game start/stop events
```

#### Probability Algorithm
```java
P_base = remainingBudget / remainingSlots

if (P_base < 1) {
    // Probabilistic: single winner possible
    if (random() < P_base) attemptAllocate(1);
} else {
    // Deterministic + fractional: multiple winners
    int fixedWinners = (int) Math.floor(P_base);
    double fractional = P_base - fixedWinners;
    int totalWinners = fixedWinners + (random() < fractional ? 1 : 0);
    attemptAllocate(Math.min(totalWinners, remainingBudget));
}
```

#### Atomic Budget Decrement (Lua Script)
```lua
-- budget_decrement.lua
-- KEYS[1] = budget key, ARGV[1] = decrement amount
local current = tonumber(redis.call('GET', KEYS[1]) or '0')
local amount = tonumber(ARGV[1])
if current >= amount then
    redis.call('DECRBY', KEYS[1], amount)
    return current - amount
else
    return -1
end
```

### Dependencies
```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.liquibase</groupId>
        <artifactId>liquibase-core</artifactId>
    </dependency>
    
    <!-- Redis -->
    <dependency>
        <groupId>io.lettuce</groupId>
        <artifactId>lettuce-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
    </dependency>
    
    <!-- Resilience -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring-boot3</artifactId>
    </dependency>
    
    <!-- Utilities -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-math3</artifactId>
    </dependency>
    
    <!-- Monitoring -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

---

## Module 2: frolic-services

**Type:** Single Executable Service  
**Packaging:** JAR (Spring Boot)  
**Purpose:** Unified Spring Boot application with all features (Port 8080)

### Package Structure

```
com.frolic.services
├── FrolicApplication             # Single Main class
│
├── controller/                   # All REST & WebSocket controllers
│   ├── play/
│   │   └── PlayController        # POST /api/v1/play
│   ├── websocket/
│   │   └── GameWebSocketController # WebSocket endpoint
│   ├── coupon/
│   │   └── CouponController      # Coupon APIs
│   └── admin/
│       ├── CampaignController    # Campaign CRUD
│       ├── GameController        # Game CRUD
│       ├── BrandController       # Brand CRUD
│       └── MetricsController     # Dashboard metrics
│
├── service/                      # Business services
│   ├── play/
│   │   ├── PlayIngestionService
│   │   └── ValidationService
│   ├── allocation/
│   │   ├── RewardAllocationService
│   │   ├── BudgetCheckService
│   │   └── CouponReservationService
│   ├── websocket/
│   │   ├── ResultPollingService
│   │   ├── SessionManagerService
│   │   └── ResultPushService
│   ├── coupon/
│   │   ├── CouponIssuanceService
│   │   ├── CouponPoolService
│   │   ├── CouponRedemptionService
│   │   └── BulkCouponUploadService
│   └── admin/
│       ├── CampaignService
│       ├── GameService
│       ├── BrandService
│       └── BudgetInitializationService
│
├── consumer/                     # Kafka consumers
│   ├── PlayEventConsumer         # Consume play-events
│   └── AllocationResultListener  # Consume allocation-results
│
├── config/                       # Configuration classes
│   ├── VirtualThreadConfig       # Virtual threads setup
│   ├── WebSocketConfig           # WebSocket configuration
│   ├── KafkaConsumerConfig       # Kafka consumer setup
│   └── SecurityConfig            # CORS, etc.
│
└── filter/                       # Filters & interceptors
    └── LoggingFilter             # Request/response logging
```

### Unified Application Features

The **FrolicApplication** is a single Spring Boot application running on **Port 8080** that provides:

#### 1. Play Ingestion API
**Controller:** `PlayController`  
**Endpoint:** `POST /api/v1/play`

```java
@RestController
@RequestMapping("/api/v1")
public class PlayController {
    @PostMapping("/play")
    public ResponseEntity<PlayResponse> submitPlay(@RequestBody PlayRequest request) {
        // Validate, generate playId, publish to Kafka
        return ResponseEntity.accepted().body(response);
    }
}
```

**Flow:**
1. Validate request (game active, user eligible)
2. Generate unique playId
3. Publish PlayEvent to Kafka
4. Return 202 Accepted with playId

**Features:** Virtual threads, request validation

---

#### 2. Reward Allocation (Background Consumer)
**Consumer:** `PlayEventConsumer`  
**Kafka Topic:** `play-events`

```java
@Component
public class PlayEventConsumer {
    @KafkaListener(
        topics = "play-events",
        groupId = "reward-allocator-group",
        concurrency = "10"
    )
    public void consumePlayEvent(PlayEvent event) {
        // Check idempotency → Calculate probability → Atomic decrement → Write result
    }
}
```

**Flow:**
1. Consume PlayEvent from Kafka
2. Check idempotency (already processed?)
3. Calculate probability (P = RB / remainingSlots)
4. Determine winner/loser
5. Attempt atomic budget decrement (if winner)
6. Write result to Redis
7. Publish allocation result for audit

**Features:** Atomic operations, idempotency, retry logic, DLQ

---

#### 3. WebSocket Real-time Updates
**Controller:** `GameWebSocketController`  
**Endpoint:** `/ws/game` (STOMP over WebSocket)

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/game")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
```

**Flow:**
1. Client connects and subscribes to `/topic/result/{playId}`
2. After 10 seconds, poll Redis for result
3. Push result to client (winner/loser + coupon)

**Features:** Virtual threads for 100k+ connections, 10s reel timing

---

#### 4. Coupon Management API
**Controller:** `CouponController`  
**Endpoints:**

```
POST /api/v1/coupons/bulk-upload     # Upload CSV of coupons
POST /api/v1/coupons/{id}/issue      # Issue coupon to user
POST /api/v1/coupons/{id}/redeem     # Redeem coupon
GET  /api/v1/coupons/user/{userId}   # Get user's coupons
```

**Kafka Listener:**
```java
@Component
public class AllocationResultListener {
    @KafkaListener(topics = "allocation-results")
    public void onAllocationResult(AllocationResultEvent event) {
        if (event.isWinner()) {
            issueCouponToUser(event.getCouponId(), event.getUserId());
        }
    }
}
```

**Features:** Bulk upload (CSV), auto-issuance, expiration handling

---

#### 5. Admin Management API
**Controllers:** `CampaignController`, `GameController`, `BrandController`, `MetricsController`  
**Endpoints:**

```
# Campaign Management
GET    /api/v1/admin/campaigns
POST   /api/v1/admin/campaigns
PUT    /api/v1/admin/campaigns/{id}
DELETE /api/v1/admin/campaigns/{id}

# Game Management
GET    /api/v1/admin/games
POST   /api/v1/admin/games
PUT    /api/v1/admin/games/{id}
DELETE /api/v1/admin/games/{id}

# Game Lifecycle
POST   /api/v1/admin/games/{id}/start    # Load budgets to Redis
POST   /api/v1/admin/games/{id}/stop     # Cleanup & reconcile
POST   /api/v1/admin/games/{id}/pause

# Brand Management
GET    /api/v1/admin/brands
POST   /api/v1/admin/brands
PUT    /api/v1/admin/brands/{id}
DELETE /api/v1/admin/brands/{id}

# Metrics Dashboard
GET    /api/v1/admin/metrics/games/{id}
GET    /api/v1/admin/metrics/budget/{gameId}
```

**Features:** CRUD operations, game lifecycle, budget initialization, metrics

---

### Application Configuration

**application.yml:**
```yaml
server:
  port: 8080

spring:
  application:
    name: frolic-application
  threads:
    virtual:
      enabled: true
  
  # Database
  datasource:
    url: jdbc:postgresql://localhost:5432/frolic
    username: frolic
    password: frolic
    hikari:
      maximum-pool-size: 10
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  
  # Redis
  data:
    redis:
      host: localhost
      port: 6379
  
  # Kafka
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: frolic-consumer-group
      auto-offset-reset: earliest
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  
  # Liquibase
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml

# Frolic specific configuration
frolic:
  probability:
    slot-granularity-seconds: 5
    min-probability: 0.01
  websocket:
    reel-duration-seconds: 10
  kafka:
    topics:
      play-events: play-events
      allocation-results: allocation-results
      coupon-issued: coupon-issued
      game-lifecycle: game-lifecycle

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info
  metrics:
    export:
      prometheus:
        enabled: true
```

**VirtualThreadConfig.java:**
```java
@Configuration
public class VirtualThreadConfig {
    
    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor asyncTaskExecutor() {
        return new TaskExecutorAdapter(
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            protocolHandler.setExecutor(
                Executors.newVirtualThreadPerTaskExecutor()
            );
        };
    }
}
```

### Dependencies
```xml
<dependencies>
    <!-- Core module -->
    <dependency>
        <groupId>com.frolic</groupId>
        <artifactId>frolic-core</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring Boot WebSocket -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    
    <!-- Spring Boot Actuator -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- Apache POI (for Excel upload) -->
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
    </dependency>
</dependencies>
```

---

## Build Configuration

### Parent POM
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>

    <groupId>com.frolic</groupId>
    <artifactId>frolic</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>frolic-core</module>
        <module>frolic-services</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- Dependency versions -->
        <lombok.version>1.18.30</lombok.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <redisson.version>3.27.0</redisson.version>
        <resilience4j.version>2.2.0</resilience4j.version>
        <testcontainers.version>1.19.3</testcontainers.version>
    </properties>

    <dependencyManagement>
        <!-- Managed dependencies -->
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

---

## Testing Strategy

### Unit Tests
- JUnit 5 for all test cases
- Mockito for mocking dependencies
- AssertJ for fluent assertions
- Each module has its own test suite

### Integration Tests
```java
@SpringBootTest
@Testcontainers
class AllocationIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15-alpine");
    
    @Container
    static GenericContainer<?> redis = 
        new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    
    @Container
    static KafkaContainer kafka = 
        new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
        );
    
    @Test
    void shouldAllocateRewardCorrectly() {
        // Test allocation flow
    }
}
```

### Performance Tests
- JMeter or Gatling for load testing
- Simulate 100k+ concurrent plays
- Measure allocation latency (target: <100ms p99)
- Verify budget correctness under load

---

## Implementation Phases

### Phase 1: Foundation (Days 1-2)
- [ ] Create parent POM with modules
- [ ] Setup frolic-core structure
- [ ] Create common DTOs and enums
- [ ] Setup Liquibase with initial schema
- [ ] Create JPA entities and repositories

### Phase 2: Infrastructure (Days 3-4)
- [ ] Implement Redis configuration and stores
- [ ] Create Lua scripts for atomic operations
- [ ] Setup Kafka producers and consumers
- [ ] Implement monitoring and health checks

### Phase 3: Core Logic (Days 5-6)
- [ ] Implement probability engine
- [ ] Implement concurrency control engine
- [ ] Write unit tests for algorithms
- [ ] Test atomic operations

### Phase 4: Services (Days 7-10)
- [ ] Implement Play Ingestion Service
- [ ] Implement Reward Allocator Service
- [ ] Implement WebSocket Service
- [ ] Implement Coupon Service
- [ ] Implement Admin API Service

### Phase 5: Integration & Testing (Days 11-12)
- [ ] Integration tests with Testcontainers
- [ ] End-to-end flow testing
- [ ] Load testing
- [ ] Bug fixes and optimization

### Phase 6: Documentation (Day 13)
- [ ] API documentation (OpenAPI)
- [ ] Deployment guide
- [ ] Architecture diagrams
- [ ] Developer setup guide

---

## Deployment

### Docker Compose (Local Development)
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: frolic
      POSTGRES_USER: frolic
      POSTGRES_PASSWORD: frolic
    ports:
      - "5432:5432"
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
  
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
    ports:
      - "9092:9092"
  
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"
```

### Container Image
**Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY frolic-services/target/frolic-services-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Build & Run:**
```bash
# Build the project
mvn clean package

# Run locally
java -jar frolic-services/target/frolic-services-1.0-SNAPSHOT.jar

# Build Docker image
docker build -t frolic:latest .

# Run container
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/frolic \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  frolic:latest
```

---

## Success Criteria

### Functional
- ✅ Campaign and game lifecycle management
- ✅ High-concurrency play ingestion (100k+ QPS)
- ✅ Probabilistic reward allocation
- ✅ Atomic budget management (no overspend)
- ✅ 10-second reel UX via WebSocket
- ✅ Coupon issuance and redemption

### Non-Functional
- ✅ Ingestion latency < 50ms (p99)
- ✅ Allocation latency < 100ms (p99)
- ✅ WebSocket delivery < 20ms (p99)
- ✅ Zero budget overspend
- ✅ Smooth reward distribution
- ✅ Horizontal scalability

---

## Notes

- **No Authentication:** Demo project, no auth/authz
- **No API Gateway:** Services communicate directly
- **No Service Mesh:** In-app resilience patterns
- **Virtual Threads:** Used throughout for high concurrency
- **Jakarta Persistence:** Using Jakarta EE 10 spec

---

**Document Version:** 2.0 (Simplified)  
**Last Updated:** 2025-11-21  
**Status:** Ready for Implementation
