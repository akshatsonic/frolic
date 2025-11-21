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

## 🚀 Demo Options

### Option 1: Interactive Demo (Recommended for Presentations!) ⭐

**User-friendly with custom configuration:**
```bash
python3 interactive_demo.py
```

**You configure:**
- Number of campaigns
- Campaign durations (in minutes)
- Games per campaign
- Brands per game (1-5)
- Budget per brand (min 1)
- Uses 100 users for simulation
- All campaigns/games start NOW
- 2-minute wait + 2-minute simulation (4 min total)
- Shows both wins and losses
- Auto-simulation and budget reports

**See:** [INTERACTIVE_DEMO_GUIDE.md](INTERACTIVE_DEMO_GUIDE.md)

---

### Option 2: Automated Demo (Pre-configured)

**Full demo with 100 users & realistic gameplay:**

**Prerequisites:** Python 3 with `requests` library
```bash
pip3 install requests
```

**Run the complete demo:**
```bash
./run_full_demo.sh
```

This script automatically:
- 🧹 **Cleans up ALL existing data first** (games, campaigns, brands, users)
- ✅ Creates 100 users, 10 brands, 20 campaigns, 100+ games
- ✅ **All games use TIME_BASED probability** (win rate increases over time)
- ✅ Starts games that should be active now
- ✅ Some games end in 1-2 minutes for fast visualization
- ✅ Simulates realistic user traffic patterns
- ✅ Shows real-time winners and statistics

**Note:** The script waits 3 seconds before cleanup - press Ctrl+C to cancel if needed.

**OR run scripts individually:**

```bash
# Step 1: Setup demo data
python3 setup_demo_data.py

# Step 2: Simulate gameplay (choose from multiple modes)
python3 simulate_gameplay.py
```

**Simulation Modes:**
1. **Wave Simulation** - 5 waves of 20 users (realistic traffic)
2. **Continuous** - 5 minutes with 10 concurrent users
3. **Stress Test** - 1000 rapid concurrent plays
4. **Quick Test** - 1 wave of 10 users (fastest demo)

## 2️⃣ Test the System (Copy & Paste)

### Create User
```bash
curl -X POST http://localhost:8080/api/v1/admin/users \
  -H "Content-Type: application/json" \
  -d '{"email":"john.doe@example.com","name":"John Doe","phoneNumber":"+1234567890","active":true}'
```
**Save the `id` from response**

### Get All Users
```bash
curl http://localhost:8080/api/v1/admin/users
```

### Get User by ID (Replace {userId})
```bash
curl http://localhost:8080/api/v1/admin/users/USER_ID_HERE
```

### Get User by Email
```bash
curl http://localhost:8080/api/v1/admin/users/email/john.doe@example.com
```

### Update User (Replace {userId})
```bash
curl -X PUT http://localhost:8080/api/v1/admin/users/USER_ID_HERE \
  -H "Content-Type: application/json" \
  -d '{"email":"john.doe@example.com","name":"John Doe Updated","phoneNumber":"+1234567890","active":true}'
```

### Delete User (Replace {userId})
```bash
curl -X DELETE http://localhost:8080/api/v1/admin/users/USER_ID_HERE
```

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
| `/api/v1/admin/users` | GET/POST | Manage users |
| `/api/v1/admin/users/{id}` | GET/PUT/DELETE | User by ID |
| `/api/v1/admin/users/email/{email}` | GET | User by email |
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
✅ Can create and manage users
✅ Can create brands, campaigns, games
✅ Game start loads budgets to Redis
✅ Play requests return 202 Accepted
✅ Results show winners/losers
✅ Budget decrements atomically

---

## 🤖 Automated Demo Scripts

For a complete automated demo with realistic data and gameplay simulation, check out:

📖 **[DEMO_SCRIPTS_README.md](DEMO_SCRIPTS_README.md)** - Comprehensive guide to demo automation scripts

**Quick run:**
```bash
pip3 install requests
./run_full_demo.sh
```

---

**Need more details?** 
- **API Testing**: Check [API_TESTING_GUIDE.md](API_TESTING_GUIDE.md)
- **Demo Automation**: Check [DEMO_SCRIPTS_README.md](DEMO_SCRIPTS_README.md)

**Happy Gaming! 🎮**
