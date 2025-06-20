#!/bin/bash

# Install Zentity plugin into Elasticsearch or OpenSearch container
# Usage: ./install-plugin.sh [elasticsearch|opensearch]

set -e

TARGET=${1:-elasticsearch}
PLUGIN_VERSION=${ZENTITY_VERSION:-1.8.3}
ES_VERSION=${ELASTICSEARCH_VERSION:-8.17.0}
OS_VERSION=${OPENSEARCH_VERSION:-2.17.0}

if [ "$TARGET" = "elasticsearch" ]; then
    CONTAINER_NAME="zentity-elasticsearch"
    PLUGIN_FILE="zentity-${PLUGIN_VERSION}-elasticsearch-${ES_VERSION}.zip"
    PLUGIN_PATH="/opt/zentity/releases/${PLUGIN_FILE}"
    INSTALL_CMD="elasticsearch-plugin install --batch file://${PLUGIN_PATH}"
elif [ "$TARGET" = "opensearch" ]; then
    CONTAINER_NAME="zentity-opensearch"
    PLUGIN_FILE="zentity-${PLUGIN_VERSION}-opensearch-${OS_VERSION}.zip"
    PLUGIN_PATH="/opt/zentity/releases/${PLUGIN_FILE}"
    INSTALL_CMD="opensearch-plugin install --batch file://${PLUGIN_PATH}"
else
    echo "Error: Target must be 'elasticsearch' or 'opensearch'"
    exit 1
fi

echo "Installing Zentity plugin into ${TARGET}..."
echo "Plugin file: ${PLUGIN_FILE}"

# Check if container is running
if ! docker ps | grep -q "${CONTAINER_NAME}"; then
    echo "Error: Container ${CONTAINER_NAME} is not running"
    echo "Start it with: docker-compose -f docker-compose.dev.yml up ${TARGET}"
    exit 1
fi

# Check if plugin file exists
if ! docker exec "${CONTAINER_NAME}" test -f "${PLUGIN_PATH}"; then
    echo "Error: Plugin file not found: ${PLUGIN_PATH}"
    echo "Make sure you have built the plugin with: mvn clean package"
    exit 1
fi

# Check if plugin is already installed
PLUGIN_INSTALLED=false
if [ "$TARGET" = "elasticsearch" ]; then
    if docker exec "${CONTAINER_NAME}" ls /usr/share/elasticsearch/plugins/zentity 2>/dev/null; then
        PLUGIN_INSTALLED=true
    fi
else
    if docker exec "${CONTAINER_NAME}" ls /usr/share/opensearch/plugins/zentity 2>/dev/null; then
        PLUGIN_INSTALLED=true
    fi
fi

if [ "$PLUGIN_INSTALLED" = "true" ]; then
    echo "Plugin already installed. Removing existing installation..."
    if [ "$TARGET" = "elasticsearch" ]; then
        docker exec "${CONTAINER_NAME}" elasticsearch-plugin remove zentity || true
    else
        docker exec "${CONTAINER_NAME}" opensearch-plugin remove zentity || true
    fi
fi

# Install the plugin
echo "Executing: ${INSTALL_CMD}"
docker exec "${CONTAINER_NAME}" ${INSTALL_CMD}

# Restart the container to load the plugin
echo "Restarting ${CONTAINER_NAME} to load the plugin..."
docker restart "${CONTAINER_NAME}"

echo "Plugin installation complete!"
echo "You can verify the installation by visiting:"
if [ "$TARGET" = "elasticsearch" ]; then
    echo "  http://localhost:9200/_zentity"
else
    echo "  http://localhost:9201/_zentity"
fi 