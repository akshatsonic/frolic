# 🎮 Demo Scripts - Quick Reference

## 📁 Files Created

| File | Purpose | Type |
|------|---------|------|
| `run_full_demo.sh` | Master script - runs setup + simulation | Shell Script |
| `setup_demo_data.py` | Creates 100 users, 10 brands, 20 campaigns, 100+ games | Python |
| `simulate_gameplay.py` | Simulates realistic user gameplay | Python |
| `check_demo_results.py` | View demo statistics and results | Python |
| `demo_data_ids.json` | Generated entity IDs (auto-created) | JSON |
| `DEMO_SCRIPTS_README.md` | Comprehensive documentation | Markdown |

## 🚀 Quick Commands

### Complete Demo (Automated)
```bash
./run_full_demo.sh
```

### Step-by-Step
```bash
# 1. Setup data (creates entities)
python3 setup_demo_data.py

# 2. Simulate gameplay
python3 simulate_gameplay.py

# 3. Check results
python3 check_demo_results.py
```

## 📊 What Gets Created

### setup_demo_data.py Creates:
- ✅ **100 Users** with realistic names
- ✅ **10 Brands** (Nike, Adidas, Starbucks, etc.)
- ✅ **20 Campaigns** (past, current, future)
- ✅ **100+ Games** (5-6 per campaign)
- ✅ **20% quick games** (end in 1-2 minutes) ⚡

### simulate_gameplay.py Does:
- ✅ Simulates users playing games
- ✅ Shows real-time winners/losers
- ✅ 4 simulation modes available
- ✅ Tracks comprehensive statistics

### check_demo_results.py Shows:
- ✅ Total entities created
- ✅ Active vs draft vs stopped games
- ✅ System health status
- ✅ Quick lookup commands

## 🎯 Simulation Modes

| Mode | Description | Best For |
|------|-------------|----------|
| **1. Wave** | 5 waves × 20 users | **Demos** ⭐ |
| **2. Continuous** | 5 min, 10 concurrent users | Long-running tests |
| **3. Stress** | 1000 concurrent plays | Load testing |
| **4. Quick** | 1 wave × 10 users | Fast validation |

## 🌐 Monitoring

| Service | URL | Purpose |
|---------|-----|---------|
| PostgreSQL | http://localhost:8083 | View tables & data |
| Redis | http://localhost:8081 | Check budgets |
| Kafka | http://localhost:8082 | Browse messages |

## 💡 Pro Tips

### Watch Games End in Real-Time
```bash
# Quick games end in 1-2 minutes!
python3 setup_demo_data.py  # Note ⚡ quick games
python3 simulate_gameplay.py  # Choose Mode 2 (Continuous)
# Watch win rates change as games approach end time
```

### Monitor Budget in Real-Time
```bash
# In another terminal while simulation runs:
watch -n 1 'docker exec frolic-redis redis-cli KEYS "budget:*" | head -10'
```

### Track Winners Live
```bash
# PostgreSQL query while simulation runs:
watch -n 2 'docker exec frolic-postgres psql -U frolic -d frolic -c "SELECT status, winner, COUNT(*) FROM play_events GROUP BY status, winner;"'
```

### Check Kafka Flow
```bash
# Watch play-events in real-time:
docker exec -it frolic-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic play-events
```

## 🎬 Demo Flow (5 Minutes)

```bash
# 1. Start everything (1 min)
docker-compose up -d && mvn clean package -DskipTests
java -jar frolic-services/target/frolic-services-1.0-SNAPSHOT.jar &
sleep 10

# 2. Run automated demo (3 min)
./run_full_demo.sh
# Select Mode 4 (Quick Test)

# 3. Show results (1 min)
python3 check_demo_results.py

# 4. Open UIs
open http://localhost:8083  # PostgreSQL
open http://localhost:8081  # Redis  
open http://localhost:8082  # Kafka
```

## 📈 Expected Output

### Setup Phase
```
Creating 100 Users
✓ Created 10/100 users
✓ Created 20/100 users
...
✓ Successfully created 100 users

Creating 10 Brands  
✓ Created brand: Nike (ID: 1a2b3c...)
...

Creating 20 Campaigns
✓ Created campaign: Black Friday Bonanza #1
...

Creating Games (5-6 per campaign)
ℹ ⚡ Quick game: Spin Wheel - Black Friday (ends in 1 min)
✓ Created & Started: Spin Wheel...
...
```

### Simulation Phase
```
━━━ Wave 1/5 ━━━
🎉 WIN | User: 1a2b3c4d... | Game: Spin Wheel... (2 coupons)
🎉 WIN | User: 5e6f7g8h... | Game: Lucky Draw... (1 coupon)
😢 LOSE | User: 9i0j1k2l... | Game: Prize Picker...
...

📊 SIMULATION STATISTICS
Total Plays: 156
Successful: 153
Failed: 3
Winners: 42 🎉
Losers: 111 😢
Win Rate: 27.45%
```

## 🐛 Common Issues

| Issue | Solution |
|-------|----------|
| Server not running | `java -jar frolic-services/target/*.jar` |
| No active games | Games may have ended. Re-run `setup_demo_data.py` |
| Python errors | `pip3 install requests` |
| Port conflicts | Check docker-compose, kill port 8080 processes |

## 🎓 Learning Path

1. **First Time**: Run `./run_full_demo.sh` → Mode 4 (Quick)
2. **Explore**: Run `check_demo_results.py`, open UIs
3. **Deep Dive**: Try Mode 1 (Wave), watch PostgreSQL
4. **Stress Test**: Mode 3, monitor Redis budget decrements
5. **Custom**: Modify scripts, adjust counts/times

## 📚 Related Documentation

- **[QUICK_START.md](QUICK_START.md)** - Manual API testing
- **[DEMO_SCRIPTS_README.md](DEMO_SCRIPTS_README.md)** - Full documentation
- **[API_TESTING_GUIDE.md](API_TESTING_GUIDE.md)** - API details
- **[README.md](README.md)** - Project overview

---

**🎮 Ready to play? Run `./run_full_demo.sh` now! 🎮**
