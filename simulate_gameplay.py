#!/usr/bin/env python3
"""
Gameplay Simulation Script
Simulates users playing active games with realistic patterns
"""

import requests
import json
import random
import time
import threading
from datetime import datetime
import sys
from collections import defaultdict

BASE_URL = "http://localhost:8080/api/v1"

# Color codes
class Colors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

# Statistics
stats = {
    "total_plays": 0,
    "successful_plays": 0,
    "failed_plays": 0,
    "winners": 0,
    "losers": 0,
    "results_checked": 0,
    "games_played": defaultdict(int),
    "lock": threading.Lock()
}

def print_header(text):
    print(f"\n{Colors.HEADER}{Colors.BOLD}{'='*70}{Colors.ENDC}")
    print(f"{Colors.HEADER}{Colors.BOLD}{text:^70}{Colors.ENDC}")
    print(f"{Colors.HEADER}{Colors.BOLD}{'='*70}{Colors.ENDC}\n")

def print_success(text):
    print(f"{Colors.OKGREEN}✓ {text}{Colors.ENDC}")

def print_info(text):
    print(f"{Colors.OKCYAN}ℹ {text}{Colors.ENDC}")

def print_warning(text):
    print(f"{Colors.WARNING}⚠ {text}{Colors.ENDC}")

def print_error(text):
    print(f"{Colors.FAIL}✗ {text}{Colors.ENDC}")

def update_stats(key, value=1):
    """Thread-safe stats update"""
    with stats["lock"]:
        if key in ["total_plays", "successful_plays", "failed_plays", "winners", "losers", "results_checked"]:
            stats[key] += value

def simulate_play(user_id, game_id, game_name, delay=0):
    """Simulate a single play"""
    if delay > 0:
        time.sleep(delay)
    
    play_data = {
        "userId": user_id,
        "gameId": game_id
    }
    
    try:
        update_stats("total_plays")
        response = requests.post(f"{BASE_URL}/play", json=play_data)
        
        if response.status_code == 202:
            play_result = response.json()
            play_id = play_result.get('playId')
            update_stats("successful_plays")
            with stats["lock"]:
                stats["games_played"][game_id] += 1
            
            # Wait a bit then check result
            time.sleep(random.uniform(1.5, 3.0))
            
            result_response = requests.get(f"{BASE_URL}/play/{play_id}/result")
            if result_response.status_code == 200:
                result = result_response.json()
                update_stats("results_checked")
                
                if result.get('winner'):
                    update_stats("winners")
                    coupons = result.get('coupons', [])
                    coupon_str = f" ({len(coupons)} coupons)" if coupons else ""
                    print(f"{Colors.OKGREEN}🎉 WIN{Colors.ENDC} | User: {user_id[:8]}... | Game: {game_name[:30]}...{coupon_str}")
                else:
                    update_stats("losers")
                    if random.random() < 0.1:  # Print 10% of losses to avoid spam
                        print(f"{Colors.WARNING}😢 LOSE{Colors.ENDC} | User: {user_id[:8]}... | Game: {game_name[:30]}...")
            
        else:
            update_stats("failed_plays")
            if response.status_code == 400:
                error = response.json()
                if "not active" in error.get('message', '').lower():
                    pass  # Game ended, don't spam
                else:
                    print_warning(f"Play failed for user {user_id[:8]}... : {error.get('message', 'Unknown error')}")
            else:
                print_warning(f"Play failed: {response.status_code}")
                
    except Exception as e:
        update_stats("failed_plays")
        print_error(f"Error during play: {e}")

def simulate_user_session(user_id, game_ids, game_map, duration_seconds=120, plays_per_user=None):
    """Simulate a user playing multiple games"""
    if not game_ids:
        return
    
    # Each user plays 3-8 games randomly
    num_plays = plays_per_user or random.randint(3, 8)
    
    for _ in range(num_plays):
        game_id = random.choice(game_ids)
        game_name = game_map.get(game_id, "Unknown Game")
        
        # Random delay between plays (0.5 to 3 seconds)
        delay = random.uniform(0.5, 3.0)
        simulate_play(user_id, game_id, game_name, delay)
        
        # Check if we should stop (simulation duration)
        if duration_seconds and random.random() < 0.1:
            break

