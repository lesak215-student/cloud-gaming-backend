#!/usr/bin/env bash

BASE="http://localhost:8080"

echo "=== 1. PING CHECKS ==="
curl -s -i "$BASE/ping"
echo ""
curl -s -i -X POST "$BASE/ping"
echo ""

echo "=== 2. USER CRUD AND FILTERING ==="
echo "Creating Standard User (Alice):"
curl -s -i -X POST "$BASE/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","subscriptionTier":"BASIC"}'
echo ""

echo "Creating Premium User (Bob):"
curl -s -i -X POST "$BASE/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"Bob","email":"bob@example.com","subscriptionTier":"PREMIUM"}'
echo ""

echo "Creating User to Delete (Charlie):"
curl -s -i -X POST "$BASE/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"Charlie","email":"charlie@example.com","subscriptionTier":"BASIC"}'
echo ""

echo "List all users:"
curl -s -i "$BASE/users"
echo ""

echo "Filter users by tier (PREMIUM):"
curl -s -i "$BASE/users?tier=PREMIUM"
echo ""

echo "Get user by ID (Alice):"
curl -s -i "$BASE/users/1"
echo ""

echo "Update user by ID (Alice):"
curl -s -i -X PUT "$BASE/users/1" \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice Smith","email":"alice.smith@example.com","subscriptionTier":"BASIC"}'
echo ""

echo "Delete user by ID (Charlie):"
curl -s -i -X DELETE "$BASE/users/3"
echo ""

echo "Verify Charlie is deleted:"
curl -s -i "$BASE/users/3"
echo ""

echo "=== 3. GAME CRUD AND FILTERING ==="
echo "Creating BASIC game (CS2):"
curl -s -i -X POST "$BASE/games" \
  -H "Content-Type: application/json" \
  -d '{"title":"Counter-Strike 2","genre":"Shooter","requiredTier":"BASIC","maxSlots":2}'
echo ""

echo "Creating PREMIUM game (Cyberpunk 2077):"
curl -s -i -X POST "$BASE/games" \
  -H "Content-Type: application/json" \
  -d '{"title":"Cyberpunk 2077","genre":"RPG","requiredTier":"PREMIUM","maxSlots":1}'
echo ""

echo "Creating Game to Delete (Dota 2):"
curl -s -i -X POST "$BASE/games" \
  -H "Content-Type: application/json" \
  -d '{"title":"Dota 2","genre":"MOBA","requiredTier":"BASIC","maxSlots":1}'
echo ""

echo "List all games:"
curl -s -i "$BASE/games"
echo ""

echo "Filter games by genre (RPG):"
curl -s -i "$BASE/games?genre=RPG"
echo ""

echo "Filter games by tier (PREMIUM):"
curl -s -i "$BASE/games?tier=PREMIUM"
echo ""

echo "Filter games by hasSlots=true:"
curl -s -i "$BASE/games?hasSlots=true"
echo ""

echo "Get game by ID:"
curl -s -i "$BASE/games/1"
echo ""

echo "Update game by ID:"
curl -s -i -X PUT "$BASE/games/1" \
  -H "Content-Type: application/json" \
  -d '{"title":"Counter-Strike 2 Source","genre":"Shooter","requiredTier":"BASIC","maxSlots":3}'
echo ""

echo "Delete game by ID (Dota 2):"
curl -s -i -X DELETE "$BASE/games/3"
echo ""

echo "Verify Dota 2 is deleted:"
curl -s -i "$BASE/games/3"
echo ""

echo "=== 4. GAMING SESSIONS: DOMAIN RULES AND CRUD ==="
echo "Start session 1 (Alice playing CS2, 1080p):"
curl -s -i -X POST "$BASE/sessions" \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"gameId":1,"resolution":1080}'
echo ""

echo "Start session 2 (Bob playing Cyberpunk 2077, 2160p 4K):"
curl -s -i -X POST "$BASE/sessions" \
  -H "Content-Type: application/json" \
  -d '{"userId":2,"gameId":2,"resolution":2160}'
echo ""

echo "List all sessions:"
curl -s -i "$BASE/sessions"
echo ""

echo "Filter sessions by userId=1:"
curl -s -i "$BASE/sessions?userId=1"
echo ""

echo "Filter sessions by gameId=2:"
curl -s -i "$BASE/sessions?gameId=2"
echo ""

echo "Filter sessions by status=ACTIVE:"
curl -s -i "$BASE/sessions?status=ACTIVE"
echo ""

echo "Get session 1:"
curl -s -i "$BASE/sessions/1"
echo ""

echo "Pause session 1:"
curl -s -i -X POST "$BASE/sessions/1/pause"
echo ""

echo "Verify session 1 is PAUSED:"
curl -s -i "$BASE/sessions/1"
echo ""

echo "Resume session 1:"
curl -s -i -X POST "$BASE/sessions/1/resume"
echo ""

echo "Update session 1 resolution to 720p:"
curl -s -i -X PUT "$BASE/sessions/1" \
  -H "Content-Type: application/json" \
  -d '{"resolution":720}'
