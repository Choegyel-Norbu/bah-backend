#!/bin/bash
# Run the app with local profile (loads application-local.yml for mail credentials).

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Load .env if present (mail, UploadThing token, etc.)
if [ -f .env ]; then
  set -a
  source .env
  set +a
  export UPLOADTHING_TOKEN
  echo "Loaded .env (UPLOADTHING_TOKEN is set for image uploads)"
fi

# Free port 8081 if already in use (e.g. from a previous run)
PID=$(lsof -ti:8081 2>/dev/null || true)
if [ -n "$PID" ]; then
  echo "Stopping process on port 8081 (PID $PID)..."
  kill -9 $PID 2>/dev/null || true
  sleep 2
fi

mvn clean spring-boot:run -Dspring-boot.run.profiles=local
