#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STATE_DIR="$ROOT_DIR/scripts/.poc"
LOG_DIR="$STATE_DIR/logs"

mkdir -p "$LOG_DIR"

is_running() {
  local pid_file="$1"
  if [[ ! -f "$pid_file" ]]; then
    return 1
  fi
  local pid
  pid="$(cat "$pid_file" 2>/dev/null || true)"
  if [[ -z "$pid" ]]; then
    return 1
  fi
  if kill -0 "$pid" 2>/dev/null; then
    return 0
  fi
  return 1
}

cleanup_stale_pid() {
  local pid_file="$1"
  if [[ -f "$pid_file" ]] && ! is_running "$pid_file"; then
    rm -f "$pid_file"
  fi
}

cleanup_stale_pid "$STATE_DIR/issuer.pid"
cleanup_stale_pid "$STATE_DIR/verifier.pid"
cleanup_stale_pid "$STATE_DIR/wallet.pid"

if is_running "$STATE_DIR/issuer.pid" || is_running "$STATE_DIR/verifier.pid" || is_running "$STATE_DIR/wallet.pid"; then
  echo "PoC appears to already be running. If you want a clean restart, run scripts/stop-poc.command first."
  exit 0
fi

cd "$ROOT_DIR"

echo "[1/4] Starting Postgres via docker compose..."
docker compose up -d

echo "[2/4] Starting Issuer (8080)..."
(
  cd "$ROOT_DIR/issuer"
  nohup ./mvnw -DskipTests spring-boot:run > "$LOG_DIR/issuer.log" 2>&1 &
  echo $! > "$STATE_DIR/issuer.pid"
)

echo "Waiting for Issuer to be ready..."
ISSUER_READY=0
for _ in {1..120}; do
  if curl -sf http://localhost:8080/jwks.json >/dev/null 2>&1; then
    ISSUER_READY=1
    break
  fi
  sleep 1
done
if [[ "$ISSUER_READY" != "1" ]]; then
  echo "Issuer did not become ready in time. Check logs: $LOG_DIR/issuer.log"
  exit 1
fi

ISSUER_JWKS="$(curl -s http://localhost:8080/jwks.json | tr -d '\n')"
if [[ -z "$ISSUER_JWKS" ]]; then
  echo "Failed to fetch issuer JWKS."
  exit 1
fi

echo "[3/4] Starting Verifier + Payment API (8090)..."
(
  cd "$ROOT_DIR"
  nohup env TRUSTED_ISSUER_PUBLIC_JWK="$ISSUER_JWKS" mvn -f verifier/pom.xml -DskipTests spring-boot:run > "$LOG_DIR/verifier.log" 2>&1 &
  echo $! > "$STATE_DIR/verifier.pid"
)

echo "Waiting for Verifier to be ready..."
VERIFIER_READY=0
for _ in {1..120}; do
  if curl -sf http://localhost:8090/verify/challenge >/dev/null 2>&1; then
    VERIFIER_READY=1
    break
  fi
  sleep 1
done
if [[ "$VERIFIER_READY" != "1" ]]; then
  echo "Verifier did not become ready in time. Check logs: $LOG_DIR/verifier.log"
  exit 1
fi

echo "[4/4] Starting Wallet (3000)..."
(
  cd "$ROOT_DIR/wallet"
  if [[ ! -d node_modules ]]; then
    npm install
  fi
  nohup npm start > "$LOG_DIR/wallet.log" 2>&1 &
  echo $! > "$STATE_DIR/wallet.pid"
)

echo "Waiting for Wallet UI to be ready..."
WALLET_READY=0
for _ in {1..180}; do
  if curl -sf http://localhost:3000 >/dev/null 2>&1; then
    WALLET_READY=1
    break
  fi
  sleep 1
done
if [[ "$WALLET_READY" != "1" ]]; then
  echo "Wallet did not become ready in time. Check logs: $LOG_DIR/wallet.log"
  exit 1
fi

open http://localhost:3000

echo ""
echo "PoC is up."
echo "- Issuer:   http://localhost:8080"
echo "- Verifier: http://localhost:8090"
echo "- Wallet:   http://localhost:3000"
echo ""
echo "Logs: $LOG_DIR"
echo "To stop everything: scripts/stop-poc.command"
