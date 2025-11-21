# Frolic Quick Start Guide

## 🌐 Web UIs Available

Once infrastructure is running, access these UIs:

- **Frolic API** 🚀 → http://localhost:8080 (Main application)
- **Redis UI** 🔴 → http://localhost:8081 (View Redis keys & budgets) 
  - connect to `host.docker.insternal` with database, username and passwor as `frolic`
- **Kafka UI** 📨 → http://localhost:8082 (Browse messages & topics)
- **PostgreSQL UI** 🐘 → http://localhost:8083 (Database admin - pgAdmin)
  - Login: `admin@frolic.com` / `admin`
  - Add server: Host=`postgres`, Port=`5432`, User=`frolic`, Password=`frolic`

## 1️⃣ One-Liner Setup

```bash
docker-compose up -d && mvn clean package -DskipTests && java -jar frolic-services/target/frolic-services-1.0-SNAPSHOT.jar
```

## 2️⃣ Test the System (Copy & Paste)

### Create Brand
```bash
curl -X POST http://localhost:8080/api/v1/admin/brands \
  -H "Content-Type: application/json" \
  -d '{"name":"Nike","description":"Sports brand","active":true}'
```
**Save the `id` from response**

### Create Campaign
```bash
curl -X POST http://localhost:8080/api/v1/admin/campaigns \
  -H "Content-Type: application/json" \
  -d '{"name":"Black Friday","status":"DRAFT","startDate":"2025-11-24T00:00:00Z","endDate":"2025-11-30T23:59:59Z"}'
```
**Save the `id` from response**

### Create Game (Replace {campaignId} and {brandId})
```bash
curl -X POST http://localhost:8080/api/v1/admin/games \
  -H "Content-Type: application/json" \
  -d '{
    "name":"Spin Wheel",
    "campaignId":"CAMPAIGN_ID_HERE",
    "status":"DRAFT",
    "startTime":"2025-11-21T18:00:00Z",
    "endTime":"2025-11-30T23:59:59Z",
    "probabilityType":"TIME_BASED",
    "slotGranularitySeconds":5,
    "brandBudgets":[{"brandId":"BRAND_ID_HERE","totalBudget":100}]
  }'
```
**Save the `id` from response**

### Start Game (Replace {gameId})
```bash
curl -X POST http://localhost:8080/api/v1/admin/games/GAME_ID_HERE/start
```

### Play! (Replace {gameId})
```bash
curl -X POST http://localhost:8080/api/v1/play \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-123","gameId":"GAME_ID_HERE"}'
```
**Save the `playId` from response**

### Check Result (Replace {playId}, wait 2-3 seconds)
```bash
curl http://localhost:8080/api/v1/play/PLAY_ID_HERE/result
```

### 🔌 WebSocket Real-Time Results (Alternative)

**JavaScript/Browser Example:**
```html
<!DOCTYPE html>
<html>
<head>
  <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
</head>
<body>
  <script>
    const playId = 'YOUR_PLAY_ID_HERE';
    
    // Connect to WebSocket
    const socket = new SockJS('http://localhost:8080/ws/game');
    const stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
      console.log('Connected: ' + frame);
      
      // Subscribe to result topic
      stompClient.subscribe('/topic/result/' + playId, function(result) {
        const data = JSON.parse(result.body);
        console.log('Result received:', data);
        alert(data.winner ? '🎉 Winner!' : '😢 Try again!');
      });
      
      // Send subscription message
      stompClient.send('/app/subscribe/' + playId, {}, '{}');
    });
  </script>
</body>
</html>
```

**Connection Details:**
- **WebSocket Endpoint**: `ws://localhost:8080/ws/game`
- **Protocol**: STOMP over SockJS
- **Subscribe to**: `/topic/result/{playId}`
- **Send message to**: `/app/subscribe/{playId}`

**Node.js Example:**
```javascript
npm install stompjs websocket

const Stomp = require('stompjs');
const WebSocket = require('websocket').w3cwebsocket;

global.WebSocket = WebSocket;

const playId = 'YOUR_PLAY_ID_HERE';
const client = Stomp.client('ws://localhost:8080/ws/game');

client.connect({}, () => {
  console.log('Connected to WebSocket');
  
  // Subscribe to results
  client.subscribe(`/topic/result/${playId}`, (message) => {
    console.log('Result:', JSON.parse(message.body));
  });
  
  // Trigger subscription
  client.send(`/app/subscribe/${playId}`, {}, '{}');
});
```

## 3️⃣ Verify Infrastructure

### Web UIs (Easiest Way!)
- **PostgreSQL UI** (pgAdmin): http://localhost:8083
  - Query `play_events` table
  - View `game_brand_budgets`
  - Check all tables and data

- **Redis UI**: http://localhost:8081 
  - View budgets: `budget:game:{gameId}:brand:{brandId}`
  - Check results: `result:{playId}`
  
- **Kafka UI**: http://localhost:8082
  - View `play-events` topic messages
  - Monitor consumer lag
  - Browse all topics

### Command Line
```bash
# Check health
curl http://localhost:8080/actuator/health

# Check Redis budget
docker exec -it frolic-redis redis-cli
GET budget:game:GAME_ID:brand:BRAND_ID

# Check Kafka messages
docker exec -it frolic-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic play-events \
  --from-beginning
```

## 4️⃣ Common Commands

```bash
# Stop infrastructure
docker-compose down

# Clean rebuild
mvn clean package -DskipTests

# View logs
docker-compose logs -f

# Check database
docker exec -it frolic-postgres psql -U frolic -d frolic
\dt
SELECT * FROM play_events ORDER BY created_at DESC LIMIT 10;
```

## 5️⃣ API Cheat Sheet

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/v1/admin/brands` | GET/POST | Manage brands |
| `/api/v1/admin/campaigns` | GET/POST | Manage campaigns |
| `/api/v1/admin/games` | GET/POST | Manage games |
| `/api/v1/admin/games/{id}/start` | POST | Start game |
| `/api/v1/admin/games/{id}/stop` | POST | Stop game |
| `/api/v1/play` | POST | Submit play |
| `/api/v1/play/{id}/result` | GET | Get result |
| `/actuator/health` | GET | Health check |

## 6️⃣ Troubleshooting

**Issue: Port already in use**
```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill -9
```

**Issue: Docker containers not starting**
```bash
docker-compose down -v
docker-compose up -d
```

**Issue: Build fails**
```bash
mvn clean install -U
```

**Issue: Game not active**
- Check game status: `GET /api/v1/admin/games/{id}`
- Ensure time window is valid
- Start the game: `POST /api/v1/admin/games/{id}/start`

## 7️⃣ Project Structure

```
frolic/
├── frolic-core/              → Core library (all business logic)
├── frolic-services/          → Single Spring Boot app
├── docker-compose.yml        → Infrastructure
├── README.md                 → Full documentation
├── API_TESTING_GUIDE.md      → Detailed testing
└── COMPLETION_SUMMARY.md     → Implementation status
```

## 8️⃣ Tech Stack at a Glance

- **Java 21** + Virtual Threads
- **Spring Boot 3.3.5**
- **PostgreSQL** → Database
- **Redis** → Cache + Atomic ops
- **Kafka** → Event streaming
- **WebSocket** → Real-time updates

## 🎯 Success Criteria

✅ Health endpoint returns UP
✅ Can create brands, campaigns, games
✅ Game start loads budgets to Redis
✅ Play requests return 202 Accepted
✅ Results show winners/losers
✅ Budget decrements atomically

---

**Need more details?** Check API_TESTING_GUIDE.md

**Happy Gaming! 🎮**
