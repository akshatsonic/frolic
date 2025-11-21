#!/usr/bin/env python3
"""
Interactive Frolic Demo
User-friendly interactive demo with custom configuration
"""

import requests
import json
from datetime import datetime, timedelta
import random
import time
import sys
import threading
from collections import defaultdict
import redis

BASE_URL = "http://localhost:8080/api/v1"
REDIS_HOST = "localhost"
REDIS_PORT = 6379

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

# Sample names
FIRST_NAMES = ["John", "Jane", "Michael", "Sarah", "David", "Emily", "James", "Emma", 
               "Robert", "Olivia", "William", "Ava", "Richard", "Sophia", "Joseph"]

LAST_NAMES = ["Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller",
              "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Wilson"]

BRANDS = [
    {"name": "Nike", "description": "Sports brand"},
    {"name": "Adidas", "description": "Athletic gear"},
    {"name": "Starbucks", "description": "Coffee chain"},
    {"name": "McDonald's", "description": "Fast food"},
    {"name": "Amazon", "description": "E-commerce"}
]

GAME_NAMES = [
    "Spin the Wheel", "Lucky Draw", "Treasure Hunt", "Prize Picker",
    "Fortune Finder", "Reward Roulette", "Gift Grab", "Bonus Box"
]

# Global budget tracking
budget_tracker = {}
game_metadata = {}  # Maps game_id to {campaign_num, game_num, total_games}
simulation_stats = {
    "total_plays": 0,
    "winners": 0,
    "losers": 0,
    "lock": threading.Lock()
}

def cleanup_all_data():
    """Clean up existing data"""
    print_header("🧹 CLEANING UP EXISTING DATA")
    print_info("Waiting 2 seconds... (Ctrl+C to cancel)\n")
    time.sleep(2)
    
    # Delete games, campaigns, brands, users
    for entity_type in ['games', 'campaigns', 'brands', 'users']:
        try:
            response = requests.get(f"{BASE_URL}/admin/{entity_type}")
            if response.status_code == 200:
                items = response.json()
                for item in items:
                    requests.delete(f"{BASE_URL}/admin/{entity_type}/{item['id']}")
        except:
            pass
    
    print_success("✓ Cleanup complete!\n")

def create_users(count=50):
    """Create demo users"""
    print_info(f"Creating {count} users...")
    created_users = []
    
    for i in range(count):
        first = random.choice(FIRST_NAMES)
        last = random.choice(LAST_NAMES)
        user_data = {
            "email": f"{first.lower()}.{last.lower()}{i}@demo.com",
            "name": f"{first} {last}",
            "phoneNumber": f"+1{random.randint(2000000000, 9999999999)}",
            "active": True
        }
        
        try:
            response = requests.post(f"{BASE_URL}/admin/users", json=user_data)
            if response.status_code == 201:
                created_users.append(response.json())
        except:
            pass
    
    print_success(f"✓ Created {len(created_users)} users")
    return created_users

def create_brands():
    """Create brands"""
    print_info("Creating brands...")
    created_brands = []
    
    for brand_data in BRANDS:
        payload = {
            "name": brand_data["name"],
            "description": brand_data["description"],
            "active": True
        }
        
        try:
            response = requests.post(f"{BASE_URL}/admin/brands", json=payload)
            if response.status_code == 201:
                created_brands.append(response.json())
        except:
            pass
    
    print_success(f"✓ Created {len(created_brands)} brands")
    return created_brands

