# Frolic Services & Ports Reference

## 🌐 Web Interfaces

| Service | URL | Port | Purpose |
|---------|-----|------|---------|
| **Frolic API** | http://localhost:8080 | 8080 | Main application REST API |
| **Redis UI** | http://localhost:8081 | 8081 | Redis Commander - View Redis keys |
| **Kafka UI** | http://localhost:8082 | 8082 | Kafka UI - Browse topics & messages |
| **PostgreSQL UI** | http://localhost:8083 | 8083 | pgAdmin - Database administration |

## 🔧 Infrastructure Services

| Service | Host | Port | Purpose |
|---------|------|------|---------|
| **PostgreSQL** | localhost | 5432 | Primary database |
| **Redis** | localhost | 6379 | Cache & atomic operations |
| **Kafka** | localhost | 9092 | Message broker |
| **Zookeeper** | localhost | 2181 | Kafka coordination |

## 📊 Container Names

```bash
docker ps
```

| Container Name | Image | Purpose |
|----------------|-------|---------|
| `frolic-postgres` | postgres:15-alpine | Database |
| `frolic-postgres-ui` | dpage/pgadmin4 | PostgreSQL Web UI |
| `frolic-redis` | redis:7-alpine | Cache |
| `frolic-redis-ui` | rediscommander/redis-commander | Redis Web UI |
| `frolic-kafka` | confluentinc/cp-kafka:7.5.0 | Message broker |
| `frolic-kafka-ui` | provectuslabs/kafka-ui | Kafka Web UI |
| `frolic-zookeeper` | confluentinc/cp-zookeeper:7.5.0 | Kafka coordination |

## 🚀 Quick Access

### Start All Services
```bash
docker-compose up -d
```

### Access Web UIs
```bash
# Open all UIs in browser
open http://localhost:8080/actuator/health  # API Health
open http://localhost:8081                  # Redis UI
open http://localhost:8082                  # Kafka UI
open http://localhost:8083                  # PostgreSQL UI (pgAdmin)
```

### Check All Services
```bash
curl http://localhost:8080/actuator/health
docker ps
docker-compose logs -f
```

### Stop All Services
```bash
docker-compose down
```

## 🔍 What to Check in Each UI

### PostgreSQL UI - pgAdmin (http://localhost:8083)

**First Time Setup:**
1. Open http://localhost:8083
2. Login with:
   - Email: `admin@frolic.com`
   - Password: `admin`
3. Click "Add New Server"
4. **General tab**: Name = `Frolic Database`
5. **Connection tab**:
   - Host: `postgres`
   - Port: `5432`
   - Database: `frolic`
   - Username: `frolic`
   - Password: `frolic`
6. Click "Save"

**What to Check:**

**Tables:**
- `campaigns` - All campaigns created
- `games` - All games with status
- `brands` - All brands
- `game_brand_budgets` - Budget allocations per game/brand
- `play_events` - All plays (audit trail)
- `coupons` - Coupon pool
- `users` - User information

**Useful Queries:**
```sql
-- Recent plays
SELECT * FROM play_events 
ORDER BY created_at DESC 
LIMIT 20;

-- Winner statistics
SELECT game_id, COUNT(*) as winner_count 
FROM play_events 
WHERE winner = true 
GROUP BY game_id;

-- Budget status
SELECT g.name, b.name as brand, gbb.total_budget, 
       gbb.allocated_budget, gbb.remaining_budget
FROM game_brand_budgets gbb
JOIN games g ON g.id = gbb.game_id
JOIN brands b ON b.id = gbb.brand_id;

-- Active games
SELECT * FROM games 
WHERE status = 'ACTIVE';
```

**How to Use:**
1. Expand server → Database → frolic → Schemas → public → Tables
2. Right-click table → "View/Edit Data" → "All Rows"
3. Use "Query Tool" for custom SQL
4. View ERD (Entity Relationship Diagram) under "ERD For Database"

### Redis UI (http://localhost:8081)

**Budget Keys:**
```
budget:game:{gameId}:brand:{brandId}
```
- Shows remaining coupons for each brand in a game
- Updates in real-time as plays are processed
- Should decrement after winner allocations

