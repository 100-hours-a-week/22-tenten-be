#!/bin/bash

echo "🔍 Starting backend health check..."

URL="http://localhost:8080/actuator/health"
RETRY=10
SLEEP=5

for ((i=1; i<=RETRY; i++)); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$URL")

  if [ "$STATUS" -eq 200 ]; then
    echo "✅ Health check passed."
    exit 0
  fi

  echo "⏳ Attempt $i/$RETRY failed with status: $STATUS. Retrying in $SLEEP seconds..."
  sleep $SLEEP
done

echo "❌ Health check failed after $RETRY attempts. Exiting with error."
exit 1