def get_user_input():
    """Get configuration from user"""
    print_header("📝 DEMO CONFIGURATION")
    
    # Get number of campaigns
    while True:
        try:
            num_campaigns = int(input(f"{Colors.OKCYAN}How many campaigns to create? (1-10): {Colors.ENDC}"))
            if 1 <= num_campaigns <= 10:
                break
            print_warning("Please enter a number between 1 and 10")
        except ValueError:
            print_warning("Please enter a valid number")
    
    campaigns_config = []
    
    for i in range(num_campaigns):
        print(f"\n{Colors.BOLD}Campaign #{i+1} Configuration:{Colors.ENDC}")
        
        # Campaign duration
        while True:
            try:
                duration = int(input(f"  Campaign duration in minutes (1-60): "))
                if 1 <= duration <= 60:
                    break
                print_warning("  Please enter a number between 1 and 60")
            except ValueError:
                print_warning("  Please enter a valid number")
        
        # Games per campaign
        while True:
            try:
                num_games = int(input(f"  Number of games (1-10): "))
                if 1 <= num_games <= 10:
                    break
                print_warning("  Please enter a number between 1 and 10")
            except ValueError:
                print_warning("  Please enter a valid number")
        
        # Brands per game
        while True:
            try:
                brands_per_game = int(input(f"  Number of brands per game (1-5): "))
                if 1 <= brands_per_game <= 5:
                    break
                print_warning("  Please enter a number between 1 and 5")
            except ValueError:
                print_warning("  Please enter a valid number")
        
        # Budget per brand
        while True:
            try:
                budget = int(input(f"  Budget per brand per game (min 1): "))
                if budget >= 1:
                    break
                print_warning("  Please enter a number >= 1")
            except ValueError:
                print_warning("  Please enter a valid number")
        
        campaigns_config.append({
            'duration_minutes': duration,
            'num_games': num_games,
            'brands_per_game': brands_per_game,
            'budget_per_brand': budget
        })
    
    return campaigns_config

def create_interactive_demo(campaigns_config, brands):
    """Create campaigns and games based on user input"""
    print_header("🎮 CREATING DEMO DATA")
    
    now = datetime.now()
    created_campaigns = []
    created_games = []
    campaign_game_counts = {}  # Track total games per campaign
    
    for idx, config in enumerate(campaigns_config):
        # Create campaign starting now
        campaign_name = f"Campaign #{idx+1}"
        campaign_start = now
        campaign_end = now + timedelta(minutes=config['duration_minutes'])
        
        campaign_data = {
            "name": campaign_name,
            "status": "DRAFT",
            "startDate": campaign_start.strftime("%Y-%m-%dT%H:%M:%SZ"),
            "endDate": campaign_end.strftime("%Y-%m-%dT%H:%M:%SZ")
        }
        
        try:
            response = requests.post(f"{BASE_URL}/admin/campaigns", json=campaign_data)
            if response.status_code == 201:
                campaign = response.json()
                created_campaigns.append(campaign)
                print_success(f"✓ Created: {campaign_name} (expires in {config['duration_minutes']} min)")
                
                # Create games for this campaign
                num_games = config['num_games']
                brands_per_game = config['brands_per_game']
                budget = config['budget_per_brand']
                campaign_num = idx + 1
                campaign_game_counts[campaign['id']] = num_games
                
                # All games end when campaign ends
                for game_idx in range(num_games):
                    game_name = f"{random.choice(GAME_NAMES)} - {campaign_name}"
                    
                    game_start = now
                    game_end = campaign_end  # All games end with campaign
                    duration_str = f"{config['duration_minutes']} min"
                    
                    # Select random brands for this game
                    selected_brands = random.sample(brands, min(brands_per_game, len(brands)))
                    brand_budgets = [
                        {"brandId": brand['id'], "totalBudget": budget}
                        for brand in selected_brands
                    ]
                    
                    game_data = {
                        "name": game_name,
                        "campaignId": campaign['id'],
                        "status": "DRAFT",
                        "startTime": game_start.strftime("%Y-%m-%dT%H:%M:%SZ"),
                        "endTime": game_end.strftime("%Y-%m-%dT%H:%M:%SZ"),
                        "probabilityType": "TIME_BASED",
                        "slotGranularitySeconds": 5,
                        "brandBudgets": brand_budgets
                    }
                    
                    try:
                        game_response = requests.post(f"{BASE_URL}/admin/games", json=game_data)
                        if game_response.status_code == 201:
                            game = game_response.json()
                            created_games.append(game)
                            
                            # Track initial budget (only for selected brands)
                            budget_tracker[game['id']] = {
                                'game_name': game_name,
                                'campaign': campaign_name,
                                'duration': duration_str,
                                'brands': {}
                            }
                            
                            # Store game metadata for display
                            game_metadata[game['id']] = {
                                'campaign_num': campaign_num,
                                'game_num': game_idx + 1,
                                'total_games': num_games
                            }
                            
                            for brand in selected_brands:
                                budget_tracker[game['id']]['brands'][brand['id']] = {
                                    'brand_name': brand['name'],
                                    'initial_budget': budget,
                                    'final_budget': None
                                }
                            
                            # Start the game immediately
                            start_response = requests.post(f"{BASE_URL}/admin/games/{game['id']}/start")
                            if start_response.status_code == 200:
                                print_success(f"  ⏱️  Started: {game_name} (ends in {duration_str}, {len(selected_brands)} brands)")
                    except Exception as e:
                        print_warning(f"  Failed to create game: {e}")
        
        except Exception as e:
            print_error(f"Failed to create campaign: {e}")
    
    return created_campaigns, created_games

