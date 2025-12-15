#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STATE_DIR="$ROOT_DIR/scripts/.poc"
LOG_DIR="$STATE_DIR/logs"

kill_if_running() {
  local pid_file="$1"
  local name="$2"
  if [[ ! -f "$pid_file" ]]; then
    return 0
  fi
  local pid
  pid="$(cat "$pid_file" 2>/dev/null || true)"
  if [[ -z "$pid" ]]; then
    rm -f "$pid_file"
    return 0
  fi

  if kill -0 "$pid" 2>/dev/null; then
    echo "Stopping $name (pid=$pid)..."
    kill "$pid" 2>/dev/null || true

    for _ in {1..30}; do
      if ! kill -0 "$pid" 2>/dev/null; then
        break
      fi
      sleep 1
    done

    if kill -0 "$pid" 2>/dev/null; then
      echo "$name still running; force killing (pid=$pid)..."
      kill -9 "$pid" 2>/dev/null || true
    fi
  fi

  rm -f "$pid_file"
}

mkdir -p "$STATE_DIR" "$LOG_DIR"

kill_if_running "$STATE_DIR/wallet.pid" "wallet"
kill_if_running "$STATE_DIR/verifier.pid" "verifier"
kill_if_running "$STATE_DIR/issuer.pid" "issuer"

echo "Stopping Postgres via docker compose..."
cd "$ROOT_DIR"
docker compose down

echo "Stopped. Logs (if needed): $LOG_DIR"
