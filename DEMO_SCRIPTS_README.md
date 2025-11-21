# 🎮 Frolic Demo Scripts

Automated scripts to quickly populate and test the Frolic gamification platform with realistic data.

## 📋 Overview

Three scripts work together to create a complete demo environment:

1. **`setup_demo_data.py`** - Generates users, brands, campaigns, and games
2. **`simulate_gameplay.py`** - Simulates users playing active games
3. **`run_full_demo.sh`** - Master script that runs both in sequence

## 🚀 Quick Start

### Prerequisites

```bash
# Install Python 3 (if not already installed)
# macOS: brew install python3
# Ubuntu: sudo apt install python3

# Install required library
pip3 install requests
```

### Run Complete Demo

```bash
# Make sure Frolic server is running first
docker-compose up -d
mvn clean package -DskipTests
java -jar frolic-services/target/frolic-services-1.0-SNAPSHOT.jar

# In another terminal, run the demo
./run_full_demo.sh
```

## 📜 Script Details

### 1. setup_demo_data.py

**Purpose:** Creates comprehensive demo data for the platform

**⚠️ IMPORTANT: Automatic Cleanup**
This script automatically deletes ALL existing data before creating new demo data:
- Deletes all games, campaigns, brands, and users
- Waits 3 seconds before cleanup (Ctrl+C to cancel)
- Ensures fresh demo data every time

