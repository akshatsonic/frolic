# Frolic Gamification System

A horizontally scalable gamification platform for campaigns, games, and probabilistic reward allocations under massive concurrent traffic (100k+ QPS) using event-driven architecture.

## Architecture

### 2-Module Design

```
frolic/
├── frolic-core/          # Core library (JAR)
│   ├── common/          # DTOs, enums, utilities, exceptions
│   ├── domain/          # Domain models & business interfaces
│   ├── repository/      # JPA entities & repositories
│   ├── cache/           # Redis configuration & stores
│   ├── messaging/       # Kafka producers & consumers
│   ├── engine/          # Probability & concurrency engines
│   └── monitoring/      # Metrics & health checks
│
└── frolic-services/     # Single Spring Boot application (JAR)
    ├── controller/      # REST & WebSocket controllers
    ├── service/         # Business services
    ├── consumer/        # Kafka consumers
    └── config/          # Application configuration
```

## Technology Stack

- **Java 21** with Virtual Threads
- **Spring Boot 3.3.5**
- **PostgreSQL** (database)
- **Redis** (cache + atomic operations)
- **Apache Kafka** (event streaming)
- **Liquibase** (database migrations)
- **Spring WebSocket** (real-time updates)
- **Micrometer + Prometheus** (metrics)

## Key Features

### 1. Play Ingestion API
- REST endpoint: `POST /api/v1/play`
- Virtual threads for high concurrency
- Request validation
- Kafka event publishing

### 2. Reward Allocation (Background)
- Kafka consumer processing play events
- Probabilistic reward distribution
- Atomic budget management via Redis Lua scripts
- Idempotency handling

### 3. Admin Management API
- Campaign CRUD operations
- Game CRUD operations
- Brand management
- Budget initialization

## Prerequisites

- Java 21
- Maven 3.8+
- Docker & Docker Compose

## Quick Start

### 1. Start Infrastructure

```bash
# Start PostgreSQL, Redis, Kafka, and UI tools
docker-compose up -d

# Verify services are running
docker ps
```

**UI Tools Available:**
- **Application**: http://localhost:8080 (Frolic API)
- **Redis UI**: http://localhost:8081 (Redis Commander)
- **Kafka UI**: http://localhost:8082 (Kafka UI)
- **PostgreSQL UI**: http://localhost:8083 (pgAdmin) - Login: admin@frolic.com / admin

### 2. Build Project

```bash
# Build all modules
mvn clean package

# Skip tests for faster build
mvn clean package -DskipTests
```

### 3. Run Application

```bash
# Run from frolic-services directory
java -jar frolic-services/target/frolic-services-1.0-SNAPSHOT.jar

# Or using Maven
cd frolic-services
mvn spring-boot:run
```

Application will start on `http://localhost:8080`

### 4. Verify Health

```bash
curl http://localhost:8080/actuator/health
```

## API Endpoints

### Play API

```bash
# Submit a play
curl -X POST http://localhost:8080/api/v1/play \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "gameId": "game456"
  }'

# Get play result
curl http://localhost:8080/api/v1/play/{playId}/result
```

### Admin API

```bash
# Create campaign
POST /api/v1/admin/campaigns

# Create game
POST /api/v1/admin/games

# Start game (loads budgets to Redis)
POST /api/v1/admin/games/{id}/start

# Stop game
POST /api/v1/admin/games/{id}/stop
```

## Core Algorithm

### Probability Engine

```
P_base = remainingBudget / remainingSlots

if (P_base < 1):
    Probabilistic: if random() < P_base then allocate 1 coupon
else:
    Deterministic + fractional: 
        allocate floor(P_base) + (random() < fractional ? 1 : 0) coupons
```

### Atomic Budget Management

- Redis Lua script ensures no race conditions
- Budget key: `budget:game:{gameId}:brand:{brandId}`
- Atomic DECRBY with check-and-set logic
- Idempotency keys: `play_processed:{playId}`

## Configuration

### Application Properties

Located at: `frolic-services/src/main/resources/application.yml`

Key configurations:
- Database connection
- Redis connection
- Kafka bootstrap servers
- Slot granularity for probability calculation
- WebSocket reel duration

### Environment Variables

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/frolic
SPRING_DATASOURCE_USERNAME=frolic
SPRING_DATASOURCE_PASSWORD=frolic

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

## Development

### Module Structure

**frolic-core**: Contains all shared business logic, infrastructure, and domain models. This is a library module that gets imported by frolic-services.

**frolic-services**: Single executable Spring Boot application that runs all services:
- Play ingestion REST API
- Kafka consumer for reward allocation
- WebSocket server for real-time updates
- Coupon management
- Admin APIs

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific module
cd frolic-core
mvn test
```

### Code Style

- Java 21 features enabled
- Lombok for reducing boilerplate
- SLF4J for logging
- Builder pattern for DTOs

## Monitoring

### Web UI Tools

**pgAdmin** (Port 8083): `http://localhost:8083`
- Full-featured PostgreSQL administration tool
- Browse tables and data with SQL queries
- Visual query builder and data editor
- View table schemas and relationships
- Monitor database performance
- **Login**: admin@frolic.com / admin
- **First time**: Add server (Host: postgres, Port: 5432, User: frolic, Password: frolic)