def print_game_summary(campaigns_config):
    """Print summary of when games will end"""
    print_header("📊 GAME TIMELINE")
    
    now = datetime.now()
    
    for idx, config in enumerate(campaigns_config):
        campaign_end = now + timedelta(minutes=config['duration_minutes'])
        print(f"{Colors.BOLD}Campaign #{idx+1}:{Colors.ENDC}")
        print(f"  • All games will end when campaign ends: {Colors.OKGREEN}{campaign_end.strftime('%H:%M:%S')}{Colors.ENDC}")
        print(f"  • Campaign duration: {Colors.OKCYAN}{config['duration_minutes']} minutes{Colors.ENDC}")
        print()

def simulate_play(user_id, game_id):
    """Simulate a single play"""
    play_data = {"userId": user_id, "gameId": game_id}
    
    # Get game metadata for display
    game_info = game_metadata.get(game_id, {})
    campaign_num = game_info.get('campaign_num', '?')
    game_num = game_info.get('game_num', '?')
    game_display = f"Campaign #{campaign_num}, Game #{game_num}"
    
    try:
        with simulation_stats["lock"]:
            simulation_stats["total_plays"] += 1
            current_total = simulation_stats["total_plays"]
        
        # Show progress every 10 plays
        if current_total % 10 == 0:
            print(f"{Colors.OKCYAN}📊 Total plays: {current_total}{Colors.ENDC}")
        
        response = requests.post(f"{BASE_URL}/play", json=play_data)
        if response.status_code == 202:
            play_result = response.json()
            play_id = play_result.get('playId')
            
            # Wait for result
            time.sleep(random.uniform(1.5, 2.5))
            
            result_response = requests.get(f"{BASE_URL}/play/{play_id}/result")
            if result_response.status_code == 200:
                result = result_response.json()
                with simulation_stats["lock"]:
                    if result.get('winner'):
                        simulation_stats["winners"] += 1
                        coupons = result.get('coupons', [])
                        
                        # Format coupon information
                        coupon_info = []
                        for coupon in coupons:
                            brand_id = coupon.get('brandId')
                            coupon_code = coupon.get('couponCode', 'N/A')
                            
                            # Look up brand name from budget tracker
                            brand_name = 'Unknown'
                            if game_id in budget_tracker:
                                brand_data = budget_tracker[game_id]['brands'].get(brand_id, {})
                                brand_name = brand_data.get('brand_name', brand_id[:8] + '...')
                            
                            coupon_info.append(f"{brand_name}:{coupon_code}")
                        
                        coupon_display = ", ".join(coupon_info) if coupon_info else "Got coupons"
                        print(f"{Colors.OKGREEN}🎉 WIN{Colors.ENDC}  | User: {user_id[:8]}... | {game_display} | 🎟️  {coupon_display}")
                    else:
                        simulation_stats["losers"] += 1
                        print(f"{Colors.WARNING}😢 LOSE{Colors.ENDC} | User: {user_id[:8]}... | {game_display}")
        else:
            print_warning(f"Play request failed: {response.status_code} - {response.text}")
    except Exception as e:
        print_warning(f"Error in simulate_play: {e}")

