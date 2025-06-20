#!/bin/bash

# Development setup script for Zentity plugin
# This script builds the plugin and sets up the development environment

set -e

echo "=== Zentity Plugin Development Setup ==="
echo ""

# Default values
TARGET=${1:-elasticsearch}
BUILD_PLUGIN=${BUILD_PLUGIN:-true}
LOAD_DATA=${LOAD_DATA:-true}

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_step() {
    echo -e "${BLUE}==> $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Check prerequisites
print_step "Checking prerequisites..."

if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed or not in PATH"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose is not installed or not in PATH"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed or not in PATH"
    exit 1
fi

if ! command -v java &> /dev/null; then
    print_error "Java is not installed or not in PATH"
    exit 1
fi

print_success "All prerequisites found"

# Build the plugin
if [ "$BUILD_PLUGIN" = "true" ]; then
    print_step "Building Zentity plugin..."
    
    # Set up Java environment for asdf users
    if command -v asdf &> /dev/null; then
        export JAVA_HOME=$(asdf where java 2>/dev/null || echo $JAVA_HOME)
    fi
    
    # Clean and build (skip tests due to OpenSearch migration test issues)
    mvn clean package -DskipTests -Dmaven.test.skip=true
    
    # Check if build was successful
    if [ $? -eq 0 ]; then
        print_success "Plugin built successfully"
        
        # List built artifacts
        echo "Built artifacts:"
        ls -la target/releases/ 2>/dev/null || echo "  No release artifacts found"
    else
        print_error "Plugin build failed"
        exit 1
    fi
else
    print_warning "Skipping plugin build (BUILD_PLUGIN=false)"
fi

# Start the target environment
print_step "Starting ${TARGET} environment..."

# Stop any existing containers
docker-compose -f docker-compose.dev.yml down 2>/dev/null || true

# Start the target service
if [ "$TARGET" = "both" ]; then
    docker-compose -f docker-compose.dev.yml up -d elasticsearch opensearch
    print_success "Started both Elasticsearch and OpenSearch"
elif [ "$TARGET" = "elasticsearch" ]; then
    docker-compose -f docker-compose.dev.yml up -d elasticsearch
    print_success "Started Elasticsearch"
elif [ "$TARGET" = "opensearch" ]; then
    docker-compose -f docker-compose.dev.yml up -d opensearch
    print_success "Started OpenSearch"
else
    print_error "Invalid target: $TARGET (must be 'elasticsearch', 'opensearch', or 'both')"
    exit 1
fi

# Wait for services to be healthy
print_step "Waiting for services to be ready..."

wait_for_service() {
    local service=$1
    local url=$2
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url/_cluster/health" > /dev/null 2>&1; then
            print_success "$service is ready"
            return 0
        fi
        
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    print_error "$service failed to start within timeout"
    return 1
}

if [ "$TARGET" = "elasticsearch" ] || [ "$TARGET" = "both" ]; then
    wait_for_service "Elasticsearch" "http://localhost:9200"
fi

if [ "$TARGET" = "opensearch" ] || [ "$TARGET" = "both" ]; then
    wait_for_service "OpenSearch" "http://localhost:9201"
fi

# Install the plugin
if [ "$BUILD_PLUGIN" = "true" ]; then
    print_step "Installing Zentity plugin..."
    
    if [ "$TARGET" = "both" ]; then
        ./scripts/install-plugin.sh elasticsearch
        # Try OpenSearch installation, but don't fail if plugin doesn't exist
        if ./scripts/install-plugin.sh opensearch 2>/dev/null; then
            print_success "OpenSearch plugin installed"
        else
            print_warning "OpenSearch plugin not available (this is expected during migration)"
        fi
    else
        ./scripts/install-plugin.sh "$TARGET"
    fi
    
    print_success "Plugin installed"
    
    # Wait for services to be ready again after plugin installation
    print_step "Waiting for services to be ready after plugin installation..."
    if [ "$TARGET" = "elasticsearch" ] || [ "$TARGET" = "both" ]; then
        wait_for_service "Elasticsearch" "http://localhost:9200"
    fi
    if [ "$TARGET" = "opensearch" ] || [ "$TARGET" = "both" ]; then
        wait_for_service "OpenSearch" "http://localhost:9201"
    fi
else
    print_warning "Skipping plugin installation (BUILD_PLUGIN=false)"
fi

# Load test data
if [ "$LOAD_DATA" = "true" ]; then
    print_step "Loading test data..."
    
    if [ "$TARGET" = "both" ]; then
        ./scripts/load-test-data.sh elasticsearch
        # Try OpenSearch data loading, but don't fail if plugin isn't installed
        if ./scripts/load-test-data.sh opensearch 2>/dev/null; then
            print_success "OpenSearch test data loaded"
        else
            print_warning "OpenSearch test data not loaded (plugin not available)"
        fi
    else
        ./scripts/load-test-data.sh "$TARGET"
    fi
    
    print_success "Test data loaded"
else
    print_warning "Skipping test data loading (LOAD_DATA=false)"
fi

# Display summary
echo ""
print_success "Development environment setup complete!"
echo ""
echo "=== Available Services ==="

if [ "$TARGET" = "elasticsearch" ] || [ "$TARGET" = "both" ]; then
    echo "Elasticsearch:"
    echo "  • Cluster Health: http://localhost:9200/_cluster/health"
    echo "  • Zentity Home:   http://localhost:9200/_zentity"
    echo "  • Kibana:         http://localhost:5601"
fi

if [ "$TARGET" = "opensearch" ] || [ "$TARGET" = "both" ]; then
    echo "OpenSearch:"
    echo "  • Cluster Health: http://localhost:9201/_cluster/health"
    echo "  • Zentity Home:   http://localhost:9201/_zentity"
    echo "  • Dashboards:     http://localhost:5602"
fi

echo ""
echo "=== Quick Commands ==="
echo "# View logs:"
echo "  docker-compose -f docker-compose.dev.yml logs -f $TARGET"
echo ""
echo "# Run tests:"
echo "  mvn test"
echo ""
echo "# Rebuild and restart:"
echo "  ./scripts/dev-setup.sh $TARGET"
echo ""
echo "# Stop all services:"
echo "  docker-compose -f docker-compose.dev.yml down"
echo ""

print_success "Ready for development!" 