**What it creates:**
- **100 Users** with realistic names and contact info
- **10 Brands** (Nike, Adidas, Starbucks, McDonald's, etc.)
- **20 Campaigns** with varied time ranges:
  - 5 past campaigns (already ended)
  - 10 current/active campaigns
  - 5 future campaigns
- **100+ Games** (5-6 per campaign):
  - **All games use TIME_BASED probability type** (win probability increases over time)
  - Multiple brands per game with random budgets (50-500 coupons)
  - **20% of games end within 1-2 minutes** ⚡ for fast visualization
  - Auto-starts games that should be active now

**Output:**
- Colorful console output showing creation progress
- `demo_data_ids.json` file with all created entity IDs
- Summary statistics

**Run standalone:**
```bash
python3 setup_demo_data.py
```

**Sample Output:**
```
============================================================
            Creating 100 Users                              
============================================================

✓ Created 10/100 users
✓ Created 20/100 users
...
✓ Successfully created 100 users

============================================================
            Creating 10 Brands                              
============================================================

✓ Created brand: Nike (ID: 1a2b3c...)
✓ Created brand: Adidas (ID: 4d5e6f...)
...
```

---

### 2. simulate_gameplay.py

**Purpose:** Simulates realistic user gameplay patterns

**Features:**
- Multi-threaded concurrent gameplay
- Real-time winner/loser notifications
- Comprehensive statistics tracking
- Multiple simulation modes

**Simulation Modes:**

#### Mode 1: Wave Simulation (Default)
- 5 waves of 20 users each
- Realistic traffic pattern
- Staggers user arrivals
- Pauses between waves
- **Best for demos**

#### Mode 2: Continuous Simulation
- Runs for 5 minutes
- Maintains ~10 concurrent users
- Users come and go naturally
- **Best for sustained load testing**

#### Mode 3: Stress Test
- Fires 1000 concurrent play requests
- Tests system under heavy load
- Validates atomicity and thread safety
- **Best for performance validation**

#### Mode 4: Quick Test
- Single wave of 10 users
- Fastest way to see results
- **Best for quick validation**

**Statistics Tracked:**
- Total plays submitted
- Successful vs failed plays
- Winners vs losers
- Win rate percentage
- Plays per game distribution

**Run standalone:**
```bash
python3 simulate_gameplay.py
```

**Sample Output:**
```
🎮 FROLIC GAMEPLAY SIMULATOR 🎮

✓ Server is running and healthy
✓ Loaded 100 users
✓ Found 15 active games

Active Games:
  1. Spin the Wheel - Black Friday Bonanza...
  2. Lucky Draw - Cyber Monday Madness...
  ...

Select Simulation Mode:
  1. Wave Simulation (5 waves of 20 users) - Realistic
  2. Continuous (5 min with 10 concurrent users) - Long running
  3. Stress Test (1000 rapid plays) - High load
  4. Quick Test (1 wave of 10 users) - Fast demo

Enter mode (1-4) [default: 1]: 

━━━ Wave 1/5 ━━━
🎉 WIN | User: 1a2b3c4d... | Game: Spin the Wheel... (2 coupons)
😢 LOSE | User: 5e6f7g8h... | Game: Lucky Draw...
...
```

---

### 3. run_full_demo.sh

**Purpose:** Master automation script

**What it does:**
1. Checks if Python 3 is installed
2. Installs `requests` library if missing
3. Verifies Frolic server is running
4. Runs `setup_demo_data.py`
5. Waits for games to initialize
6. Runs `simulate_gameplay.py`
7. Shows links to web UIs

**Run:**
```bash
./run_full_demo.sh
```

## 📊 Monitoring the Demo

### Web UIs

While simulation runs, monitor in real-time:

| Service | URL | What to Check |
|---------|-----|---------------|
| **PostgreSQL** | http://localhost:8083 | `play_events`, `coupons`, `game_brand_budgets` tables |
| **Redis** | http://localhost:8081 | Budget keys: `budget:game:{gameId}:brand:{brandId}` |
| **Kafka** | http://localhost:8082 | `play-events`, `allocation-results` topics |

### Command Line

```bash
# Check active games
curl http://localhost:8080/api/v1/admin/games | jq '.[] | select(.status=="ACTIVE")'

# Check recent play events
docker exec -it frolic-postgres psql -U frolic -d frolic \
  -c "SELECT * FROM play_events ORDER BY created_at DESC LIMIT 10;"

# Check Redis budget for a game
docker exec -it frolic-redis redis-cli
GET budget:game:{GAME_ID}:brand:{BRAND_ID}

# Monitor Kafka play-events topic
docker exec -it frolic-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic play-events \
  --from-beginning
```

## 🎯 Use Cases

### 1. Quick Demo for Stakeholders
```bash
./run_full_demo.sh
# Select Mode 4 (Quick Test)
# Show winners/losers in real-time
# Open PostgreSQL UI to show data
```

### 2. Load Testing
```bash
python3 setup_demo_data.py
python3 simulate_gameplay.py
# Select Mode 3 (Stress Test)
# Monitor system performance
```

### 3. Budget Validation
```bash
# After running simulation, verify budgets
docker exec -it frolic-postgres psql -U frolic -d frolic

-- Check total coupons issued vs budget
SELECT 
  g.name as game_name,
  b.name as brand_name,
  gb.total_budget,
  COUNT(c.id) as coupons_issued,
  gb.total_budget - COUNT(c.id) as remaining
FROM games g
JOIN game_brand_budgets gb ON g.id = gb.game_id
JOIN brands b ON gb.brand_id = b.id
LEFT JOIN coupons c ON c.game_id = g.id AND c.brand_id = b.id
GROUP BY g.name, b.name, gb.total_budget;
```

### 4. Time-Based Probability Testing
```bash
# Watch games that end in 1-2 minutes
python3 setup_demo_data.py
# Note which games are marked as "⚡ Quick game"
python3 simulate_gameplay.py
# Select Mode 2 (Continuous)
# Observe win rate changes as game approaches end time
```

## 🔧 Customization

### Modify User Count
```python
# In setup_demo_data.py, line ~380
users = create_users(100)  # Change to desired count
```

### Modify Campaign Count
```python
# In setup_demo_data.py, line ~382
campaigns = create_campaigns(20)  # Change to desired count
```

### Modify Games per Campaign
```python
# In setup_demo_data.py, line ~250
num_games = random.randint(5, 6)  # Change range
```

### Change Quick Game Percentage
```python
# In setup_demo_data.py, line ~260
is_quick_game = random.random() < 0.2  # 20% are quick, change to 0.5 for 50%
```

### Modify Simulation Intensity
```python
# In simulate_gameplay.py, modify mode parameters:
wave_simulation(user_ids, game_ids, game_map, 
                waves=10,           # More waves
                users_per_wave=50)  # More users per wave
```

## 🐛 Troubleshooting

### Error: "Cannot connect to server"
```bash
# Check if server is running
curl http://localhost:8080/actuator/health

# Start infrastructure if needed
docker-compose up -d

# Start application
java -jar frolic-services/target/frolic-services-1.0-SNAPSHOT.jar
```

### Error: "No active games found"
```bash
# Check game status
curl http://localhost:8080/api/v1/admin/games | jq '.[].status'

# Manually start a game
curl -X POST http://localhost:8080/api/v1/admin/games/{GAME_ID}/start
```

### Error: "demo_data_ids.json not found"
```bash
# Run setup first
python3 setup_demo_data.py
```

### Error: "No module named 'requests'"
```bash
# Install requests library
pip3 install requests
```

### Games ending too fast / too slow
- Adjust the quick game logic in `setup_demo_data.py` lines 260-267
- Modify the time ranges for game creation

## 📈 Performance Expectations

With default settings:

| Metric | Expected Value |
|--------|---------------|
| Setup time | 30-60 seconds |
| Entities created | 100 users, 10 brands, 20 campaigns, ~100 games |
| Active games | 10-20 (depends on time ranges) |
| Simulation duration (Mode 1) | 2-3 minutes |
| Plays per second (Mode 3) | 100-200 |
| Win rate | 10-30% (depends on budgets and slots) |

## 🎨 Color-Coded Output

- 🟢 **Green** - Success messages, winners
- 🟡 **Yellow** - Warnings, losers (sample)
- 🔵 **Blue** - Info messages, headers
- 🔴 **Red** - Errors, failures
- ⚡ **Lightning** - Quick games (1-2 min duration)

## 📝 Output Files

| File | Purpose | When Created |
|------|---------|--------------|
| `demo_data_ids.json` | Entity IDs for simulation | After setup_demo_data.py |

## 🔒 Safety Features

- ✅ Health check before execution
- ✅ Graceful error handling
- ✅ Thread-safe statistics
- ✅ Keyboard interrupt support (Ctrl+C)
- ✅ No data deletion (only creates)

## 🌟 Best Practices

1. **Always check server health first** - Scripts verify but good to know
2. **Monitor Redis during stress tests** - Watch budget decrements in real-time
3. **Use PostgreSQL UI for deep dives** - Query play_events for patterns
4. **Run cleanup between demos** - Drop and recreate database if needed
5. **Adjust game times for timezone** - Scripts use UTC

## 🎬 Demo Script Example

```bash
# Complete walkthrough for a presentation

# 1. Start infrastructure
docker-compose up -d

# 2. Build and start application
mvn clean package -DskipTests
java -jar frolic-services/target/frolic-services-1.0-SNAPSHOT.jar &

# 3. Wait for startup
sleep 10

# 4. Run full demo
./run_full_demo.sh

# Select Mode 1 (Wave Simulation)
# Watch real-time winners/losers

# 5. Open monitoring UIs
open http://localhost:8083  # PostgreSQL
open http://localhost:8081  # Redis
open http://localhost:8082  # Kafka

# 6. Query results
docker exec -it frolic-postgres psql -U frolic -d frolic \
  -c "SELECT status, COUNT(*) FROM play_events GROUP BY status;"

# 7. Check budget usage
docker exec -it frolic-redis redis-cli KEYS "budget:*"
```

---

**Happy Gaming! 🎮**

For questions or issues, check the main [README.md](README.md) and [API_TESTING_GUIDE.md](API_TESTING_GUIDE.md).