def wave_simulation(user_ids, game_ids, game_map, waves=5, users_per_wave=20):
    """Simulate waves of users joining"""
    print_header("🌊 WAVE SIMULATION MODE 🌊")
    print_info(f"Will run {waves} waves with {users_per_wave} users each")
    print_info("Simulating realistic traffic patterns...\n")
    
    for wave in range(waves):
        print(f"\n{Colors.BOLD}{Colors.OKCYAN}━━━ Wave {wave + 1}/{waves} ━━━{Colors.ENDC}")
        
        # Select random users for this wave
        wave_users = random.sample(user_ids, min(users_per_wave, len(user_ids)))
        
        # Start threads for concurrent users
        threads = []
        for user_id in wave_users:
            thread = threading.Thread(
                target=simulate_user_session,
                args=(user_id, game_ids, game_map, 60, None)
            )
            threads.append(thread)
            thread.start()
            
            # Stagger user starts slightly
            time.sleep(random.uniform(0.05, 0.2))
        
        # Wait for wave to complete
        for thread in threads:
            thread.join()
        
        print(f"{Colors.OKGREEN}✓ Wave {wave + 1} completed{Colors.ENDC}")
        
        # Pause between waves
        if wave < waves - 1:
            pause = random.uniform(2, 5)
            print_info(f"Pausing {pause:.1f}s before next wave...")
            time.sleep(pause)

def continuous_simulation(user_ids, game_ids, game_map, duration_seconds=300, concurrent_users=10):
    """Continuously simulate users playing games"""
    print_header("♾️  CONTINUOUS SIMULATION MODE ♾️")
    print_info(f"Running for {duration_seconds} seconds with ~{concurrent_users} concurrent users")
    print_info("Press Ctrl+C to stop early\n")
    
    start_time = time.time()
    active_threads = []
    
    try:
        while time.time() - start_time < duration_seconds:
            # Clean up finished threads
            active_threads = [t for t in active_threads if t.is_alive()]
            
            # Maintain concurrent user count
            while len(active_threads) < concurrent_users:
                user_id = random.choice(user_ids)
                thread = threading.Thread(
                    target=simulate_user_session,
                    args=(user_id, game_ids, game_map, 60, random.randint(2, 5))
                )
                thread.start()
                active_threads.append(thread)
                time.sleep(random.uniform(0.5, 2.0))
            
            time.sleep(1)
    
    except KeyboardInterrupt:
        print_warning("\n\n⚠ Interrupted by user")
    
    # Wait for remaining threads
    print_info("Waiting for active sessions to complete...")
    for thread in active_threads:
        thread.join(timeout=5)
    
    print_success("✓ Simulation completed")

def stress_test(user_ids, game_ids, game_map, total_plays=1000):
    """Stress test with rapid concurrent plays"""
    print_header("🔥 STRESS TEST MODE 🔥")
    print_info(f"Firing {total_plays} concurrent play requests")
    print_warning("This will test system under heavy load!\n")
    
    threads = []
    for i in range(total_plays):
        user_id = random.choice(user_ids)
        game_id = random.choice(game_ids)
        game_name = game_map.get(game_id, "Unknown")
        
        thread = threading.Thread(
            target=simulate_play,
            args=(user_id, game_id, game_name, 0)
        )
        threads.append(thread)
        thread.start()
        
        if (i + 1) % 100 == 0:
            print_info(f"Launched {i + 1}/{total_plays} play requests...")
    
    # Wait for all to complete
    print_info("Waiting for all plays to complete...")
    for thread in threads:
        thread.join()
    
    print_success("✓ Stress test completed")

def print_stats():
    """Print simulation statistics"""
    print_header("📊 SIMULATION STATISTICS 📊")
    
    with stats["lock"]:
        print(f"{Colors.BOLD}Total Plays:{Colors.ENDC} {stats['total_plays']}")
        print(f"{Colors.OKGREEN}Successful:{Colors.ENDC} {stats['successful_plays']}")
        print(f"{Colors.FAIL}Failed:{Colors.ENDC} {stats['failed_plays']}")
        print(f"{Colors.OKGREEN}Winners:{Colors.ENDC} {stats['winners']} 🎉")
        print(f"{Colors.WARNING}Losers:{Colors.ENDC} {stats['losers']} 😢")
        print(f"{Colors.OKCYAN}Results Checked:{Colors.ENDC} {stats['results_checked']}")
        
        if stats['results_checked'] > 0:
            win_rate = (stats['winners'] / stats['results_checked']) * 100
            print(f"\n{Colors.BOLD}Win Rate:{Colors.ENDC} {win_rate:.2f}%")
        
        if stats['games_played']:
            print(f"\n{Colors.BOLD}Games Played:{Colors.ENDC}")
            for game_id, count in sorted(stats['games_played'].items(), key=lambda x: x[1], reverse=True)[:10]:
                print(f"  • {game_id[:20]}... : {count} plays")