def run_simulation(users, games, duration_minutes):
    """Run gameplay simulation"""
    print_header("🎮 RUNNING SIMULATION")
    print_info(f"Simulating gameplay for {duration_minutes} minutes to test probability...")
    print_info(f"Note: Campaign may last longer, but simulation runs for {duration_minutes} min for quick results")
    print_info(f"Using {len(users)} users and {len(games)} games\n")
    print_info("Press Ctrl+C to stop early\n")
    
    start_time = time.time()
    end_time = start_time + (duration_minutes * 60)
    
    active_threads = []
    
    # Initial check for active games
    print_info("Checking for active games...")
    initial_active = 0
    for game in games:
        try:
            game_response = requests.get(f"{BASE_URL}/admin/games/{game['id']}")
            if game_response.status_code == 200:
                game_data = game_response.json()
                status = game_data.get('status')
                if status == 'ACTIVE':
                    initial_active += 1
                    print_success(f"  ✓ {game_data.get('name')} - {status}")
                else:
                    print_warning(f"  ⚠ {game_data.get('name')} - {status}")
        except Exception as e:
            print_warning(f"  ✗ Error checking game: {e}")
    
    if initial_active == 0:
        print_error("\n✗ No active games found! Cannot run simulation.")
        return
    
    print_success(f"\n✓ Found {initial_active} active games. Starting simulation...\n")
    
    try:
        while time.time() < end_time:
            # Check which games are still active
            active_games = []
            for game in games:
                try:
                    game_response = requests.get(f"{BASE_URL}/admin/games/{game['id']}")
                    if game_response.status_code == 200:
                        game_data = game_response.json()
                        if game_data.get('status') == 'ACTIVE':
                            active_games.append(game)
                except Exception as e:
                    pass
            
            if not active_games:
                print_warning("\n⚠ No active games remaining")
                break
            
            # Clean up finished threads
            active_threads = [t for t in active_threads if t.is_alive()]
            
            # Maintain 5-10 concurrent users
            while len(active_threads) < random.randint(5, 10):
                user = random.choice(users)
                game = random.choice(active_games)
                
                thread = threading.Thread(target=simulate_play, args=(user['id'], game['id']))
                thread.start()
                active_threads.append(thread)
                
                time.sleep(random.uniform(0.5, 2.0))
    
    except KeyboardInterrupt:
        print_warning("\n\n⚠ Simulation interrupted by user")
    
    # Wait for remaining threads
    print_info("\nWaiting for active plays to complete...")
    for thread in active_threads:
        thread.join(timeout=5)
    
    print_success("✓ Simulation complete!")

def fetch_final_budgets(games, brands):
    """Fetch final budgets directly from Redis"""
    print_info("\nFetching final budget data from Redis...")
    
    try:
        # Connect to Redis
        redis_client = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, db=0, decode_responses=True)
        redis_client.ping()  # Test connection
        print_success("✓ Connected to Redis")
        
        for game in games:
            try:
                if game['id'] in budget_tracker:
                    # Fetch remaining budget for each brand from Redis
                    for brand_id in budget_tracker[game['id']]['brands'].keys():
                        # Redis key pattern: budget:game:{gameId}:brand:{brandId}
                        redis_key = f"budget:game:{game['id']}:brand:{brand_id}"
                        remaining_budget = redis_client.get(redis_key)
                        
                        if remaining_budget is not None:
                            budget_tracker[game['id']]['brands'][brand_id]['final_budget'] = int(remaining_budget)
                            print_success(f"  ✓ Game {game['id'][:8]}..., Brand {brand_id[:8]}...: {remaining_budget} remaining")
                        else:
                            # Key doesn't exist, assume budget is exhausted
                            budget_tracker[game['id']]['brands'][brand_id]['final_budget'] = 0
                            print_info(f"  ℹ Game {game['id'][:8]}..., Brand {brand_id[:8]}...: Key not found (assuming 0)")
            except Exception as e:
                print_warning(f"  ⚠ Could not fetch budget for game {game['id']}: {e}")
        
        redis_client.close()
        print_success("✓ Final budgets fetched from Redis")
        
    except redis.ConnectionError as e:
        print_error(f"✗ Could not connect to Redis: {e}")
        print_warning("  Make sure Redis is running on {REDIS_HOST}:{REDIS_PORT}")
    except Exception as e:
        print_error(f"✗ Error fetching budgets from Redis: {e}")

