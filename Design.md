# Gamification System (Frolic) — Architecture & Design

This document contains a full architecture, data models, sequence diagram, probability algorithm, and an interview-ready explanation for the gamification system you described.

---

# 1. Overview

Goal: Run **Campaigns** (e.g., Friday Weekend) that contain multiple **Games**. Each Game has a start/end time and a budget of brand coupons. Users play games (with a ~10s jackpot reel UX) and may win coupons. Requirements:

* Avoid exhausting budget too early
* Distribute rewards smoothly over the game duration
* Survive extremely high concurrency (mass arrivals)
* Ensure fairness / prevent double allocation
* Fast user experience with 10s reel

High-level approach (summary):

* Decouple ingestion (user plays) from allocation using a durable message queue.
* Use a probabilistic allocation algorithm where `P = remainingBudget / remainingSlots` evaluated per play.
* Use an atomic decrement (Redis or DB) to reserve/confirm coupons to avoid overspend.
* Keep websocket servers light — they only maintain connections and display results.

---

# 2. Components

1. **Client (Mobile/Web)**

    * Opens STICKY websocket for fast real-time UX or uses HTTP + long-poll.
    * Sends `PlayRequest` with `userId`, `gameId`, `playId` when user clicks play.
    * Waits for jackpot reel; during 10s the app polls or listens for result.

2. **Edge / API Gateway**

    * TLS termination, rate-limiting, bot checks, auth.

3. **Play Ingestion Service (stateless)**

    * Accepts PlayRequests at very high QPS.
    * Immediately enqueues event to **Message Bus** (Kafka / Pulsar) and returns acknowledgment.
    * Emits a lightweight `queued` ack that the client shows an animation.

4. **Message Bus (Kafka/Pulsar)**

    * Durable, partitioned, ordered stream of play events.
    * Partitions by `gameId` (and optionally shard by `userId % N`) for parallelism.

5. **Reward Allocator Workers (stateless, scaled)**

    * Consume play events.
    * Evaluate probability and attempt to allocate using an **atomic budget store** (Redis Lua or DB transaction).
    * Write allocation result to **Result Store** (Redis hash or DB) keyed by `playId`.

6. **Atomic Budget Store**

    * Redis counters (or DynamoDB/Spanner) storing remaining coupons per game-brand pair.
    * Allocation performed with atomic `DECR` / compare-and-set via Lua script to guarantee correctness.

7. **Result Store**

    * Redis for low-latency lookups of play results: `result:{playId} => {winner:true/false, couponId?, brandId?, reason}`.
    * TTL set to e.g. 24 hours; long-term audit in event store.

8. **Websocket Gateway / Game Server (connection manager)**

    * Sticky sessions for the UI. Does *not* perform heavy allocation.
    * At the 10s reel expiration, queries Result Store and pushes result to client.

9. **Coupon Issuance Service**

    * On confirmed allocation, issues coupon codes (from preloaded coupon pools) and marks them consumed.
    * Logs to audit store.

10. **Monitoring & Metrics**

    * Track allocations per minute, remaining budget, queue lag, consumer lag, allocation latency.

11. **Admin UI**

    * Configure Campaigns, Games, Brands, Budgets, and upload coupon pools.

---

# 3. Component Interaction Diagram (textual)

Client -> Edge/API -> PlayIngestion -> MessageBus --> RewardAllocator -> AtomicBudgetStore

RewardAllocator -> ResultStore
WebsocketServer -> ResultStore -> Client (at 10s)
CouponIssuance reads ResultStore and marks coupon as consumed

---

# 4. Data models

## 4.1 Persistent DB (Postgres / MySQL)

### Campaigns

```
Campaign(
  campaign_id UUID PK,
  name TEXT,
  start_time TIMESTAMP,
  end_time TIMESTAMP,
  created_by UUID,
  metadata JSONB
)
```

### Games

```
Game(
  game_id UUID PK,
  campaign_id UUID FK,
  name TEXT,
  start_time TIMESTAMP,
  end_time TIMESTAMP,
  initial_budget INT, -- total coupons for the game (sum of brand budgets)
  config JSONB, -- e.g. slot-granularity, min_prob, constraints
  status ENUM
)
```

### Brands

```
Brand(
  brand_id UUID PK,
  name TEXT,
  total_coupons INT,
  coupon_pool_source TEXT
)
```

### GameBrandBudget

