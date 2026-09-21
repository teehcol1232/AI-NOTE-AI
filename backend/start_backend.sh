#!/bin/bash
PID=$(pgrep -f "node /app/applet/backend/server.js" || true)
if [ -z "$PID" ]; then
    echo "Starting backend server..."
    nohup node /app/applet/backend/server.js > /tmp/backend.log 2>&1 &
    sleep 1
    echo "Started backend (PID $(pgrep -f "node /app/applet/backend/server.js"))"
else
    echo "Backend is already running (PID $PID)"
fi
