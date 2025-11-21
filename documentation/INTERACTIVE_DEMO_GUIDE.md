# 🎮 Interactive Demo Guide

## Overview

The **Interactive Demo** is a user-friendly, guided demonstration that lets you customize:
- Number of campaigns
- Campaign durations
- Games per campaign
- Budget allocations
- Automatic simulation and reporting

## 📋 Prerequisites

1. **Python 3.8+** installed
2. **Python dependencies** installed:
   ```bash
   pip install -r requirements.txt
   ```
3. **Frolic services running** (backend, PostgreSQL, Redis, Kafka)
4. **Redis accessible** at `localhost:6379` (for budget tracking)

## 🚀 Quick Start

```bash
# Install dependencies (first time only)
pip install -r requirements.txt

# Run the demo
python3 interactive_demo.py
```

## 📝 Step-by-Step Walkthrough

### Step 1: Server Health Check
The script automatically verifies the Frolic server is running.

### Step 2: Data Cleanup
Automatically removes all existing data (with 2-second warning).

### Step 3: Configuration (Interactive Input)

You'll be asked:

1. **How many campaigns?** (1-10)
   ```
   How many campaigns to create? (1-10): 3
   ```

2. **For each campaign:**
   
   **Campaign #1 Configuration:**
   ```
   Campaign duration in minutes (1-60): 5
   Number of games (1-10): 6
   Number of brands per game (1-5): 3
   Budget per brand per game (min 1): 100
   ```
   
   **Campaign #2 Configuration:**
   ```
   Campaign duration in minutes (1-60): 10
   Number of games (1-10): 4
   Number of brands per game (1-5): 2
   Budget per brand per game (min 1): 200
   ```
   
   And so on...

### Step 4: Data Creation

