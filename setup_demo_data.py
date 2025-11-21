#!/usr/bin/env python3
"""
Demo Data Setup Script
Creates users, brands, campaigns, and games with realistic data
"""

import requests
import json
from datetime import datetime, timedelta, timezone
import random
import time
import sys

BASE_URL = "http://localhost:8080/api/v1"

# Color codes for output
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
    print(f"\n{Colors.HEADER}{Colors.BOLD}{'='*60}{Colors.ENDC}")
    print(f"{Colors.HEADER}{Colors.BOLD}{text:^60}{Colors.ENDC}")
    print(f"{Colors.HEADER}{Colors.BOLD}{'='*60}{Colors.ENDC}\n")

def print_success(text):
    print(f"{Colors.OKGREEN}✓ {text}{Colors.ENDC}")

def print_info(text):
    print(f"{Colors.OKCYAN}ℹ {text}{Colors.ENDC}")

def print_warning(text):
    print(f"{Colors.WARNING}⚠ {text}{Colors.ENDC}")

def print_error(text):
    print(f"{Colors.FAIL}✗ {text}{Colors.ENDC}")

# Sample data
FIRST_NAMES = ["John", "Jane", "Michael", "Sarah", "David", "Emily", "James", "Emma", 
               "Robert", "Olivia", "William", "Ava", "Richard", "Sophia", "Joseph", 
               "Isabella", "Thomas", "Mia", "Charles", "Charlotte", "Daniel", "Amelia",
               "Matthew", "Harper", "Anthony", "Evelyn", "Mark", "Abigail", "Donald", 
               "Emily", "Steven", "Elizabeth", "Paul", "Sofia", "Andrew", "Avery",
               "Joshua", "Ella", "Kenneth", "Scarlett", "Kevin", "Grace", "Brian",
               "Chloe", "George", "Victoria", "Edward", "Madison", "Ronald", "Luna"]

LAST_NAMES = ["Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller",
              "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez",
              "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin",
              "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez", "Clark",
              "Ramirez", "Lewis", "Robinson", "Walker", "Young", "Allen", "King",
              "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores", "Green"]

BRANDS = [
    {"name": "Nike", "description": "Global sports brand - athletic footwear and apparel"},
    {"name": "Adidas", "description": "Sports fashion and performance gear"},
    {"name": "Starbucks", "description": "Premium coffee and beverages"},
    {"name": "McDonald's", "description": "Fast food restaurant chain"},
    {"name": "Amazon", "description": "E-commerce and cloud services"},
    {"name": "Apple", "description": "Technology and consumer electronics"},
    {"name": "Coca-Cola", "description": "Beverage company"},
    {"name": "Pepsi", "description": "Food and beverage company"},
    {"name": "Samsung", "description": "Electronics and technology"},
    {"name": "Netflix", "description": "Streaming entertainment service"}
]

CAMPAIGN_THEMES = [
    "Black Friday Bonanza", "Cyber Monday Madness", "New Year Blast",
    "Valentine's Special", "Spring Sale", "Summer Splash",
    "Back to School", "Halloween Treats", "Thanksgiving Deals",
    "Christmas Carnival", "Winter Wonderland", "Flash Sale Friday",
    "Weekend Warriors", "Mega Monday", "Happy Hour Special",
    "Early Bird Offers", "Late Night Deals", "Birthday Bash",
    "Anniversary Special", "Grand Opening"
]

GAME_NAMES = [
    "Spin the Wheel", "Lucky Draw", "Treasure Hunt", "Prize Picker",
    "Fortune Finder", "Reward Roulette", "Gift Grab", "Bonus Box",
    "Golden Ticket", "Diamond Dash", "Mystery Box", "Power Play"
]

