#!/usr/bin/env bash
# Build, start and check forum + admin. Same script on the Mac and on the Synology.
# Usage: ./deploy.sh            (pull, build, start, check)
#        ./deploy.sh --tunnel   (also start cloudflared; needs CLOUDFLARE_TUNNEL_TOKEN in .env)
#        SKIP_PULL=1 ./deploy.sh (no git pull)
set -euo pipefail
cd "$(dirname "$0")"

FORUM=${FORUM_URL:-http://localhost:8080}
ADMIN=${ADMIN_URL:-http://localhost:8081}
compose=(docker compose)

fail() { echo "FAIL: $*" >&2; exit 1; }

[ -f .env ] || fail ".env is missing. Run: cp .env.example .env, then set FORUM_JWT_SECRET (openssl rand -base64 48)"
grep -Eq '^FORUM_JWT_SECRET=.{32,}' .env || fail "FORUM_JWT_SECRET in .env must have at least 32 characters"

if [ "${1:-}" = "--tunnel" ]; then
  grep -Eq '^CLOUDFLARE_TUNNEL_TOKEN=.+' .env || fail "CLOUDFLARE_TUNNEL_TOKEN in .env is empty"
  compose+=(--profile tunnel)
fi

if [ -z "${SKIP_PULL:-}" ]; then
  git pull --ff-only
fi

mkdir -p data
"${compose[@]}" up -d --build

# Wait for the API (max 120 s).
for _ in $(seq 1 120); do
  [ "$(curl -s -o /dev/null -w '%{http_code}' "$FORUM/topics")" = 200 ] && break
  sleep 1
done
[ "$(curl -s -o /dev/null -w '%{http_code}' "$FORUM/topics")" = 200 ] || fail "GET /topics is not 200. See: docker compose logs forum"
echo "ok   GET /topics 200"

curl -s -X POST "$FORUM/auth" -H 'Content-Type: application/json' \
  -d '{"email":"aluno@email.com","password":"123456"}' | grep -q '"token"' \
  || fail "POST /auth gave no token"
echo "ok   POST /auth token"

[ "$(curl -s -o /dev/null -L -w '%{http_code}' "$FORUM/swagger-ui.html")" = 200 ] || fail "swagger-ui.html is not 200"
echo "ok   swagger-ui.html 200"

# The admin needs some seconds to see the forum as UP (max 60 s).
for _ in $(seq 1 60); do
  curl -s -H 'Accept: application/json' "$ADMIN/applications" | grep -q '"name":"forum","status":"UP"' && break
  sleep 1
done
curl -s -H 'Accept: application/json' "$ADMIN/applications" | grep -q '"name":"forum","status":"UP"' \
  || fail "admin does not show forum UP. See: docker compose logs admin forum"
echo "ok   admin shows forum UP"

echo "Deploy OK: forum $FORUM, admin $ADMIN"