**Redis Commander** (Port 8081): `http://localhost:8081`
- View all Redis keys in a web interface
- Inspect budget values: `budget:game:{gameId}:brand:{brandId}`
- Check play results: `result:{playId}`
- Monitor idempotency keys: `play_processed:{playId}`
- Real-time key monitoring and updates

**Kafka UI** (Port 8082): `http://localhost:8082`
- Browse all Kafka topics (`play-events`, `allocation-results`, etc.)
- View messages with JSON formatting
- Monitor consumer groups and lag
- Track partitions and offsets
- Real-time message streaming

### Actuator Endpoints

- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Prometheus: `http://localhost:8080/actuator/prometheus`

### Key Metrics

- Play ingestion latency
- Allocation processing time
- Budget remaining per game/brand
- Winner/loser ratio
- Kafka consumer lag

### Command Line Tools

**Redis CLI:**
```bash
docker exec -it frolic-redis redis-cli
GET budget:game:{gameId}:brand:{brandId}
KEYS *
```

**Kafka CLI:**
```bash
# List topics
docker exec -it frolic-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Consume messages
docker exec -it frolic-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic play-events \
  --from-beginning
```

**PostgreSQL:**
```bash
docker exec -it frolic-postgres psql -U frolic -d frolic
\dt
SELECT * FROM play_events ORDER BY created_at DESC LIMIT 10;
```

## Database Schema

### Tables

- `campaigns` - Campaign metadata
- `games` - Game configuration
- `brands` - Brand information
- `game_brand_budgets` - Budget allocation per game-brand
- `coupons` - Coupon pool
- `play_events` - Play event audit log
- `users` - User information

### Migrations

Managed by Liquibase. Changelog location:
```
frolic-core/src/main/resources/db/changelog/db.changelog-master.xml
```

## Kafka Topics

- `play-events` - Play event stream (partitioned by gameId)
- `allocation-results` - Allocation audit trail
- `coupon-issued` - Coupon issuance events
- `game-lifecycle` - Game start/stop events

## Redis Keys

```
budget:game:{gameId}:brand:{brandId}  # Remaining coupons
result:{playId}                        # Play result
slots:game:{gameId}                    # Cached slots count
play_processed:{playId}                # Idempotency flag
game_config:{gameId}                   # Game configuration cache
```

## Performance Targets

- Ingestion latency: < 50ms (p99)
- Allocation latency: < 100ms (p99)
- WebSocket delivery: < 20ms (p99)
- Zero budget overspend (atomicity guarantee)
- Horizontal scalability

## Troubleshooting

### Application won't start

1. Verify Docker services are running:
   ```bash
   docker ps
   ```

2. Check database connectivity:
   ```bash
   psql -h localhost -U frolic -d frolic
   ```

3. Check Kafka:
   ```bash
   docker logs frolic-kafka
   ```

### No rewards being allocated

1. Check if game is active and within time window
2. Verify budget is loaded in Redis:
   ```bash
   redis-cli
   GET budget:game:{gameId}:brand:{brandId}
   ```
3. Check Kafka consumer logs

### High latency

1. Monitor virtual thread usage
2. Check Redis connection pool
3. Verify Kafka consumer lag
4. Review database query performance

## 🎮 Demo Scripts

### Interactive Demo (Recommended!) ⭐

User-friendly demo with custom configuration:

```bash
pip3 install requests
python3 interactive_demo.py
```

**Features:**
- Configure campaigns, durations, games, brands, and budgets interactively
- Uses 100 users for simulation
- All campaigns/games start immediately
- 2-minute wait for probability stabilization
- 2-minute simulation (regardless of campaign duration) for quick probability testing
- Shows both winners and losers in real-time
- Budget comparison report (initial vs final)
- Perfect for presentations and testing

**See:** [INTERACTIVE_DEMO_GUIDE.md](INTERACTIVE_DEMO_GUIDE.md)

---

### Automated Demo Scripts

For pre-configured demos with realistic data:

```bash
# Install Python dependencies
pip3 install requests

# Run complete automated demo
./run_full_demo.sh
```

**What it does:**
- Creates 100 users, 10 brands, 20 campaigns, 100+ games
- All games use TIME_BASED probability (win rate increases over time)
- Some games end in 1-2 minutes for fast visualization
- Simulates realistic user traffic patterns
- Shows real-time winners and statistics

**Individual scripts:**
```bash
python3 setup_demo_data.py      # Create demo data
python3 simulate_gameplay.py     # Simulate users playing
python3 check_demo_results.py    # View statistics
```

**Documentation:**
- **[INTERACTIVE_DEMO_GUIDE.md](INTERACTIVE_DEMO_GUIDE.md)** - Interactive demo guide
- **[QUICK_START.md](QUICK_START.md)** - Quick start guide with manual testing
- **[DEMO_SCRIPTS_README.md](DEMO_SCRIPTS_README.md)** - Complete demo automation guide
- **[DEMO_SCRIPTS_SUMMARY.md](DEMO_SCRIPTS_SUMMARY.md)** - Quick reference card

## License

This is a demo project for educational purposes.

## Contact

For questions or issues, please refer to the IMPLEMENTATION_PLAN.md document for detailed architecture and design decisions.