def create_users(count=100):
    """Create demo users"""
    print_header(f"Creating {count} Users")
    created_users = []
    
    for i in range(count):
        first_name = random.choice(FIRST_NAMES)
        last_name = random.choice(LAST_NAMES)
        email = f"{first_name.lower()}.{last_name.lower()}{i}@demo.com"
        phone = f"+1{random.randint(2000000000, 9999999999)}"
        
        user_data = {
            "email": email,
            "name": f"{first_name} {last_name}",
            "phoneNumber": phone,
            "active": True
        }
        
        try:
            response = requests.post(f"{BASE_URL}/admin/users", json=user_data)
            if response.status_code == 201:
                user = response.json()
                created_users.append(user)
                if (i + 1) % 10 == 0:
                    print_success(f"Created {i + 1}/{count} users")
            else:
                print_warning(f"Failed to create user {email}: {response.status_code}")
        except Exception as e:
            print_error(f"Error creating user: {e}")
    
    print_success(f"✓ Successfully created {len(created_users)} users")
    return created_users

def create_brands():
    """Create demo brands"""
    print_header(f"Creating {len(BRANDS)} Brands")
    created_brands = []
    
    for brand_data in BRANDS:
        brand_payload = {
            "name": brand_data["name"],
            "description": brand_data["description"],
            "active": True
        }
        
        try:
            response = requests.post(f"{BASE_URL}/admin/brands", json=brand_payload)
            if response.status_code == 201:
                brand = response.json()
                created_brands.append(brand)
                print_success(f"Created brand: {brand['name']} (ID: {brand['id']})")
            else:
                print_warning(f"Failed to create brand {brand_data['name']}: {response.status_code}")
        except Exception as e:
            print_error(f"Error creating brand: {e}")
    
    print_success(f"✓ Successfully created {len(created_brands)} brands")
    return created_brands

def create_campaigns(count=20):
    """Create demo campaigns with realistic time ranges"""
    print_header(f"Creating {count} Campaigns")
    created_campaigns = []
    now = datetime.now(timezone.utc)
    
    for i in range(count):
        theme = random.choice(CAMPAIGN_THEMES)
        name = f"{theme} #{i+1}"
        
        # Mix of past, current, and future campaigns
        if i < 5:
            # Past campaigns (already ended)
            start_date = now - timedelta(days=random.randint(10, 30))
            end_date = start_date + timedelta(days=random.randint(3, 7))
        elif i < 15:
            # Current/active campaigns
            start_date = now - timedelta(days=random.randint(0, 3))
            end_date = now + timedelta(days=random.randint(1, 10))
        else:
            # Future campaigns
            start_date = now + timedelta(days=random.randint(1, 5))
            end_date = start_date + timedelta(days=random.randint(3, 10))
        
        campaign_data = {
            "name": name,
            "status": "DRAFT",
            "startDate": start_date.strftime("%Y-%m-%dT%H:%M:%SZ"),
            "endDate": end_date.strftime("%Y-%m-%dT%H:%M:%SZ")
        }
        
        try:
            response = requests.post(f"{BASE_URL}/admin/campaigns", json=campaign_data)
            if response.status_code == 201:
                campaign = response.json()
                created_campaigns.append(campaign)
                print_success(f"Created campaign: {campaign['name']} (ID: {campaign['id']})")
            else:
                print_warning(f"Failed to create campaign {name}: {response.status_code}")
        except Exception as e:
            print_error(f"Error creating campaign: {e}")
    
    print_success(f"✓ Successfully created {len(created_campaigns)} campaigns")
    return created_campaigns

