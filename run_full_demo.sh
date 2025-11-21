#!/bin/bash

# Full Demo Script - Sets up data and runs gameplay simulation

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

echo -e "${BOLD}${CYAN}"
echo "════════════════════════════════════════════════════════════════"
echo "           🎮 FROLIC FULL DEMO AUTOMATION 🎮                    "
echo "════════════════════════════════════════════════════════════════"
echo -e "${NC}"

# Check if Python 3 is installed
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}✗ Python 3 is not installed${NC}"
    exit 1
fi

# Check if requests library is installed
python3 -c "import requests" 2>/dev/null
if [ $? -ne 0 ]; then
    echo -e "${YELLOW}⚠ 'requests' library not found. Installing...${NC}"
    pip3 install requests
fi

# Check if server is running
echo -e "${CYAN}ℹ Checking if Frolic server is running...${NC}"
if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo -e "${RED}✗ Frolic server is not running on port 8080${NC}"
    echo -e "${YELLOW}Please start the server first:${NC}"
    echo -e "  docker-compose up -d"
    echo -e "  mvn clean package -DskipTests"
    echo -e "  java -jar frolic-services/target/frolic-services-1.0-SNAPSHOT.jar"
    exit 1
fi
echo -e "${GREEN}✓ Server is running${NC}\n"

# Step 1: Setup demo data
echo -e "${BOLD}${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BOLD}${BLUE}STEP 1: Setting up demo data${NC}"
echo -e "${BOLD}${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

python3 setup_demo_data.py

if [ $? -ne 0 ]; then
    echo -e "\n${RED}✗ Demo data setup failed${NC}"
    exit 1
fi

echo -e "\n${GREEN}✓ Demo data setup completed successfully${NC}"

# Wait a bit for games to start
echo -e "\n${CYAN}ℹ Waiting 3 seconds for games to initialize...${NC}"
sleep 3

# Step 2: Run gameplay simulation
echo -e "\n${BOLD}${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BOLD}${BLUE}STEP 2: Running gameplay simulation${NC}"
echo -e "${BOLD}${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

python3 simulate_gameplay.py

if [ $? -ne 0 ]; then
    echo -e "\n${YELLOW}⚠ Simulation ended (may have been interrupted)${NC}"
fi

# Done
echo -e "\n${BOLD}${GREEN}"
echo "════════════════════════════════════════════════════════════════"
echo "                   ✨ DEMO COMPLETE ✨                          "
echo "════════════════════════════════════════════════════════════════"
echo -e "${NC}"

echo -e "${CYAN}You can now:${NC}"
echo -e "  • Check PostgreSQL UI: ${BLUE}http://localhost:8083${NC}"
echo -e "  • View Redis data: ${BLUE}http://localhost:8081${NC}"
echo -e "  • Browse Kafka messages: ${BLUE}http://localhost:8082${NC}"
echo -e "  • Re-run simulation: ${GREEN}python3 simulate_gameplay.py${NC}"
echo ""
