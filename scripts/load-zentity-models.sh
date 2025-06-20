#!/bin/bash

# Script to load Zentity models into OpenSearch
set -e

OPENSEARCH_URL=${1:-"http://localhost:9200"}
MODELS_DIR=${2:-"/opt/zentity/sandbox/models"}

echo "=== Loading Zentity Models ==="
echo "OpenSearch URL: $OPENSEARCH_URL"
echo "Models directory: $MODELS_DIR"

# Function to wait for OpenSearch to be ready
wait_for_opensearch() {
    local url="$1"
    local max_attempts=30
    local attempt=1
    
    echo "Waiting for OpenSearch to be ready..."
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url/_cluster/health" > /dev/null 2>&1; then
            echo "✓ OpenSearch is ready"
            return 0
        fi
        
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo "✗ OpenSearch failed to be ready within timeout"
    return 1
}

# Function to check if Zentity plugin is available
check_zentity_plugin() {
    local url="$1"
    
    echo "Checking Zentity plugin availability..."
    if curl -s "$url/_zentity" > /dev/null 2>&1; then
        echo "✓ Zentity plugin is available"
        return 0
    else
        echo "⚠ Zentity plugin not available yet"
        return 1
    fi
}

# Function to load a single model
load_model() {
    local model_file="$1"
    local model_name="$2"
    local url="$3"
    
    echo "Loading model: $model_name from $model_file"
    
    if [ ! -f "$model_file" ]; then
        echo "✗ Model file not found: $model_file"
        return 1
    fi
    
    local response=$(curl -s -w "%{http_code}" -X PUT "$url/_zentity/models/$model_name" \
        -H "Content-Type: application/json" \
        -d @"$model_file")
    
    local http_code="${response: -3}"
    local body="${response%???}"
    
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        echo "✓ Model $model_name loaded successfully"
        return 0
    else
        echo "✗ Failed to load model $model_name (HTTP $http_code)"
        echo "Response: $body"
        return 1
    fi
}

# Main execution
main() {
    # Wait for OpenSearch to be ready
    if ! wait_for_opensearch "$OPENSEARCH_URL"; then
        exit 1
    fi
    
    # Wait a bit more for Zentity plugin to be available
    sleep 5
    
    # Check if Zentity plugin is available
    if ! check_zentity_plugin "$OPENSEARCH_URL"; then
        echo "⚠ Zentity plugin not available, skipping model loading"
        echo "Run this script again after installing the Zentity plugin"
        exit 0
    fi
    
    # Load models
    local models_loaded=0
    local models_failed=0
    
    if [ -f "$MODELS_DIR/organization.json" ]; then
        if load_model "$MODELS_DIR/organization.json" "organization" "$OPENSEARCH_URL"; then
            models_loaded=$((models_loaded + 1))
        else
            models_failed=$((models_failed + 1))
        fi
    else
        echo "⚠ Organization model not found: $MODELS_DIR/organization.json"
    fi
    
    if [ -f "$MODELS_DIR/person.json" ]; then
        if load_model "$MODELS_DIR/person.json" "person" "$OPENSEARCH_URL"; then
            models_loaded=$((models_loaded + 1))
        else
            models_failed=$((models_failed + 1))
        fi
    else
        echo "⚠ Person model not found: $MODELS_DIR/person.json"
    fi
    
    echo ""
    echo "=== Model Loading Summary ==="
    echo "Models loaded: $models_loaded"
    echo "Models failed: $models_failed"
    
    if [ $models_loaded -gt 0 ]; then
        echo ""
        echo "Available models:"
        curl -s "$OPENSEARCH_URL/_zentity/models" | \
            jq -r '.hits.hits[]._id' 2>/dev/null || \
            echo "Unable to list models (jq not available)"
    fi
    
    if [ $models_failed -gt 0 ]; then
        exit 1
    fi
}

# Run the main function
main "$@" 