def create_games(campaigns, brands):
    """Create 5-6 games per campaign with varied durations"""
    print_header(f"Creating Games (5-6 per campaign)")
    created_games = []
    now = datetime.now(timezone.utc)
    
    for campaign in campaigns:
        num_games = random.randint(5, 6)
        # Parse datetime - handle both with and without 'Z' suffix
        start_date_str = campaign['startDate'].rstrip('Z')
        end_date_str = campaign['endDate'].rstrip('Z')
        campaign_start = datetime.fromisoformat(start_date_str).replace(tzinfo=timezone.utc)
        campaign_end = datetime.fromisoformat(end_date_str).replace(tzinfo=timezone.utc)
        
        for i in range(num_games):
            game_name = f"{random.choice(GAME_NAMES)} - {campaign['name'][:20]}"
            
            # Determine game duration
            # 20% of games end within 1-2 minutes for fast visualization
            is_quick_game = random.random() < 0.2
            
            if is_quick_game and campaign_start <= now <= campaign_end:
                # Quick game: starts now, ends in 1-2 minutes
                game_start = now
                game_end = now + timedelta(minutes=random.randint(1, 2))
                print_info(f"⚡ Quick game: {game_name} (ends in {(game_end - game_start).seconds // 60} min)")
            else:
                # Regular game: spans part of campaign duration
                game_duration = (campaign_end - campaign_start).total_seconds() / num_games
                game_start = campaign_start + timedelta(seconds=i * game_duration)
                game_end = game_start + timedelta(seconds=game_duration * 0.8)
            
            # Select 2-4 brands with budgets
            selected_brands = random.sample(brands, random.randint(2, 4))
            brand_budgets = [
                {
                    "brandId": brand['id'],
                    "totalBudget": random.randint(50, 500)
                }
                for brand in selected_brands
            ]
            
            game_data = {
                "name": game_name,
                "campaignId": campaign['id'],
                "status": "DRAFT",
                "startTime": game_start.strftime("%Y-%m-%dT%H:%M:%SZ"),
                "endTime": game_end.strftime("%Y-%m-%dT%H:%M:%SZ"),
                "probabilityType": "TIME_BASED",
                "slotGranularitySeconds": 5 if is_quick_game else random.choice([5, 10, 15, 30]),
                "brandBudgets": brand_budgets
            }
            
            try:
                response = requests.post(f"{BASE_URL}/admin/games", json=game_data)
                if response.status_code == 201:
                    game = response.json()
                    created_games.append(game)
                    
                    # Auto-start games that should be active now
                    if game_start <= now < game_end:
                        start_response = requests.post(f"{BASE_URL}/admin/games/{game['id']}/start")
                        if start_response.status_code == 200:
                            print_success(f"✓ Created & Started: {game['name'][:50]}... (ID: {game['id']})")
                        else:
                            print_warning(f"Created but failed to start game: {game['id']}")
                    else:
                        print_success(f"Created game: {game['name'][:50]}... (ID: {game['id']})")
                else:
                    print_warning(f"Failed to create game: {response.status_code}")
            except Exception as e:
                print_error(f"Error creating game: {e}")
        
        time.sleep(0.1)  # Small delay between campaigns
    
    print_success(f"✓ Successfully created {len(created_games)} games")
    return created_games

def get_active_games():
    """Fetch all active games"""
    try:
        response = requests.get(f"{BASE_URL}/admin/games")
        if response.status_code == 200:
            all_games = response.json()
            active_games = [g for g in all_games if g['status'] == 'ACTIVE']
            return active_games
        return []
    except Exception as e:
        print_error(f"Error fetching games: {e}")
        return []

