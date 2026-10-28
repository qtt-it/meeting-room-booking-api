#!/usr/bin/env bash
set -euo pipefail
booking_base_url="${API_BASE_URL:-http://localhost:8080}"
booking_email="${BOOTSTRAP_ADMIN_EMAIL:-admin@example.com}"
booking_password="${BOOTSTRAP_ADMIN_PASSWORD:-LocalDemo!2026ChangeMe}"
curl --fail --silent "$booking_base_url/actuator/health/readiness" | jq -e '.status == "UP"' >/dev/null
booking_payload=$(jq -n --arg email "$booking_email" --arg password "$booking_password" '{email:$email,password:$password}')
booking_token=$(curl --fail --silent --request POST "$booking_base_url/api/v1/auth/login" --header 'Content-Type: application/json' --data "$booking_payload" | jq -er '.data.accessToken')
curl --fail --silent "$booking_base_url/api/v1/users/me" --header "Authorization: Bearer $booking_token" | jq -e '.data.role == "admin"' >/dev/null
curl --fail --silent "$booking_base_url/api/v1/rooms" --header "Authorization: Bearer $booking_token" | jq -e '.success == true' >/dev/null
echo "Smoke checks passed"
