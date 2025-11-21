# 🎮 Interactive Demo - Quick Reference

## One-Liner
```bash
python3 interactive_demo.py
```

## What You Get

### 1. Interactive Configuration
```
✓ Choose number of campaigns (1-10)
✓ Set campaign duration in minutes (1-60)
✓ Configure games per campaign (1-10)
✓ Set budget per brand (10-1000 coupons)
```

### 2. Immediate Start
```
✓ All campaigns start NOW
✓ All games start NOW
✓ All games end when campaign ends ⏱️
```

### 3. Smart Timing
```
✓ 2-minute wait after games start (probability stabilization)
✓ 2-minute simulation with active gameplay
✓ Total demo time: 4 minutes
```

### 4. Auto-Simulation
```
✓ Simulates 5-10 concurrent users
✓ Real-time winner notifications
✓ Automatic completion
```

### 5. Comprehensive Reports
```
✓ Simulation statistics (plays, winners, win rate)
✓ Budget comparison (initial vs final)
✓ Usage percentages per game-brand
✓ Color-coded results
```

## Example Usage

### Quick 1-Minute Demo
```
Campaigns: 1
Duration: 1 minute
Games: 3
Budget: 50

Perfect for: Quick presentations, fast results
```

### Balanced 5-Minute Demo
```
Campaigns: 2
Duration: 5 minutes each
Games: 4-5 per campaign
Budget: 100-200

Perfect for: Standard demonstrations
```

### Extended 15-Minute Demo
```
Campaigns: 3
Duration: 5-15 minutes
Games: 6-8 per campaign
Budget: 200-500

Perfect for: In-depth analysis, time-based probability showcase
```

## Sample Output Flow

```
🎮 FROLIC INTERACTIVE DEMO 🎮
✓ Server is running and healthy

🧹 CLEANING UP EXISTING DATA
✓ Cleanup complete!

📝 DEMO CONFIGURATION
How many campaigns to create? (1-10): 2

Campaign #1 Configuration:
  Campaign duration in minutes (1-60): 3
  Number of games (1-10): 4
  Budget per brand per game (10-1000 coupons): 100

Campaign #2 Configuration:
  Campaign duration in minutes (1-60): 5
  Number of games (1-10): 3
  Budget per brand per game (10-1000 coupons): 150

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
...
✓ Wait complete! Starting simulation now...

🎮 RUNNING SIMULATION
Simulating gameplay for 2 minutes to test probability...
Note: Campaign may last longer, but simulation runs for 2 min for quick results
Using 100 users and 7 games

🎉 WIN  | User: abc12345... | Coupons: 2
😢 LOSE | User: bcd67890...
🎉 WIN  | User: def67890... | Coupons: 1
😢 LOSE | User: efg12345...
...

✓ Simulation complete!

📊 SIMULATION STATISTICS
Total Plays: 287
Winners: 54 🎉
Losers: 233 😢

Win Rate: 18.82%

💰 BUDGET REPORT - INITIAL VS FINAL

Spin the Wheel - Campaign #1
Campaign: Campaign #1 | Duration: 1 min
──────────────────────────────────────────────────────────────────────
Brand                Initial Budget       Final Budget         Used           
──────────────────────────────────────────────────────────────────────
Nike                 100                  8                    92 (92.0%)
Adidas               100                  12                   88 (88.0%)
Starbucks            100                  10                   90 (90.0%)
McDonald's           100                  15                   85 (85.0%)
Amazon               100                  11                   89 (89.0%)

[... more games ...]

🎉 DEMO COMPLETE 🎉
Thank you for using Frolic Interactive Demo!
```

## Key Differences from Automated Demo

| Feature | Interactive Demo | Automated Demo |
|---------|-----------------|----------------|
| Configuration | User inputs each value | Pre-configured |
| Campaign count | 1-10 (your choice) | 20 (fixed) |
| Start time | NOW | Mixed (past/current/future) |
| Quick games | At least half per campaign | 20% of all games |
| Simulation | Auto-runs after setup | Separate script |
| Budget report | Included automatically | Not included |
| Best for | Presentations, custom testing | Batch testing, realistic data |

## Advantages

✅ **Perfect for presentations** - configure on the fly  
✅ **Immediate results** - everything starts now  
✅ **Quick games** - see results in 1 minute  
✅ **Budget tracking** - see exactly what was used  
✅ **Single command** - no separate simulation script  
✅ **User-friendly** - guided prompts and validation  

## When to Use

### Use Interactive Demo When:
- Presenting to stakeholders
- Need custom configuration
- Want immediate results (games start NOW)
- Need budget comparison reports
- Testing specific scenarios
- Time is limited (1-5 minutes)

### Use Automated Demo When:
- Need realistic historical data
- Want 100+ entities
- Testing across time ranges
- Need reproducible setups
- Batch testing
- Don't need budget reports

## Quick Tips

1. **Start with 1-2 campaigns** to understand the flow
2. **Use 1-2 minute durations** for fastest results
3. **Set moderate budgets** (50-200) to see variety
4. **Watch quick games** - they show budget usage clearly
5. **Press Ctrl+C** during simulation to stop early if needed

## Files Created

| File | Description |
|------|-------------|
| `interactive_demo.py` | Main interactive demo script (550+ lines) |
| `INTERACTIVE_DEMO_GUIDE.md` | Comprehensive guide with examples |
| `INTERACTIVE_DEMO_SUMMARY.md` | This quick reference |

## Full Documentation

📖 **[INTERACTIVE_DEMO_GUIDE.md](INTERACTIVE_DEMO_GUIDE.md)** - Complete guide with detailed examples

---

**🎮 Ready? Just run: `python3 interactive_demo.py` 🎮**
