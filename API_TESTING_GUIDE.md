# Frolic API Testing Guide

Complete step-by-step guide to test the Frolic gamification system.

## Prerequisites

1. **Start Infrastructure**
   ```bash
   docker-compose up -d
   ```

2. **Run Application**
   ```bash
   mvn clean package -DskipTests
   java -jar frolic-services/target/frolic-services-1.0-SNAPSHOT.jar
   ```

3. **Verify Health**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

---

## Step 1: Create a Brand

```bash
curl -X POST http://localhost:8080/api/v1/admin/brands \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Nike",
    "description": "Sports brand",
    "logoUrl": "https://example.com/nike-logo.png",
    "active": true
  }'
```

**Response:**
```json
{
  "id": "brand-123-abc",
  "name": "Nike",
  "description": "Sports brand",
  "logoUrl": "https://example.com/nike-logo.png",
  "active": true,
  "createdAt": "2025-11-21T17:30:00Z",
  "updatedAt": "2025-11-21T17:30:00Z"
}
```

**Save the `brand ID` for later steps.**

---

## Step 2: Create a Campaign

```bash
curl -X POST http://localhost:8080/api/v1/admin/campaigns \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Black Friday 2025",
    "description": "Black Friday promotional campaign",
    "status": "DRAFT",
    "startDate": "2025-11-24T00:00:00Z",
    "endDate": "2025-11-30T23:59:59Z"
  }'
```

**Response:**
```json
{
  "id": "campaign-456-def",
  "name": "Black Friday 2025",
  "description": "Black Friday promotional campaign",
  "status": "DRAFT",
  "startDate": "2025-11-24T00:00:00Z",
  "endDate": "2025-11-30T23:59:59Z",
  "createdAt": "2025-11-21T17:31:00Z",
  "updatedAt": "2025-11-21T17:31:00Z"
}
```

**Save the `campaign ID`.**

---

## Step 3: Create a Game

Replace `{campaignId}` and `{brandId}` with actual values from previous steps.

```bash
curl -X POST http://localhost:8080/api/v1/admin/games \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Spin the Wheel",
    "campaignId": "campaign-456-def",
    "status": "DRAFT",
    "startTime": "2025-11-21T18:00:00Z",
    "endTime": "2025-11-21T23:59:59Z",
    "probabilityType": "TIME_BASED",
    "slotGranularitySeconds": 5,
    "brandBudgets": [
      {
        "brandId": "brand-123-abc",
        "totalBudget": 100
      }
    ]
  }'
```

**Response:**
```json
{
  "id": "game-789-ghi",
  "name": "Spin the Wheel",
  "campaignId": "campaign-456-def",
  "status": "DRAFT",
  "startTime": "2025-11-21T18:00:00Z",
  "endTime": "2025-11-21T23:59:59Z",
  "probabilityType": "TIME_BASED",
  "slotGranularitySeconds": 5,
  "brandBudgets": [
    {
      "id": "budget-999-xyz",
      "gameId": "game-789-ghi",
      "brandId": "brand-123-abc",
      "totalBudget": 100,
      "allocatedBudget": 0,
      "remainingBudget": 100
    }
  ],
  "createdAt": "2025-11-21T17:32:00Z",
  "updatedAt": "2025-11-21T17:32:00Z"
}
```

**Save the `game ID`.**

---

## Step 4: Start the Game

This loads the budget into Redis and makes the game active.

```bash
curl -X POST http://localhost:8080/api/v1/admin/games/game-789-ghi/start
```

**Response:**
```json
{
  "id": "game-789-ghi",
  "name": "Spin the Wheel",
  "status": "ACTIVE",
  ...
}
```

Check logs - you should see:
```
INFO - Started game: id=game-789-ghi, budgets loaded to Redis
```

---

## Step 5: Submit Play Requests

Now users can play the game!

```bash
# Play 1
curl -X POST http://localhost:8080/api/v1/play \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-001",
    "gameId": "game-789-ghi",
    "metadata": {"source": "mobile"}
  }'

# Play 2
curl -X POST http://localhost:8080/api/v1/play \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-002",
    "gameId": "game-789-ghi"
  }'

# Play 3
curl -X POST http://localhost:8080/api/v1/play \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-003",
    "gameId": "game-789-ghi"
  }'
```

**Response:**
```json
{
  "playId": "play-abc-123",
  "gameId": "game-789-ghi",
  "userId": "user-001",
  "status": "QUEUED",
  "winner": null,
  "message": "Play submitted successfully. Result will be available in 10 seconds."
}
```

**Save the `playId`.**

---

## Step 6: Check Result

Wait ~2-3 seconds (Kafka processing time), then check the result:

```bash
curl http://localhost:8080/api/v1/play/play-abc-123/result
```

**Winner Response:**
```json
{
  "playId": "play-abc-123",
  "gameId": "game-789-ghi",
  "userId": "user-001",
  "status": "WINNER",
  "winner": true,
  "couponCode": "COUPON-play-abc-123",
  "brandName": "Brand-brand-123-abc",
  "message": "Congratulations! You won 1 coupon(s)!"
}
```

**Loser Response:**
```json
{
  "playId": "play-abc-124",
  "gameId": "game-789-ghi",
  "userId": "user-002",
  "status": "LOSER",
  "winner": false,
  "message": "Better luck next time!"
}
```

---

## Step 7: Verify Redis Budget

### Option 1: Using Redis UI (Recommended)

Open **http://localhost:8081** in your browser:

1. Navigate to the Redis keys view
2. Search for: `budget:game:game-789-ghi:brand:brand-123-abc`
3. View the remaining budget value
4. Refresh to see updates in real-time