**Result Keys:**
```
result:{playId}
```
- Contains play result (winner/loser)
- Includes coupon details for winners
- TTL: 1 hour

**Idempotency Keys:**
```
play_processed:{playId}
```
- Prevents duplicate processing
- TTL: 24 hours

**Game Config:**
```
game_config:{gameId}
```
- Cached game configuration

**How to Use:**
1. Open http://localhost:8081
2. Browse keys on the left sidebar
3. Click any key to view its value
4. Refresh to see real-time updates
5. Use search bar to find specific keys

### Kafka UI (http://localhost:8082)

**Topics to Monitor:**

1. **play-events**
   - All play requests from users
   - Partitioned by gameId
   - Check message keys and payloads

2. **allocation-results**
   - Audit trail of allocations
   - Winner/loser outcomes
   - Budget decrements

3. **coupon-issued**
   - Coupon issuance events
   - Link to user accounts

4. **game-lifecycle**
   - Game start/stop events

**Consumer Groups:**
- `reward-allocator-group` - Processes play events
  - Check lag (should be near 0)
  - Monitor processing rate

**How to Use:**
1. Open http://localhost:8082
2. Click "Topics" to see all topics
3. Click any topic to browse messages
4. Use "Consumer Groups" to check lag
5. View partitions and offsets

## 📈 Monitoring Tips

### Check if System is Healthy

1. **API Health**
   ```bash
   curl http://localhost:8080/actuator/health
   # Should return: {"status":"UP"}
   ```

2. **Redis Connection**
   - Open http://localhost:8081
   - Should see Redis keys listed

3. **Kafka Topics**
   - Open http://localhost:8082
   - Should see 4 topics created

4. **Database**
   ```bash
   docker exec -it frolic-postgres psql -U frolic -d frolic -c "SELECT COUNT(*) FROM play_events;"
   ```

### Debug Issues

**No messages in Kafka?**
- Check if game is started: `POST /api/v1/admin/games/{id}/start`
- Verify plays are being submitted: `POST /api/v1/play`
- Look at Kafka UI producer metrics

**Budget not decreasing?**
- Check Redis UI for budget key
- Verify game is ACTIVE
- Check Kafka consumer lag (should be low)
- View application logs

**No results showing?**
- Wait 2-3 seconds for Kafka processing
- Check Redis UI for result key
- Verify Kafka consumer is running
- Check consumer group lag in Kafka UI

## 🎯 Development Workflow

1. **Start infrastructure**: `docker-compose up -d`
2. **Check UIs are accessible**:
   - Redis UI: http://localhost:8081
   - Kafka UI: http://localhost:8082
3. **Run application**: `mvn spring-boot:run -pl frolic-services`
4. **Create game via API**
5. **Start game** (loads budget to Redis)
6. **Monitor in Redis UI**: Watch budget key
7. **Submit plays**
8. **Check Kafka UI**: See play-events messages
9. **Monitor Redis UI**: Watch budget decrement
10. **Verify results**: Check result keys

## 🔐 Credentials

All services use default/demo credentials:

- **PostgreSQL**: 
  - Username: `frolic`
  - Password: `frolic`
  - Database: `frolic`

- **pgAdmin UI**:
  - Email: `admin@frolic.com`
  - Password: `admin`

- **Redis**: No authentication (local dev)
- **Kafka**: No authentication (local dev)
- **Redis UI & Kafka UI**: No authentication required

## 🌟 Pro Tips

1. **Keep UIs Open**: Monitor Redis and Kafka in separate browser tabs while testing
2. **Use Search**: Both UIs have search features to find specific keys/messages
3. **Watch Real-Time**: Redis UI auto-refreshes, Kafka UI shows live messages
4. **Filter Messages**: Kafka UI supports filtering by time, partition, offset
5. **Export Data**: Both UIs support exporting data for analysis

---

**Quick Links:**
- 🚀 [Frolic API](http://localhost:8080)
- 🔴 [Redis UI](http://localhost:8081)
- 📨 [Kafka UI](http://localhost:8082)
- 🐘 [PostgreSQL UI](http://localhost:8083)
- ❤️ [Health Check](http://localhost:8080/actuator/health)