```
GameBrandBudget(
  id UUID PK,
  game_id UUID FK,
  brand_id UUID FK,
  initial_budget INT,
  coupon_template_id UUID,
  metadata JSONB
)
```

### Coupons (pre-generated)

```
Coupon(
  coupon_id UUID PK,
  brand_id UUID,
  game_id UUID,
  code TEXT,
  status ENUM('AVAILABLE','RESERVED','ISSUED','REDEEMED'),
  reserved_at TIMESTAMP,
  issued_to UUID,
  issued_at TIMESTAMP
)
```

## 4.2 Redis / Atomic store keys

* `budget:game:{gameId}:brand:{brandId}` => integer (remaining coupons)
* `slots:game:{gameId}` => integer (optional precomputed remaining slots)
* `result:{playId}` => JSON (winner boolean + coupon info)
* `play_status:{userId}:{gameId}` => to prevent duplicate plays per user (if required)

Redis Lua script will atomically check and decrement budgets.

## 4.3 Event Bus schema (PlayEvent)

```
PlayEvent {
  playId UUID,
  userId UUID,
  gameId UUID,
  clientTs TIMESTAMP,
  arrivalTs TIMESTAMP,
  meta JSON
}
```

---

# 5. End-to-end sequence (step-by-step)

1. **User clicks play** in client. Client generates `playId` and opens/uses existing WS.
2. Client sends `PlayRequest(playId,userId,gameId)` to ingestion endpoint.
3. **Edge** validates request, returns `202 PlayQueued` to client quickly.
4. **PlayIngestion** writes `PlayEvent` to Kafka partitioned by `gameId`.
5. **RewardAllocator workers** (many consumers) read PlayEvent.

    * For each event, worker computes probability (see Algorithm below).
    * Worker attempts atomic decrement on chosen brand's budget using Redis Lua.
    * If success, worker reserves a coupon and writes `{winner:true,couponId,..}` to `result:{playId}`.
    * If fail (budget 0) or probability check fails, writes `{winner:false}`.
6. At ~10s (when the client reel ends), **Websocket server** checks `result:{playId}`.

    * If result exists => push to client.
    * If not yet present (lag), WS server waits up to TTL (e.g., 2s) then either push loser or fall back.
7. If winner, **CouponIssuance** binds coupon to user and marks coupon as `ISSUED` permanently.
8. Audit logs store event and issuance for reconciliation.

---

# 6. Probability algorithm (clean)

Problem: Smoothly allocate `Y` remaining coupons over `T` remaining seconds (or slots), while handling stochastic arrivals.

### Definitions

* `remainingBudget` (RB): remaining coupons for the game or brand (integer)
* `now` = current time
* `t_end` = game end time
* `remainingSeconds` = max(1, floor(t_end - now))
* `slotGranularity` = slot duration in seconds (e.g., 1s or 5s)
* `remainingSlots` = ceil(remainingSeconds / slotGranularity)
* `minP` = optional floor probability (to ensure some small chance even when RB << slots)

### Per-play probability (base)

```
P_base = RB / remainingSlots
```

This is the expected number of coupons per slot. When `P_base <= 1`, use it as a probability.
When `P_base > 1`, expected winners per slot should be `floor(P_base)` plus fractional.

### Algorithm (pseudocode)

```python
# called per PlayEvent
def attempt_allocate(playEvent):
    RB = read_budget(gameId, brandId)  # atomic read
    remainingSlots = compute_remaining_slots(gameId)
    if RB <= 0 or remainingSlots <= 0:
        write_result(playId, winner=False)
        return

    P_base = RB / remainingSlots

    # determine expected winners this event
    if P_base < 1:
        if random() < P_base:
            success = atomic_decrement_if_positive(budget_key)
            if success:
                coupon = reserve_coupon(gameId, brandId)
                write_result(playId, winner=True, coupon=coupon)
                return
    else:
        # P_base >= 1 means on average more than 1 coupon per slot
        # decide how many winners to give per consumed play event
        k = floor(P_base)
        frac = P_base - k
        winners = k
        if random() < frac:
            winners += 1

        # Try to allocate up to 'winners' coupons atomically
        allocated = 0
        for i in range(winners):
            success = atomic_decrement_if_positive(budget_key)
            if success:
                allocated += 1
            else:
                break

        if allocated > 0:
            coupon = reserve_n_coupons(gameId, brandId, n=allocated)
            write_result(playId, winner=True, coupon=coupon, count=allocated)
            return

    # no allocation
    write_result(playId, winner=False)
```

