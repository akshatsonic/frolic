# Gamification System (Frolic) — High-Level Design (HLD) & Low-Level Design (LLD)

This document provides heavy focus on:
- **Probability Engine** (reward distribution logic)
- **Concurrency Control Engine** (atomic budget management, high load safety)

---

# 1. SYSTEM OVERVIEW
A gamification platform that powers campaigns, games, and probabilistic reward allocations under massive traffic.

## Core design goals
- Handle **hundreds of thousands of concurrent players**.
- Prevent **budget exhaustion** due to concurrency.
- Ensure **smooth reward distribution over time**.
- Provide **10s reel latency masking** for UI.
- Guarantee **fairness, auditability, idempotency**.

---

# 2. HIGH-LEVEL ARCHITECTURE (HLD)

```
           ┌─────────────────────┐
           │     Mobile App      │
           │   (WebSocket + API) │
           └──────────┬──────────┘
                      │ Play Request
                      ▼
           ┌─────────────────────┐
           │   API Gateway       │
           │ Auth + Rate Limit   │
           └──────────┬──────────┘
                      │
                      ▼
           ┌─────────────────────┐
           │ Play Ingestion Svc  │
           │  → Publish to Kafka │
           └──────────┬──────────┘
                      │ Kafka Topic: play-events
                      ▼
       ┌──────────────────────────────────────┐
       │    Reward Allocation Service         │
       │  (Consumers + Probability Engine)    │
       │   - Concurrency Control (Redis)      │
       │   - Probability Calculator           │
       └──────────┬───────────────────────────┘
                  │ Writes result
                  ▼
           ┌─────────────────────┐
           │  Redis Result Store |
           |(with TTL of 20s)    │
           └──────────┬──────────┘
                      │ Pull
                      ▼
           ┌─────────────────────┐
           │ WebSocket Service   │
           │ 10s Reel Completion │
           └──────────┬──────────┘
                      │ If Winner
                      ▼
           ┌─────────────────────┐
           │ Coupon Service      │
           │ Issue + Lock codes  │
           └─────────────────────┘
```

---

# 3. HLD — KEY COMPONENTS

## 3.1 Play Ingestion Service
- Validates request.
- Creates `playId`.
- Publishes event to Kafka topic partitioned by `gameId`.
- Ensures high throughput.

## 3.2 Reward Allocation Service
- Kafka consumers process each play.
- Uses a **probability engine** to determine win/lose.
- Uses **concurrency control engine** with atomic Redis Lua scripts.
- Writes results to Redis.
- Completely stateless; horizontally scalable.

## 3.3 Concurrency Control Engine (HLD)
Responsible for:
- Atomic budget decrement.
- Preventing duplicate rewards.
- Idempotent execution.
- Avoiding race conditions between consumers.

**Design:**
- Redis per-game-brand counters.
- Lua scripts for multi-key atomicity.
- Optional optimistic locking fallback.

## 3.4 Probability Engine (HLD)
Responsible for:
- Ensuring rewards last through game duration.
- Handling low or high traffic variations.
- Ensuring fairness while remaining random.

**Formula base:**
```
P = remaining_budget / remaining_time_slots
```

## 3.5 WebSocket Engine
- Manages realtime connections.
- Pushes results exactly after 10 seconds.
- Reads results from Redis.

---

# 4. LOW-LEVEL DESIGN (LLD)

# 4.1 Detailed Component Responsibilities

## Play Ingestion Service (LLD)
- API: `POST /play` → returns `{playId, status:QUEUED}`.
- Validates if user allowed to play.
- Writes a lightweight record to DB (optional).
- Publishes **PlayEvent** to Kafka.
- Does NOT compute rewards.

**Data Structure:**
```
PlayEvent {
  playId: UUID,
  userId: UUID,
  gameId: UUID,
  timestamp: long,
  device: {...}
}
```


---

# 4.2 Reward Allocation Service (LLD)

### Internal Steps
1. Consume PlayEvent.
2. Load cache values:
   - remaining budget
   - game end time
3. Compute probability.
4. Perform `atomic_decrement()`.
5. If success → allocate coupon.
6. Write result to Redis.
7. Publish audit.


### Threading Model
- No shared state among consumers.
- Redis is the SINGLE source of truth for budget.

---

# 4.3 Concurrency Control Engine (LLD)

## Redis Key Structure
```
budget:game:{gameId}:brand:{brandId} = remaining integer
```
```
result:{playId} = JSON
```

