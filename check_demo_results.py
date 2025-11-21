#!/usr/bin/env python3
"""
Demo Results Checker
Quick script to view demo statistics and game results
"""

import requests
import json
from datetime import datetime

BASE_URL = "http://localhost:8080/api/v1"

class Colors:
    OKGREEN = '\033[92m'
    OKCYAN = '\033[96m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

def print_section(title):
    print(f"\n{Colors.BOLD}{Colors.OKCYAN}{'='*70}{Colors.ENDC}")
    print(f"{Colors.BOLD}{Colors.OKCYAN}{title:^70}{Colors.ENDC}")
    print(f"{Colors.BOLD}{Colors.OKCYAN}{'='*70}{Colors.ENDC}\n")

def get_stats():
    """Fetch and display demo statistics"""
    
    # Get all entities
    try:
        users = requests.get(f"{BASE_URL}/admin/users").json()
        brands = requests.get(f"{BASE_URL}/admin/brands").json()
        campaigns = requests.get(f"{BASE_URL}/admin/campaigns").json()
        games = requests.get(f"{BASE_URL}/admin/games").json()
    except Exception as e:
        print(f"{Colors.FAIL}Error connecting to server: {e}{Colors.ENDC}")
        return
    
    # Count active games
    active_games = [g for g in games if g['status'] == 'ACTIVE']
    draft_games = [g for g in games if g['status'] == 'DRAFT']
    stopped_games = [g for g in games if g['status'] == 'STOPPED']
    
    # Display summary
    print_section("📊 DEMO DATA SUMMARY")
    
    print(f"{Colors.BOLD}Total Entities:{Colors.ENDC}")
    print(f"  👥 Users: {Colors.OKGREEN}{len(users)}{Colors.ENDC}")
    print(f"  🏷️  Brands: {Colors.OKGREEN}{len(brands)}{Colors.ENDC}")
    print(f"  📢 Campaigns: {Colors.OKGREEN}{len(campaigns)}{Colors.ENDC}")
    print(f"  🎮 Games: {Colors.OKGREEN}{len(games)}{Colors.ENDC}")
    
    print(f"\n{Colors.BOLD}Game Status:{Colors.ENDC}")
    print(f"  ✅ Active: {Colors.OKGREEN}{len(active_games)}{Colors.ENDC}")
    print(f"  📝 Draft: {Colors.WARNING}{len(draft_games)}{Colors.ENDC}")
    print(f"  🛑 Stopped: {Colors.FAIL}{len(stopped_games)}{Colors.ENDC}")
    
    # Show active games
    if active_games:
        print_section("🎯 ACTIVE GAMES")
        for i, game in enumerate(active_games[:10], 1):
            print(f"{i:2}. {game['name'][:50]}")
            print(f"    ID: {game['id']}")
            print(f"    Type: {game.get('probabilityType', 'N/A')}")
            print(f"    Ends: {game.get('endTime', 'N/A')}")
            print()
        
        if len(active_games) > 10:
            print(f"    ... and {len(active_games) - 10} more\n")
    
    # Show brands
    print_section("🏷️ BRANDS")
    for i, brand in enumerate(brands, 1):
        status = f"{Colors.OKGREEN}Active{Colors.ENDC}" if brand.get('active') else f"{Colors.FAIL}Inactive{Colors.ENDC}"
        print(f"{i:2}. {brand['name']:20} | {status} | {brand.get('description', 'N/A')[:40]}")
    
    # Show sample users
    print_section("👥 SAMPLE USERS")
    for i, user in enumerate(users[:10], 1):
        status = f"{Colors.OKGREEN}✓{Colors.ENDC}" if user.get('active') else f"{Colors.FAIL}✗{Colors.ENDC}"
        print(f"{i:2}. {status} {user.get('name', 'N/A'):20} | {user.get('email', 'N/A')}")
    
    if len(users) > 10:
        print(f"\n    ... and {len(users) - 10} more users\n")
    
    # Quick game lookup helper
    print_section("🔍 QUICK LOOKUP")
    print(f"{Colors.BOLD}Get game details:{Colors.ENDC}")
    print(f"  curl http://localhost:8080/api/v1/admin/games/{{GAME_ID}}\n")
    
    print(f"{Colors.BOLD}Start a game:{Colors.ENDC}")
    print(f"  curl -X POST http://localhost:8080/api/v1/admin/games/{{GAME_ID}}/start\n")
    
    print(f"{Colors.BOLD}Play a game:{Colors.ENDC}")
    print(f"  curl -X POST http://localhost:8080/api/v1/play \\")
    print(f"    -H 'Content-Type: application/json' \\")
    if users:
        print(f"    -d '{{\"userId\":\"{users[0]['id']}\",\"gameId\":\"{{GAME_ID}}\"}}'")
    else:
        print(f"    -d '{{\"userId\":\"USER_ID\",\"gameId\":\"GAME_ID\"}}'")
    
    # System health
    print_section("💚 SYSTEM HEALTH")
    try:
        health = requests.get(f"{BASE_URL.replace('/api/v1', '')}/actuator/health").json()
        status = health.get('status', 'UNKNOWN')
        if status == 'UP':
            print(f"{Colors.OKGREEN}✓ System is healthy{Colors.ENDC}\n")
        else:
            print(f"{Colors.WARNING}⚠ System status: {status}{Colors.ENDC}\n")
    except:
        print(f"{Colors.FAIL}✗ Cannot reach health endpoint{Colors.ENDC}\n")
    
    print(f"{Colors.BOLD}Web UIs:{Colors.ENDC}")
    print(f"  🐘 PostgreSQL: {Colors.OKCYAN}http://localhost:8083{Colors.ENDC}")
    print(f"  🔴 Redis:      {Colors.OKCYAN}http://localhost:8081{Colors.ENDC}")
    print(f"  📨 Kafka:      {Colors.OKCYAN}http://localhost:8082{Colors.ENDC}")
    print()

def main():
    print(f"\n{Colors.BOLD}🎮 Frolic Demo Results Checker 🎮{Colors.ENDC}")
    get_stats()
    print(f"\n{Colors.OKGREEN}✓ Check complete!{Colors.ENDC}\n")

if __name__ == "__main__":
    main()