def print_budget_report():
    """Print budget comparison report"""
    print_header("💰 BUDGET REPORT - INITIAL VS FINAL")
    
    for game_id, game_info in budget_tracker.items():
        print(f"\n{Colors.BOLD}{game_info['game_name']}{Colors.ENDC}")
        print(f"Campaign: {game_info['campaign']} | Duration: {game_info['duration']}")
        print(f"{Colors.OKCYAN}{'─' * 70}{Colors.ENDC}")
        
        print(f"{'Brand':<20} {'Initial Budget':<20} {'Final Budget':<20} {'Used':<15}")
        print(f"{Colors.OKCYAN}{'─' * 70}{Colors.ENDC}")
        
        for brand_id, brand_info in game_info['brands'].items():
            initial = brand_info['initial_budget']
            final = brand_info.get('final_budget')
            
            if final is not None:
                used = initial - final
                used_pct = (used / initial * 100) if initial > 0 else 0
                final_str = f"{final}"
                used_str = f"{used} ({used_pct:.1f}%)"
                
                if used == initial:
                    color = Colors.OKGREEN
                elif used > initial * 0.5:
                    color = Colors.WARNING
                else:
                    color = Colors.ENDC
            else:
                final_str = "N/A"
                used_str = "N/A"
                color = Colors.ENDC
            
            print(f"{brand_info['brand_name']:<20} {initial:<20} {color}{final_str:<20}{Colors.ENDC} {used_str:<15}")
        
        print()

def print_simulation_stats():
    """Print simulation statistics"""
    print_header("📊 SIMULATION STATISTICS")
    
    with simulation_stats["lock"]:
        total = simulation_stats["total_plays"]
        winners = simulation_stats["winners"]
        losers = simulation_stats["losers"]
        
        print(f"{Colors.BOLD}Total Plays:{Colors.ENDC} {total}")
        print(f"{Colors.OKGREEN}Winners:{Colors.ENDC} {winners} 🎉")
        print(f"{Colors.WARNING}Losers:{Colors.ENDC} {losers} 😢")
        
        if total > 0:
            win_rate = (winners / total) * 100
            print(f"\n{Colors.BOLD}Win Rate:{Colors.ENDC} {win_rate:.2f}%")

def main():
    print_header("🎮 FROLIC INTERACTIVE DEMO 🎮")
    
    # Check server
    try:
        response = requests.get(f"{BASE_URL.replace('/api/v1', '')}/actuator/health")
        if response.status_code != 200:
            print_error("Server is not healthy!")
            sys.exit(1)
        print_success("✓ Server is running and healthy\n")
    except Exception as e:
        print_error(f"Cannot connect to server: {e}")
        sys.exit(1)
    
    # Cleanup
    cleanup_all_data()
    
    # Get user input
    campaigns_config = get_user_input()
    
    # Create base data
    print_header("🎯 CREATING BASE DATA")
    users = create_users(100)
    brands = create_brands()
    
    # Create campaigns and games
    campaigns, games = create_interactive_demo(campaigns_config, brands)
    
    if not games:
        print_error("No games were created!")
        sys.exit(1)
    
    # Print game summary
    print_game_summary(campaigns_config)
    
    # Wait 2 minutes before starting simulation
    wait_duration = 2
    simulation_duration = 2
    
    print_info(f"\n{Colors.BOLD}Games are now active!{Colors.ENDC}")
    print_info(f"Waiting {wait_duration} minutes for games to stabilize before starting simulation...")
    print_warning(f"This gives the probability algorithm time to adjust.\n")
    
    # Countdown for 2 minutes
    for remaining in range(wait_duration * 60, 0, -10):
        mins = remaining // 60
        secs = remaining % 60
        print(f"\r{Colors.OKCYAN}⏳ Time remaining: {mins:02d}:{secs:02d}{Colors.ENDC}", end='', flush=True)
        time.sleep(10)
    
    print(f"\r{Colors.OKGREEN}✓ Wait complete! Starting simulation now...{Colors.ENDC}\n")
    
    # Run simulation
    run_simulation(users, games, simulation_duration)
    
    # Get final budgets
    fetch_final_budgets(games, brands)
    
    # Print reports
    print_simulation_stats()
    print_budget_report()
    
    print_header("🎉 DEMO COMPLETE 🎉")
    print_success("Thank you for using Frolic Interactive Demo!")

if __name__ == "__main__":
    main()
