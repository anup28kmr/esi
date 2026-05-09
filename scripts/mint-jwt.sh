#!/usr/bin/env bash
# Mint a development JWT for the QuickBite stack without Python.
# Requires only `openssl` and `base64`, both of which ship with Git
# Bash on Windows and every mainstream Linux/Mac shell.
#
# The token is signed HS256 with the dev secret pinned in
# `application.properties` (`jwt.secret`), which both Sierra-Lima
# services base64-decode at boot (see
# `JwtAuthFilter.java:40 -- Keys.hmacShaKeyFor(Decoders.BASE64.decode(...))`).
# Issuer is the same `quickbite-user-service` value the services pin
# via `requireIssuer(...)`, so wrong-issuer tokens still fail with 401.
#
# Usage:
#   scripts/mint-jwt.sh              # default: owner-1 (owns d0000001/d0000002)
#   scripts/mint-jwt.sh owner-1      # explicit owner-1
#   scripts/mint-jwt.sh owner-2      # owns d0000003 (closed) + d0000004
#   scripts/mint-jwt.sh owner-3      # owns d0000005 + d0000006 (closed)
#   scripts/mint-jwt.sh customer     # Customer role (no ownership)
#   scripts/mint-jwt.sh admin        # Admin role (bypasses ownership)
#   scripts/mint-jwt.sh custom <userUuid> <role>
#
# Pipe straight into curl:
#   JWT=$(scripts/mint-jwt.sh owner-1)
#   curl -H "Authorization: Bearer $JWT" http://localhost:8080/restaurants/.../availability

set -euo pipefail

# Decoded form of `jwt.secret` in application.properties. Same value
# in restaurant-service and menu-service. Keep in sync if it rotates.
SECRET_TEXT="test-secret-for-dev-only-do-not-use-in-prod-123456"
ISSUER="quickbite-user-service"
TTL_SECONDS=3600

case "${1:-owner-1}" in
  owner-1)  SUB="00000000-0000-0000-0000-000000000001"; ROLE="RestaurantOwner" ;;
  owner-2)  SUB="00000000-0000-0000-0000-000000000002"; ROLE="RestaurantOwner" ;;
  owner-3)  SUB="00000000-0000-0000-0000-000000000003"; ROLE="RestaurantOwner" ;;
  customer) SUB="00000000-0000-0000-0000-0000000000c1"; ROLE="Customer" ;;
  admin)    SUB="00000000-0000-0000-0000-0000000000a1"; ROLE="Admin" ;;
  custom)
    SUB="${2:?custom profile requires <userUuid> <role>}"
    ROLE="${3:?custom profile requires <userUuid> <role>}"
    ;;
  -h|--help)
    sed -n '2,28p' "$0"
    exit 0
    ;;
  *)
    echo "Unknown profile: $1 (try owner-1|owner-2|owner-3|customer|admin|custom)" >&2
    exit 2
    ;;
esac

# base64url, no padding -- the JWT spec requires this variant.
b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

now=$(date +%s)
exp=$((now + TTL_SECONDS))

header='{"alg":"HS256","typ":"JWT"}'
payload="{\"iss\":\"${ISSUER}\",\"sub\":\"${SUB}\",\"userId\":\"${SUB}\",\"role\":\"${ROLE}\",\"tokenType\":\"USER\",\"iat\":${now},\"exp\":${exp}}"

h=$(printf '%s' "$header"  | b64url)
p=$(printf '%s' "$payload" | b64url)
signing_input="${h}.${p}"

sig=$(printf '%s' "$signing_input" \
  | openssl dgst -sha256 -hmac "$SECRET_TEXT" -binary \
  | b64url)

printf '%s.%s\n' "$signing_input" "$sig"