echo ""

echo "Terminate session 1:"
curl -s -i -X POST "$BASE/sessions/1/terminate"
echo ""

echo "Delete terminated session 1:"
curl -s -i -X DELETE "$BASE/sessions/1"
echo ""

echo "Verify session 1 deleted:"
curl -s -i "$BASE/sessions/1"
echo ""

echo "=== 5. ERROR SCENARIOS (400, 404, 405, 409) ==="

echo "--- 400: Empty request body on user creation ---"
curl -s -i -X POST "$BASE/users" \
  -H "Content-Type: application/json" \
  -d ''
echo ""

echo "--- 400: Invalid user email (missing @) ---"
curl -s -i -X POST "$BASE/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"Dave","email":"invalid-email","subscriptionTier":"BASIC"}'
echo ""

echo "--- 400: Blank user name ---"
curl -s -i -X POST "$BASE/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"","email":"dave@example.com","subscriptionTier":"BASIC"}'
echo ""

echo "--- 400: Invalid subscription tier ---"
curl -s -i -X POST "$BASE/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"Dave","email":"dave@example.com","subscriptionTier":"ULTRA"}'
echo ""

echo "--- 400: Non-integer ID in path ---"
curl -s -i "$BASE/users/abc"
echo ""

echo "--- 400: Negative ID in path ---"
curl -s -i "$BASE/users/-5"
echo ""

echo "--- 400: Strict boolean parsing error (not true/false) ---"
curl -s -i "$BASE/games?hasSlots=notabool"
echo ""

echo "--- 400: Invalid query tier enum ---"
curl -s -i "$BASE/users?tier=SUPER"
echo ""

echo "--- 404: User not found ---"
curl -s -i "$BASE/users/999"
echo ""

echo "--- 404: Game not found ---"
curl -s -i "$BASE/games/999"
echo ""

echo "--- 404: Session not found ---"
curl -s -i "$BASE/sessions/999"
echo ""

echo "--- 404: Unknown route ---"
curl -s -i "$BASE/unknown/route"
echo ""

echo "--- 405: Method not allowed (POST on item) ---"
curl -s -i -X POST "$BASE/users/1"
echo ""

echo "--- 409: Duplicate user email ---"
curl -s -i -X POST "$BASE/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"Bob Twin","email":"bob@example.com","subscriptionTier":"BASIC"}'
echo ""

echo "--- 409: Duplicate game title ---"
curl -s -i -X POST "$BASE/games" \
  -H "Content-Type: application/json" \
  -d '{"title":"Cyberpunk 2077","genre":"Action","requiredTier":"PREMIUM","maxSlots":5}'
echo ""

echo "--- 409: Standard user cannot launch PREMIUM game ---"
curl -s -i -X POST "$BASE/sessions" \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"gameId":2,"resolution":1080}'
echo ""

echo "--- 409: Resolution exceeds user subscription tier (Standard limited to 1080p) ---"
curl -s -i -X POST "$BASE/sessions" \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"gameId":1,"resolution":2160}'
echo ""

echo "--- 409: Game slots full (Cyberpunk has 1 slot, occupied by Bob) ---"
curl -s -i -X POST "$BASE/sessions" \
  -H "Content-Type: application/json" \
  -d '{"userId":2,"gameId":2,"resolution":2160}'
echo ""

echo "--- 409: User concurrent session limit exceeded (Bob already has active session on Cyberpunk, tries other games beyond limit) ---"
curl -s -i -X POST "$BASE/sessions" \
  -H "Content-Type: application/json" \
  -d '{"userId":2,"gameId":1,"resolution":1080}'
echo ""
curl -s -i -X POST "$BASE/sessions" \
  -H "Content-Type: application/json" \
  -d '{"userId":2,"gameId":1,"resolution":1080}'
echo ""
curl -s -i -X POST "$BASE/sessions" \
  -H "Content-Type: application/json" \
  -d '{"userId":2,"gameId":1,"resolution":1080}'
echo ""

echo "--- 409: Cannot change user tier while user has active session ---"
curl -s -i -X PUT "$BASE/users/2" \
  -H "Content-Type: application/json" \
  -d '{"name":"Bob","email":"bob@example.com","subscriptionTier":"BASIC"}'
echo ""

echo "--- 409: Cannot delete user with active session ---"
curl -s -i -X DELETE "$BASE/users/2"
echo ""

echo "--- 409: Cannot delete game with active session ---"
curl -s -i -X DELETE "$BASE/games/2"
echo ""

echo "--- 409: Cannot pause already paused session ---"
curl -s -i -X POST "$BASE/sessions/2/pause"
echo ""
curl -s -i -X POST "$BASE/sessions/2/pause"
echo ""

echo "--- 409: Cannot resume active session ---"
curl -s -i -X POST "$BASE/sessions/2/resume"
echo ""
curl -s -i -X POST "$BASE/sessions/2/resume"
echo ""

echo "--- 409: Cannot delete active or paused session (must terminate first) ---"
curl -s -i -X DELETE "$BASE/sessions/2"
echo ""

echo "All tests finished!"
