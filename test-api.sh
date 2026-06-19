#!/bin/bash

BASE_URL="http://localhost:8080"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}=== Seat Reservation Platform - API Test ===${NC}\n"

WEBHOOK_SECRET=${WEBHOOK_SECRET:-dev-secret}

echo -e "${GREEN}1. Registering user...${NC}"
UNIQUE_EMAIL="test@example.com"
PASSWORD="password123"
REGISTER=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$UNIQUE_EMAIL\",\"password\":\"$PASSWORD\"}")
echo "Response:"
echo "$REGISTER" | jq . || echo "$REGISTER"

echo -e "${GREEN}2. Logging in...${NC}"
LOGIN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$UNIQUE_EMAIL\",\"password\":\"$PASSWORD\"}")
echo "Response:"
echo "$LOGIN" | jq . || echo "$LOGIN"
TOKEN=$(echo "$LOGIN" | jq -r .token 2>/dev/null || true)
echo -e "Token: ${TOKEN:-<none>}\n"

echo -e "${GREEN}3. Getting available seats...${NC}"
curl -s -X GET "$BASE_URL/api/seats" \
  -H "Authorization: Bearer $TOKEN" | jq .
echo ""

echo -e "${GREEN}4. Reserving a seat (first available)...${NC}"
SEAT_ID=$(curl -s -X GET "$BASE_URL/api/seats" -H "Authorization: Bearer $TOKEN" | jq 'map(select(.status == "AVAILABLE"))[0].id' 2>/dev/null || true)
if [ -z "$SEAT_ID" ] || [ "$SEAT_ID" = "null" ]; then
  echo "No available seat found or failed to fetch seats. Exiting."
  exit 1
fi
RESERVATION=$(curl -s -X POST "$BASE_URL/api/reservations" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"seatId\":$SEAT_ID}")
echo "Response:"
echo "$RESERVATION" | jq . || echo "$RESERVATION"
RESERVATION_ID=$(echo "$RESERVATION" | jq -r .id 2>/dev/null || true)
echo -e "Reservation ID: ${RESERVATION_ID:-<none>}\n"

echo -e "${GREEN}5. Getting user reservations...${NC}"
curl -s -X GET "$BASE_URL/api/reservations" \
  -H "Authorization: Bearer $TOKEN" | jq .
echo ""

echo -e "${GREEN}6. Initiating payment...${NC}"
PAYMENT=$(curl -s -X POST "$BASE_URL/api/payments?reservationId=$RESERVATION_ID" \
  -H "Authorization: Bearer $TOKEN")
echo "Response:"
echo "$PAYMENT" | jq . || echo "$PAYMENT"
PROVIDER_REF=$(echo "$PAYMENT" | jq -r .provider_reference 2>/dev/null || true)
echo -e "Provider Reference: ${PROVIDER_REF:-<none>}\n"

echo -e "${GREEN}7. Simulating payment success webhook...${NC}"
EVENT_ID="evt_$(date +%s)"
WEBHOOK_BODY=$(jq -n --arg eventId "$EVENT_ID" --arg providerRef "$PROVIDER_REF" '{eventId: $eventId, providerReference: $providerRef}')
SIGNATURE=$(echo -n "$WEBHOOK_BODY" | openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" -hex | awk '{print $2}')
WEBHOOK=$(curl -s -X POST "$BASE_URL/api/webhooks/payment-success" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Signature: $SIGNATURE" \
  -H "Content-Type: application/json" \
  -d "$WEBHOOK_BODY")
echo "Response:"
echo "$WEBHOOK" | jq . || echo "$WEBHOOK"

echo -e "${GREEN}8. Getting reservations after payment...${NC}"
curl -s -X GET "$BASE_URL/api/reservations" \
  -H "Authorization: Bearer $TOKEN" | jq .
echo ""

echo -e "${GREEN}=== Test Complete ===${NC}"

