#!/bin/bash

# Configuration
TOTAL_REQUESTS=10000
PRODUCT_ID=1
ENDPOINT="http://localhost:8081/api/v1/orders/buy"

echo "🚀 Starting High-Throughput Stress Test: $TOTAL_REQUESTS requests..."

for i in $(seq 1 $TOTAL_REQUESTS)
do
   # Fire and forget (Background process)
   curl -s -X POST $ENDPOINT \
   -H "Content-Type: application/json" \
   -d "{\"userId\": \"bolt_user_$i\", \"productId\": $PRODUCT_ID}" > /dev/null &
done

wait
echo "✅ All requests dispatched. Check your Worker logs and Vue UI!"