### Option 2: Using Redis CLI

```bash
# Connect to Redis
docker exec -it frolic-redis redis-cli

# Check budget
GET budget:game:game-789-ghi:brand:brand-123-abc

# Should show remaining budget (e.g., 99 if 1 winner)

# List all budget keys
KEYS budget:*

# Monitor all commands (real-time)
MONITOR
```

---

## Additional API Tests

### Get All Games
```bash
curl http://localhost:8080/api/v1/admin/games
```

### Get Game by ID
```bash
curl http://localhost:8080/api/v1/admin/games/game-789-ghi
```

### Pause Game
```bash
curl -X POST http://localhost:8080/api/v1/admin/games/game-789-ghi/pause
```

### Resume Game
```bash
curl -X POST http://localhost:8080/api/v1/admin/games/game-789-ghi/resume
```

### Stop Game
```bash
curl -X POST http://localhost:8080/api/v1/admin/games/game-789-ghi/stop
```

### Get All Campaigns
```bash
curl http://localhost:8080/api/v1/admin/campaigns
```

### Get All Brands
```bash
curl http://localhost:8080/api/v1/admin/brands
```

### Filter Active Brands
```bash
curl "http://localhost:8080/api/v1/admin/brands?activeOnly=true"
```

---

## WebSocket Testing

### Using JavaScript (Browser Console)

```javascript
// Connect to WebSocket
const socket = new SockJS('http://localhost:8080/ws/game');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // Subscribe to result topic
    const playId = 'play-abc-123';
    stompClient.subscribe('/topic/result/' + playId, function(message) {
        const result = JSON.parse(message.body);
        console.log('Received result:', result);
    });
    
    // Send subscribe message
    stompClient.send('/app/subscribe/' + playId, {}, JSON.stringify({}));
});
```

---

## Monitoring

### Actuator Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

### Check Kafka Topics

#### Option 1: Using Kafka UI (Recommended)

Open **http://localhost:8082** in your browser:

**Topics View:**
1. Click on "Topics" in the sidebar
2. You'll see all topics: `play-events`, `allocation-results`, `coupon-issued`, `game-lifecycle`
3. Click on `play-events` to view messages
4. Browse messages with filtering and search
5. View partition details and consumer groups

**Consumer Groups:**
1. Click on "Consumer Groups"
2. View `reward-allocator-group` lag and status
3. Monitor processing performance

**Messages:**
- View message keys (gameId)
- Inspect message payloads (JSON)
- See message timestamps
- Track partitions and offsets

#### Option 2: Using Command Line

```bash
# List topics
docker exec -it frolic-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Consume play-events
docker exec -it frolic-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic play-events \
  --from-beginning

# View consumer group details
docker exec -it frolic-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group reward-allocator-group
```

### Check PostgreSQL

#### Option 1: Using pgAdmin UI (Recommended)

Open **http://localhost:8083** in your browser:

**First Time Setup:**
1. Login: `admin@frolic.com` / `admin`
2. Add Server:
   - Name: `Frolic Database`
   - Host: `postgres`
   - Port: `5432`
   - Database: `frolic`
   - Username: `frolic`
   - Password: `frolic`

**Browse Data:**
1. Expand: Servers → Frolic Database → Databases → frolic → Schemas → public → Tables
2. Right-click table → "View/Edit Data" → "All Rows"

**Run Queries:**
```sql
-- View recent plays
SELECT * FROM play_events 
ORDER BY created_at DESC 
LIMIT 10;

-- Check budgets
SELECT * FROM game_brand_budgets;

-- Winner count per game
SELECT game_id, COUNT(*) as winners 
FROM play_events 
WHERE winner = true 
GROUP BY game_id;
```

#### Option 2: Using Command Line

```bash
# Connect to database
docker exec -it frolic-postgres psql -U frolic -d frolic

# Check tables
\dt

# Query play events
SELECT * FROM play_events ORDER BY created_at DESC LIMIT 10;

# Check brand budgets
SELECT * FROM game_brand_budgets;
```

---

## Load Testing

### Using Apache Bench

```bash
# 1000 requests, 50 concurrent
ab -n 1000 -c 50 -p play.json -T application/json \
  http://localhost:8080/api/v1/play
```

**play.json:**
```json
{
  "userId": "load-test-user",
  "gameId": "game-789-ghi"
}
```

---

## Troubleshooting

### Issue: "Game is not active"
- Verify game status: `curl http://localhost:8080/api/v1/admin/games/game-789-ghi`
- Start the game: `curl -X POST http://localhost:8080/api/v1/admin/games/game-789-ghi/start`

### Issue: "Game is not currently running"
- Check time window in game configuration
- Ensure current time is between `startTime` and `endTime`

### Issue: No winners
- Check budget in Redis: `GET budget:game:{gameId}:brand:{brandId}`
- Verify budget was initialized when game started
- Check application logs for allocation details

### Issue: Result not available
- Wait 2-3 seconds for Kafka processing
- Check Kafka consumer logs
- Verify Kafka is running: `docker ps | grep kafka`

---

## Success Criteria

✅ **Functional:**
- Campaigns, games, and brands can be created
- Games can be started/stopped
- Play requests are accepted
- Rewards are allocated probabilistically
- Results are stored in Redis
- No budget overspend

✅ **Non-Functional:**
- Ingestion responds within 50ms
- Allocation completes within 100ms
- Virtual threads handle high concurrency
- Atomic budget operations prevent race conditions

---

**Happy Testing! 🎉**