def cleanup_all_data():
    """Clean up all existing data before creating new demo data"""
    print_header("🧹 CLEANING UP EXISTING DATA 🧹")
    print_warning("This will delete ALL users, brands, campaigns, games, and play events!")
    print_info("Waiting 3 seconds... (Ctrl+C to cancel)\n")
    time.sleep(3)
    
    deleted_counts = {
        'games': 0,
        'campaigns': 0,
        'brands': 0,
        'users': 0
    }
    
    # Delete in order: Games -> Campaigns -> Brands -> Users
    # (respects foreign key constraints)
    
    # 1. Delete all games
    print_info("Deleting games...")
    try:
        response = requests.get(f"{BASE_URL}/admin/games")
        if response.status_code == 200:
            games = response.json()
            for game in games:
                try:
                    del_response = requests.delete(f"{BASE_URL}/admin/games/{game['id']}")
                    if del_response.status_code in [200, 204]:
                        deleted_counts['games'] += 1
                except Exception as e:
                    print_warning(f"Failed to delete game {game['id']}: {e}")
            print_success(f"✓ Deleted {deleted_counts['games']} games")
    except Exception as e:
        print_warning(f"Could not fetch games: {e}")
    
    # 2. Delete all campaigns
    print_info("Deleting campaigns...")
    try:
        response = requests.get(f"{BASE_URL}/admin/campaigns")
        if response.status_code == 200:
            campaigns = response.json()
            for campaign in campaigns:
                try:
                    del_response = requests.delete(f"{BASE_URL}/admin/campaigns/{campaign['id']}")
                    if del_response.status_code in [200, 204]:
                        deleted_counts['campaigns'] += 1
                except Exception as e:
                    print_warning(f"Failed to delete campaign {campaign['id']}: {e}")
            print_success(f"✓ Deleted {deleted_counts['campaigns']} campaigns")
    except Exception as e:
        print_warning(f"Could not fetch campaigns: {e}")
    
    # 3. Delete all brands
    print_info("Deleting brands...")
    try:
        response = requests.get(f"{BASE_URL}/admin/brands")
        if response.status_code == 200:
            brands = response.json()
            for brand in brands:
                try:
                    del_response = requests.delete(f"{BASE_URL}/admin/brands/{brand['id']}")
                    if del_response.status_code in [200, 204]:
                        deleted_counts['brands'] += 1
                except Exception as e:
                    print_warning(f"Failed to delete brand {brand['id']}: {e}")
            print_success(f"✓ Deleted {deleted_counts['brands']} brands")
    except Exception as e:
        print_warning(f"Could not fetch brands: {e}")
    
    # 4. Delete all users
    print_info("Deleting users...")
    try:
        response = requests.get(f"{BASE_URL}/admin/users")
        if response.status_code == 200:
            users = response.json()
            for user in users:
                try:
                    del_response = requests.delete(f"{BASE_URL}/admin/users/{user['id']}")
                    if del_response.status_code in [200, 204]:
                        deleted_counts['users'] += 1
                except Exception as e:
                    print_warning(f"Failed to delete user {user['id']}: {e}")
            print_success(f"✓ Deleted {deleted_counts['users']} users")
    except Exception as e:
        print_warning(f"Could not fetch users: {e}")
    
    # Summary
    total_deleted = sum(deleted_counts.values())
    if total_deleted > 0:
        print_success(f"\n✓ Cleanup complete! Deleted {total_deleted} entities total")
        print_info("Note: Redis keys and Kafka messages will be cleaned on game restart")
        print_info("Note: Play events are cascade deleted with games")
    else:
        print_info("\n✓ No existing data to clean up")
    
    print_info("Waiting 2 seconds before creating new data...\n")
    time.sleep(2)

def main():
    print_header("🎮 FROLIC DEMO DATA GENERATOR 🎮")
    print_info("This script will create demo data for the Frolic platform")
    print_info(f"Target: {BASE_URL}\n")
    
    # Check if server is running
    try:
        response = requests.get(f"{BASE_URL.replace('/api/v1', '')}/actuator/health")
        if response.status_code != 200:
            print_error("Server is not healthy!")
            sys.exit(1)
        print_success("✓ Server is running and healthy\n")
    except Exception as e:
        print_error(f"Cannot connect to server: {e}")
        print_warning("Make sure the Frolic application is running on port 8080")
        sys.exit(1)
    
    # Clean up existing data
    cleanup_all_data()
    
    # Create all entities
    users = create_users(100)
    brands = create_brands()
    campaigns = create_campaigns(20)
    games = create_games(campaigns, brands)
    
    # Get active games count
    active_games = get_active_games()
    
    # Summary
    print_header("📊 SETUP COMPLETE 📊")
    print_success(f"Users created: {len(users)}")
    print_success(f"Brands created: {len(brands)}")
    print_success(f"Campaigns created: {len(campaigns)}")
    print_success(f"Games created: {len(games)}")
    print_success(f"Active games: {len(active_games)}")
    
    # Save IDs to file for simulation script
    data = {
        "user_ids": [u['id'] for u in users],
        "active_game_ids": [g['id'] for g in active_games],
        "all_game_ids": [g['id'] for g in games],
        "brand_ids": [b['id'] for b in brands],
        "campaign_ids": [c['id'] for c in campaigns]
    }
    
    with open('demo_data_ids.json', 'w') as f:
        json.dump(data, f, indent=2)
    
    print_info("\n💾 Saved IDs to 'demo_data_ids.json'")
    
    if active_games:
        print_header("🎯 READY FOR SIMULATION 🎯")
        print_info("Run the following command to simulate gameplay:")
        print(f"{Colors.OKGREEN}{Colors.BOLD}python3 simulate_gameplay.py{Colors.ENDC}\n")
    else:
        print_warning("\n⚠ No active games found. Adjust time ranges or start games manually.")

if __name__ == "__main__":
    main()