### Atomic budget decrement (Redis Lua)

```
-- KEYS[1] = budget_key
-- ARGV[1] = n
local current = tonumber(redis.call('GET', KEYS[1]) or '0')
local n = tonumber(ARGV[1])
if current >= n then
  redis.call('DECRBY', KEYS[1], n)
  return 1
else
  return 0
end
```

### Notes

* Use sampled smoothing: do not always apply the allocation only on the *first* N plays in a slot; evaluate per event.
* If arrivals are sparse and RB >> remainingSlots, probability will be >1 → more winners per event. This accommodates low traffic.
* Recompute `remainingSlots` often (e.g., TTL 1s) to reflect time progression.

---

# 7. Ensuring smooth distribution when traffic is bursty

* **Queue-based smoothing**: spikes are flattened by Kafka; Reward Allocators process at steady rate.
* **Adaptive worker scaling**: autoscale consumers based on consumer lag.
* **Backoff / throttling**: ingestion can return `429` when queue backlog exceeds threshold.
* **Graceful degradation**: when lag too high, show client a degraded UX (animated message: "Prize pool updating...").

---

# 8. Handling concurrent hotpaths & safety

1. **Idempotency:** `playId` deduped by ingestion or at allocator (store `seen:playId`).
2. **Atomic counters:** Redis Lua or DB transactions for budget decrements.
3. **Coupon reservation:** Reserve coupons at decrement time; if issuance later fails, rollback with compensating credit.
4. **Fraud controls:** rate-limit per user, require human checks.
5. **Auditing:** write every allocation attempt to an append-only event store for reconciliation.

---

# 9. Failure modes & recovery

* *Consumer died before writing result*: consumer should write a tombstone loser result or another consumer must reprocess (use consumer groups with commit semantics).
* *Race on coupon issuance*: coupon store should be single source of truth; only issue coupons after atomic decrement and reservation.
* *Redis outage*: fallback to strongly consistent DB with higher latency.

---

# 10. Metrics to monitor

* Play events/sec
* Consumer lag (Kafka offset lag)
* Allocations/sec
* Remaining budget per game-brand
* Allocation latency (time from play to result stored)
* WS delivery latency (time from result stored to client push)
* Failed allocations

---

# 11. Interview-ready explanation (2-minute pitch + bullets)

**2-minute pitch:**

> We build a horizontally scalable gamification system by decoupling play ingestion from reward allocation. The client submits play events which are durable in a message bus. Scalable allocator workers consume events and make probabilistic allocation decisions based on remaining budget and remaining time slots. We use atomic budget counters (Redis Lua or DB CAS) to avoid overspending. To meet the 10s UX requirement, the websocket server only handles connections and pushes the final result after the allocator writes a low-latency result entry. This approach smooths spikes, ensures fairness, and allows low-latency UX while keeping heavy logic out of connection managers.

**Key bullets to emphasize:**

* Decoupling: ingestion vs allocation
* Probabilistic allocation: `P = RB / remainingSlots`
* Atomic budget control with Redis Lua
* Durable event bus to handle bursty traffic
* Websocket server kept light — no heavy state
* Autoscaling consumers and adaptive throttling
* Full audit trails for reconciliation

---

# 12. Variants & optimizations

* **Per-brand prioritization**: weight probabilities per brand to achieve marketing goals.
* **Soft-guarantees**: configure min and max daily/slot winners.
* **Bucket hybrid**: maintain a small per-slot reservation pool for the first few seconds to improve UX predictability.
* **Dynamic bucket sizing**: if traffic is extremely bursty, dynamically adapt slotGranularity.

---

# 13. Appendix: sample Redis / Kafka sizing notes

* Partition Kafka by `gameId % 50` or higher depending on concurrency.
* Keep Redis clusters with replication and persistence; use Lua scripts for atomic ops.
* Ensure consumer count >= partitions to scale parallel consumption.

---

# 14. Next steps I can do for you

* Draw a visual diagram (PNG/SVG)
* Provide sequence diagram in PlantUML
* Provide sample Node/Java pseudo-implementation for allocator + Redis Lua scripts
* Create a test harness to simulate bursty traffic and verify distribution

---

*End of document.*