The script will:
- ✅ Create 100 users
- ✅ Create 5 brands (Nike, Adidas, Starbucks, McDonald's, Amazon)
- ✅ Create your configured campaigns **starting NOW**
- ✅ Create games for each campaign **starting NOW**
- ✅ **All games end when their campaign ends** ⏱️
- ✅ Automatically start all games

### Step 5: Game Timeline

Shows when games will end:
```
📊 GAME TIMELINE

Campaign #1:
  • All games will end when campaign ends: 14:35:00
  • Campaign duration: 5 minutes

Campaign #2:
  • All games will end when campaign ends: 14:40:00
  • Campaign duration: 10 minutes
```

### Step 6: Wait Period (2 minutes)

After games are created and started:
```
Games are now active!
Waiting 2 minutes for games to stabilize before starting simulation...
This gives the probability algorithm time to adjust.

⏳ Time remaining: 01:50
```

**Why wait 2 minutes?**
- Allows TIME_BASED probability algorithm to adjust
- Games build up time-based probability scores
- Results in more realistic win patterns during simulation

### Step 7: Simulation (2 minutes)

After the wait, simulation starts automatically:
```
✓ Wait complete! Starting simulation now...

🎮 RUNNING SIMULATION
Simulating gameplay for 2 minutes to test probability...
Note: Campaign may last longer, but simulation runs for 2 min for quick results
Using 100 users and 7 games

🎉 WIN  | User: abc12345... | Coupons: 2
😢 LOSE | User: def67890...
🎉 WIN  | User: ghi12345... | Coupons: 1
😢 LOSE | User: jkl67890...
```

The simulation will:
- Run for exactly 2 minutes (regardless of campaign duration)
- Purpose: Quick visualization of winning probability
- Maintain 5-10 concurrent users
- Show **all wins and losses** in real-time
- Automatically stop when time is up or all games end

### Step 8: Results

After simulation completes, you'll see:

**A. Simulation Statistics:**
```
📊 SIMULATION STATISTICS

Total Plays: 487
Winners: 89 🎉
Losers: 398 😢

Win Rate: 18.28%
```

**B. Budget Report:**
```
💰 BUDGET REPORT - INITIAL VS FINAL

Spin the Wheel - Campaign #1
Campaign: Campaign #1 | Duration: 5 min
──────────────────────────────────────────────────────────────────────
Brand                Initial Budget       Final Budget         Used           
──────────────────────────────────────────────────────────────────────
Nike                 100                  78                   22 (22.0%)
Adidas               100                  81                   19 (19.0%)
Starbucks            100                  85                   15 (15.0%)
McDonald's           100                  80                   20 (20.0%)
Amazon               100                  82                   18 (18.0%)
```

## 🎯 Key Features

### 1. **Flexible Configuration**
- Choose exactly how many campaigns you want
- Control campaign durations (1-60 minutes)
- Set games per campaign (1-10)
- Configure brands per game (1-5)
- Configure budgets (minimum 1 coupon per brand)

### 2. **Realistic Timing**
- All campaigns and games **start immediately**
- All games end when **their campaign ends** ⏱️
- Consistent game timing for predictable results

### 3. **Smart Timing**
- **2-minute wait** after games start (for probability stabilization)
- **2-minute simulation** with active user gameplay
- **Simulation runs for 2 min** regardless of campaign duration (for quick probability testing)
- **Total demo time: 4 minutes** (2 min wait + 2 min simulation)

### 4. **Automatic Simulation**
- Maintains realistic concurrent user load (5-10 users)
- Random gameplay patterns
- Real-time winner notifications
- Stops after 2 minutes or when all games end

### 5. **Comprehensive Reporting**
- **Initial vs Final budgets** for every game-brand combination
- **Usage percentage** showing how much budget was consumed
- **Win rate statistics** across all plays
- **Color-coded results** for easy reading

## 📊 Example Session

```bash
$ python3 interactive_demo.py

🎮 FROLIC INTERACTIVE DEMO 🎮
✓ Server is running and healthy

🧹 CLEANING UP EXISTING DATA
✓ Cleanup complete!

📝 DEMO CONFIGURATION
How many campaigns to create? (1-10): 2

Campaign #1 Configuration:
  Campaign duration in minutes (1-60): 3
  Number of games (1-10): 4
  Budget per brand per game (10-1000 coupons): 150

Campaign #2 Configuration:
  Campaign duration in minutes (1-60): 5
  Number of games (1-10): 3
  Budget per brand per game (10-1000 coupons): 200

🎯 CREATING BASE DATA
✓ Created 100 users
✓ Created 5 brands

🎮 CREATING DEMO DATA
✓ Created: Campaign #1 (expires in 3 min)
  ⏱️  Started: Spin the Wheel - Campaign #1 (ends in 3 min)
  ⏱️  Started: Lucky Draw - Campaign #1 (ends in 3 min)
  ⏱️  Started: Treasure Hunt - Campaign #1 (ends in 3 min)
  ⏱️  Started: Prize Picker - Campaign #1 (ends in 3 min)

✓ Created: Campaign #2 (expires in 5 min)
  ⏱️  Started: Fortune Finder - Campaign #2 (ends in 5 min)
  ⏱️  Started: Reward Roulette - Campaign #2 (ends in 5 min)
  ⏱️  Started: Gift Grab - Campaign #2 (ends in 5 min)

📊 GAME TIMELINE
Campaign #1:
  • All games will end when campaign ends: 14:33:00
  • Campaign duration: 3 minutes

Campaign #2:
  • All games will end when campaign ends: 14:35:00
  • Campaign duration: 5 minutes

Games are now active!
Waiting 2 minutes for games to stabilize before starting simulation...
This gives the probability algorithm time to adjust.

⏳ Time remaining: 01:50
⏳ Time remaining: 01:40
⏳ Time remaining: 01:30
...
✓ Wait complete! Starting simulation now...

🎮 RUNNING SIMULATION
Simulating gameplay for 2 minutes to test probability...
Note: Campaign may last longer, but simulation runs for 2 min for quick results
Using 100 users and 7 games

🎉 WIN  | User: 1a2b3c4d... | Coupons: 2
😢 LOSE | User: 2b3c4d5e...
🎉 WIN  | User: 5e6f7g8h... | Coupons: 1
😢 LOSE | User: 6f7g8h9i...
...
✓ Simulation complete!

📊 SIMULATION STATISTICS
Total Plays: 324
Winners: 67 🎉
Losers: 257 😢

Win Rate: 20.68%

💰 BUDGET REPORT - INITIAL VS FINAL
[Detailed budget breakdown for each game...]

🎉 DEMO COMPLETE 🎉
Thank you for using Frolic Interactive Demo!
```

## 💡 Use Cases

### 1. **Quick 1-Minute Demo**
```
Campaigns: 1
Duration: 1 minute
Games: 3
Budget: 50

Shows rapid gameplay and budget depletion
```

### 2. **Medium Demo (5-10 minutes)**
```
Campaigns: 2-3
Duration: 5-10 minutes each
Games: 4-6 per campaign
Budget: 100-200

Balanced demo showing both quick and regular games
```

### 3. **Extended Demo (15-30 minutes)**
```
Campaigns: 3-5
Duration: 10-30 minutes
Games: 6-8 per campaign
Budget: 300-500

Full demonstration of time-based probability
```

### 4. **Budget Testing**
```
Campaigns: 1
Duration: 2 minutes
Games: 5
Budget: Vary (50, 100, 200, 500)

Compare budget usage across different allocations
```

## 🎨 Visual Indicators

| Icon | Meaning |
|------|---------|
| ⚡ | Quick game (ends in 1 minute) |
| ⏱️ | Regular game (ends with campaign) |
| 🎉 | Winner |
| 😢 | Loser |
| ✓ | Success |
| ⚠ | Warning |
| ✗ | Error |

## 🔧 Tips

1. **Start Small**: Begin with 1-2 campaigns and 3-4 games
2. **Quick Results**: Use 1-2 minute campaigns to see fast results
3. **Budget Variety**: Try different budgets to see consumption patterns
4. **Watch Quick Games**: They show budget depletion most clearly
5. **Interrupt Safely**: Press Ctrl+C during simulation to stop early

## ⚠️ Important Notes

- **All campaigns start immediately** at data creation time
- **At least half the games are "quick"** (1 minute duration)
- **Simulation runs for longest campaign duration**
- **Budget report shows initial vs final** for all game-brand combinations
- **Data is cleaned up at start** (fresh demo every time)

## 🐛 Troubleshooting

**Issue: "Server is not healthy"**
```bash
# Start the server first
java -jar frolic-services/target/frolic-services-1.0-SNAPSHOT.jar
```

**Issue: No active games remaining**
- Games ended faster than expected
- Try increasing campaign duration or budget

**Issue: Low win rate**
- Normal for TIME_BASED probability early in game
- Win rate increases as games approach end time

## 📚 Related Documentation

- **[QUICK_START.md](QUICK_START.md)** - General quick start
- **[DEMO_SCRIPTS_README.md](DEMO_SCRIPTS_README.md)** - Automated demos
- **[API_TESTING_GUIDE.md](API_TESTING_GUIDE.md)** - Manual API testing

---

**🎮 Ready to play? Run `python3 interactive_demo.py` now! 🎮**