## Atomic Decrement Logic (LUASCRIPT)
```
local current = tonumber(redis.call('GET', KEYS[1]) or '0')
local n = tonumber(ARGV[1])
if current >= n then
    redis.call('DECRBY', KEYS[1], n)
    return 1
end
return 0
```

Guarantees:
- No two consumers can decrement beyond 0.
- No race conditions.
- Entire operation is atomic.

### Idempotency Handling
- Redis SETNX: `play_processed:{playId}`.
- If exists: return cached result.
- Prevents duplicate consumption and retries.

### High Concurrency Safety
- Consumers scaled to number of partitions.
- Redis cluster with sharding based on `{gameId}`.
- Lua scripts ensure atomicity even under 100k+ concurrent events.

---

# 4.4 Probability Engine (LLD)

### Step 1: Compute Remaining Slots
```
remainingSeconds = max(1, gameEnd - now)
slotSizeSec = 5  # or 1 (this is the time slot bucket)
remainingSlots = ceil(remainingSeconds / slotSizeSec)
```

### Step 2: Compute P
```
P_base = remainingBudget / remainingSlots
```

Two cases:

## Case 1: `P_base < 1`
Probability of winning is < 1:
```
if random() < P_base:
    attempt atomic decrement
```

**Ensures few winners when budget << slots left.**

## Case 2: `P_base >= 1`
On average more than one coupon can/should be given per slot.

Compute deterministic + probabilistic part:
```
fixedWinners = floor(P_base)
extra = 1 if random() < (P_base - fixedWinners) else 0
winners = fixedWinners + extra
```

Then:
```
attempt atomic decrement by `winners`
```

This makes reward distribution robust even when traffic is low.

### Protection Against Over-allocation
After computing winners:
```
winners = min(winners, remainingBudget)
```

---

# 4.5 Sequence Diagram (LLD)

Textual flow:
```
User → API Gateway → Play Ingestion → Kafka → Reward Allocator → Redis(result)
User(WebSocket) ← WebSocket Service ← Redis(result)
```

Detailed:
```
User: play
API: queue OK
PlayService → Kafka: PlayEvent
Allocator ← Kafka
Allocator → Redis: atomic budget decrement
Allocator → Redis: store result
WebSocket waits 10s
WebSocket → Redis: fetch result
WebSocket → User: winner/loser
CouponService issues coupon
```

---

# 5. DATABASE TABLES (LLD)

#### 1. campaigns

```
CREATE TABLE campaigns (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

#### 2. games

```
CREATE TABLE games (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL REFERENCES campaigns(id),
    name VARCHAR(255) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    total_budget INT NOT NULL,
    probability_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

#### 3. brands

```
CREATE TABLE brands (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    logo_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

#### 4. game_brand_budgets

```
CREATE TABLE game_brand_budgets (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL REFERENCES games(id),
    brand_id BIGINT NOT NULL REFERENCES brands(id),
    total_coupons INT NOT NULL,
    remaining_coupons INT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(game_id, brand_id)
);
```

#### 5. coupons

```
CREATE TABLE coupons (
    id BIGSERIAL PRIMARY KEY,
    brand_id BIGINT NOT NULL REFERENCES brands(id),
    code VARCHAR(255) NOT NULL UNIQUE,
    is_used BOOLEAN DEFAULT FALSE,
    game_id BIGINT REFERENCES games(id),
    issued_to BIGINT,
    issued_at TIMESTAMPTZ,
    used_at TIMESTAMPTZ,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

#### 6. play_events

```
CREATE TABLE play_events (
    id BIGSERIAL PRIMARY KEY,
    play_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL REFERENCES games(id),
    status VARCHAR(50) NOT NULL,
    reward_coupon_id BIGINT REFERENCES coupons(id),
    request_time TIMESTAMPTZ NOT NULL,
    processed_time TIMESTAMPTZ,
    latency_ms INT,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

#### 7. users

```
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(255) UNIQUE,
    name VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

---

# 6. NON-FUNCTIONAL REQUIREMENTS

### Scalability
- Kafka partitions = 20–50 per active game.
- Reward allocators autoscale horizontally.

### Reliability
- At-least-once Kafka processing + idempotency keys.

### Latency Goals
- Ingestion < 50ms
- Allocation < 100ms p99
- WebSocket delivery < 20ms

### Fairness
- Strict atomic counters.
- Universal probability engine irrespective of traffic.

---

# 7. MONITORING & ALERTING

### Key Metrics
- Kafka consumer lag
- Redis CPU + slowlog
- Budget drift (expected vs remaining)
- Winners per minute

