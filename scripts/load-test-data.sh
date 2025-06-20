#!/bin/bash

# Load test data into Elasticsearch or OpenSearch
# Usage: ./load-test-data.sh [elasticsearch|opensearch]

set -e

TARGET=${1:-elasticsearch}

if [ "$TARGET" = "elasticsearch" ]; then
    BASE_URL="http://localhost:9200"
    CONTAINER_NAME="zentity-elasticsearch"
elif [ "$TARGET" = "opensearch" ]; then
    BASE_URL="http://localhost:9201"
    CONTAINER_NAME="zentity-opensearch"
else
    echo "Error: Target must be 'elasticsearch' or 'opensearch'"
    exit 1
fi

echo "Loading test data into ${TARGET} at ${BASE_URL}..."

# Check if service is running
if ! curl -s "${BASE_URL}/_cluster/health" > /dev/null; then
    echo "Error: ${TARGET} is not accessible at ${BASE_URL}"
    echo "Make sure it's running with: docker-compose -f docker-compose.dev.yml up ${TARGET}"
    exit 1
fi

# Setup Zentity models index
echo "Setting up Zentity models index..."
curl -X POST "${BASE_URL}/_zentity/_setup" -H "Content-Type: application/json" || true

# Load test indices
echo "Creating test indices..."

# Main test index
curl -X PUT "${BASE_URL}/zentity_test_index" \
    -H "Content-Type: application/json" \
    -d @src/test/resources/TestIndex.json

# Array test index
curl -X PUT "${BASE_URL}/zentity_test_index_arrays" \
    -H "Content-Type: application/json" \
    -d @src/test/resources/TestIndexArrays.json

# Object arrays test index
curl -X PUT "${BASE_URL}/zentity_test_index_object_arrays" \
    -H "Content-Type: application/json" \
    -d @src/test/resources/TestIndexObjectArrays.json

# Load entity models
echo "Loading entity models..."

# Main entity models
curl -X POST "${BASE_URL}/_zentity/models/zentity_test_entity_a" \
    -H "Content-Type: application/json" \
    -d @src/test/resources/TestEntityModelA.json

curl -X POST "${BASE_URL}/_zentity/models/zentity_test_entity_b" \
    -H "Content-Type: application/json" \
    -d @src/test/resources/TestEntityModelB.json

# Array entity model
curl -X POST "${BASE_URL}/_zentity/models/zentity_test_entity_arrays" \
    -H "Content-Type: application/json" \
    -d @src/test/resources/TestEntityModelArrays.json

# Object arrays entity model
curl -X POST "${BASE_URL}/_zentity/models/zentity_test_entity_object_arrays" \
    -H "Content-Type: application/json" \
    -d @src/test/resources/TestEntityModelObjectArrays.json

# Error test models
curl -X POST "${BASE_URL}/_zentity/models/zentity_test_entity_elasticsearch_error" \
    -H "Content-Type: application/json" \
    -d @src/test/resources/TestEntityModelElasticsearchError.json

curl -X POST "${BASE_URL}/_zentity/models/zentity_test_entity_zentity_error" \
    -H "Content-Type: application/json" \
    -d @src/test/resources/TestEntityModelZentityError.json

# Load test data
echo "Loading test data..."

# Load main test data
curl -X POST "${BASE_URL}/_bulk?refresh=true" \
    -H "Content-Type: application/x-ndjson" \
    --data-binary @src/test/resources/TestData.txt

# Load arrays test data
curl -X POST "${BASE_URL}/_bulk?refresh=true" \
    -H "Content-Type: application/x-ndjson" \
    --data-binary @src/test/resources/TestDataArrays.txt

# Load object arrays test data
curl -X POST "${BASE_URL}/_bulk?refresh=true" \
    -H "Content-Type: application/x-ndjson" \
    --data-binary @src/test/resources/TestDataObjectArrays.txt

echo ""
echo "Test data loading complete!"
echo ""
echo "Available endpoints:"
echo "  Health:           ${BASE_URL}/_cluster/health"
echo "  Zentity Home:     ${BASE_URL}/_zentity"
echo "  Entity Models:    ${BASE_URL}/_zentity/models"
echo "  Test Indices:     ${BASE_URL}/_cat/indices/zentity_test_*"
echo ""
echo "Example entity resolution query:"
echo "  curl -X POST '${BASE_URL}/_zentity/resolution/zentity_test_entity_a' \\"
echo "       -H 'Content-Type: application/json' \\"
echo "       -d '{\"attributes\":{\"attribute_a\":[\"a1\"]}}'"
echo ""

if [ "$TARGET" = "elasticsearch" ]; then
    echo "Kibana: http://localhost:5601"
else
    echo "OpenSearch Dashboards: http://localhost:5602"
fi 