def load_demo_data():
    """Load demo data IDs from file"""
    try:
        with open('demo_data_ids.json', 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        print_error("demo_data_ids.json not found!")
        print_warning("Run setup_demo_data.py first to create demo data")
        sys.exit(1)
    except Exception as e:
        print_error(f"Error loading demo data: {e}")
        sys.exit(1)

def get_active_games():
    """Fetch currently active games"""
    try:
        response = requests.get(f"{BASE_URL}/admin/games")
        if response.status_code == 200:
            all_games = response.json()
            active = [g for g in all_games if g['status'] == 'ACTIVE']
            return active
        return []
    except Exception as e:
        print_error(f"Error fetching games: {e}")
        return []

def main():
    print_header("🎮 FROLIC GAMEPLAY SIMULATOR 🎮")
    
    # Check server health
    try:
        response = requests.get(f"{BASE_URL.replace('/api/v1', '')}/actuator/health")
        if response.status_code != 200:
            print_error("Server is not healthy!")
            sys.exit(1)
        print_success("✓ Server is running and healthy")
    except Exception as e:
        print_error(f"Cannot connect to server: {e}")
        sys.exit(1)
    
    # Load demo data
    demo_data = load_demo_data()
    user_ids = demo_data.get('user_ids', [])
    
    if not user_ids:
        print_error("No users found in demo data!")
        sys.exit(1)
    
    # Get active games
    active_games = get_active_games()
    
    if not active_games:
        print_error("No active games found!")
        print_warning("Make sure games are started. Run:")
        print_info("  curl -X POST http://localhost:8080/api/v1/admin/games/{game-id}/start")
        sys.exit(1)
    
    game_ids = [g['id'] for g in active_games]
    game_map = {g['id']: g['name'] for g in active_games}
    
    print_success(f"✓ Loaded {len(user_ids)} users")
    print_success(f"✓ Found {len(active_games)} active games\n")
    
    # Show active games
    print(f"{Colors.BOLD}Active Games:{Colors.ENDC}")
    for i, game in enumerate(active_games[:10], 1):
        print(f"  {i}. {game['name'][:50]}...")
    if len(active_games) > 10:
        print(f"  ... and {len(active_games) - 10} more")
    
    # Simulation mode selection
    print(f"\n{Colors.BOLD}Select Simulation Mode:{Colors.ENDC}")
    print("  1. Wave Simulation (5 waves of 20 users) - Realistic")
    print("  2. Continuous (5 min with 10 concurrent users) - Long running")
    print("  3. Stress Test (1000 rapid plays) - High load")
    print("  4. Quick Test (1 wave of 10 users) - Fast demo")
    
    mode = input(f"\n{Colors.OKCYAN}Enter mode (1-4) [default: 1]: {Colors.ENDC}").strip() or "1"
    
    print()  # Blank line before simulation starts
    
    if mode == "1":
        wave_simulation(user_ids, game_ids, game_map, waves=5, users_per_wave=20)
    elif mode == "2":
        continuous_simulation(user_ids, game_ids, game_map, duration_seconds=300, concurrent_users=10)
    elif mode == "3":
        stress_test(user_ids, game_ids, game_map, total_plays=1000)
    elif mode == "4":
        wave_simulation(user_ids, game_ids, game_map, waves=1, users_per_wave=10)
    else:
        print_warning("Invalid mode, using default (Wave Simulation)")
        wave_simulation(user_ids, game_ids, game_map, waves=5, users_per_wave=20)
    
    # Print final statistics
    print_stats()
    
    print(f"\n{Colors.BOLD}🎉 Simulation Complete! 🎉{Colors.ENDC}\n")

if __name__ == "__main__":
    main()
