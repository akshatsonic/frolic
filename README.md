# Frolic Gamification System

A scalable gamification platform for campaigns, games, and probabilistic reward allocations using event-driven architecture with Java 21, Spring Boot, PostgreSQL, Redis, and Kafka.

## Overview

Frolic is a demo project showcasing:
- **Probabilistic reward distribution** with atomic budget management
- **Event-driven architecture** using Kafka for play event processing
- **High concurrency** with Java 21 Virtual Threads
- **Real-time updates** via WebSocket
- **Zero budget overspend** guaranteed through Redis Lua scripts

## Prerequisites

- **Java 21**
- **Maven 3.8+**
- **Docker & Docker Compose**
- **Python 3** (for running simulation scripts)

## Getting Started

### 1. Start Infrastructure Services

Start PostgreSQL, Redis, and Kafka using Docker Compose:

```bash
docker-compose up -d
```

Verify all services are running:

```bash
docker ps
```

### 2. Build the Project

Build both modules (frolic-core and frolic-services):

```bash
mvn clean install
```

Or skip tests for faster build:

```bash
mvn clean install -DskipTests
```

### 3. Start the Application

Run the Spring Boot application JAR:

```bash
java -jar frolic-services/target/frolic-services-1.0-SNAPSHOT.jar
```

The application will start on **http://localhost:8080**

Verify the application is running:

```bash
curl http://localhost:8080/actuator/health
```

### 4. Run the Interactive Demo

Install Python dependencies:

```bash
pip3 install requests
pip3 install redis
```

Run the interactive simulation script:
> This script is designed only to simulate the environment for 2 minutes to get the idea of how the rewards are getting distributed over time

```bash
python3 interactive_demo.py
```

The script will:
- Guide you through configuring campaigns, games, brands, and budgets
- Create 100 simulated users
- Run gameplay simulation for 2 minutes
- Display real-time winners and losers
- Show final budget comparison report

## What's Running

Once started, you'll have access to:

- **Frolic API**: http://localhost:8080
- **Redis Commander**: http://localhost:8081 (view Redis keys)
- **Kafka UI**: http://localhost:8082 (view Kafka topics)
- **pgAdmin**: http://localhost:8083 (PostgreSQL admin - login: admin@frolic.com / admin)

## Project Structure

```
frolic/
├── frolic-core/          # Core library (JAR)
│   ├── engine/          # Probability & concurrency engines
│   ├── cache/           # Redis Lua scripts & stores
│   ├── messaging/       # Kafka producers & consumers
│   └── repository/      # JPA entities & repositories
│
└── frolic-services/     # Spring Boot application (JAR)
    ├── controller/      # REST & WebSocket endpoints
    ├── service/         # Business logic
    └── consumer/        # Kafka event consumers
```

## Technology Stack

- **Java 21** with Virtual Threads
- **Spring Boot 3.3.5**
- **PostgreSQL** (database)
- **Redis** (cache + atomic operations)
- **Apache Kafka** (event streaming)
- **Spring WebSocket** (real-time updates)

## API Documentation

### User Management

```bash
# Create user
POST /api/v1/admin/users
{"email": "user@example.com", "name": "John Doe", "phone": "1234567890"}

# Update user
PUT /api/v1/admin/users/{id}
{"email": "user@example.com", "name": "John Smith", "phone": "1234567890"}

# Get user
GET /api/v1/admin/users/{id}

# Delete user
DELETE /api/v1/admin/users/{id}
```

### Campaign Management

```bash
# Create campaign
POST /api/v1/admin/campaigns
{"name": "Summer Sale", "description": "Summer campaign", "startDate": "2024-06-01T00:00:00", "endDate": "2024-08-31T23:59:59"}

# Update campaign
PUT /api/v1/admin/campaigns/{id}
{"name": "Summer Mega Sale", "description": "Updated campaign"}

# Get campaign
GET /api/v1/admin/campaigns/{id}

# Delete campaign
DELETE /api/v1/admin/campaigns/{id}

# Activate campaign
POST /api/v1/admin/campaigns/{id}/activate

# End campaign
POST /api/v1/admin/campaigns/{id}/end
```

### Game Management

```bash
# Create game
POST /api/v1/admin/games
{"name": "Spin Wheel", "campaignId": "{campaignId}", "startTime": "2024-06-01T10:00:00", "endTime": "2024-06-01T22:00:00", "probabilityType": "UNIFORM", "slotGranularitySeconds": 300, "brandBudgets": [{"brandId": "{brandId}", "budget": 1000}]}

# Update game
PUT /api/v1/admin/games/{id}
{"name": "Lucky Wheel", "slotGranularitySeconds": 600}

# Get game
GET /api/v1/admin/games/{id}

# Delete game
DELETE /api/v1/admin/games/{id}

# Start game (loads budgets to Redis)
POST /api/v1/admin/games/{id}/start

# Stop game
POST /api/v1/admin/games/{id}/stop
```

### Brand Management

```bash
# Create brand
POST /api/v1/admin/brands
{"name": "Brand A", "description": "Premium brand", "logoUrl": "https://example.com/logo.png"}

# Update brand
PUT /api/v1/admin/brands/{id}
{"name": "Brand A Pro", "description": "Updated description"}

# Get brand
GET /api/v1/admin/brands/{id}

# Delete brand
DELETE /api/v1/admin/brands/{id}
```

### Play Game

```bash
# Submit play request
POST /api/v1/play
{"userId": "{userId}", "gameId": "{gameId}"}

# Response: {"playId": "abc123", "status": "PROCESSING"}

# Get play result
GET /api/v1/play/{playId}/result

# Response: {"playId": "abc123", "status": "COMPLETED", "winner": true, "coupons": [...]}
```

## License

This is a demo project for educational purposes.
