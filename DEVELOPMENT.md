# Zentity Plugin Development Guide

This guide provides instructions for setting up and developing the Zentity entity resolution plugin using Docker Compose.

## Prerequisites

- Docker and Docker Compose
- Java 17+
- Maven 3.6+
- curl (for testing)

## Quick Start

The fastest way to get started with development:

```bash
# Build plugin and start Elasticsearch environment
./scripts/dev-setup.sh elasticsearch

# Or start OpenSearch environment
./scripts/dev-setup.sh opensearch

# Or start both for migration testing
./scripts/dev-setup.sh both
```

This will:
1. Build the Zentity plugin
2. Start the selected environment(s)
3. Install the plugin
4. Load test data

## Development Environments

### Elasticsearch Environment

```bash
# Start Elasticsearch only
docker-compose -f docker-compose.dev.yml up elasticsearch

# Start Elasticsearch with Kibana
docker-compose -f docker-compose.dev.yml up elasticsearch kibana
```

**Endpoints:**
- Elasticsearch: http://localhost:9200
- Kibana: http://localhost:5601
- Zentity API: http://localhost:9200/_zentity

### OpenSearch Environment

```bash
# Start OpenSearch only
docker-compose -f docker-compose.dev.yml up opensearch

# Start OpenSearch with Dashboards
docker-compose -f docker-compose.dev.yml up opensearch opensearch-dashboards
```

**Endpoints:**
- OpenSearch: http://localhost:9201
- OpenSearch Dashboards: http://localhost:5602
- Zentity API: http://localhost:9201/_zentity

### Both Environments (Migration Testing)

```bash
# Start both Elasticsearch and OpenSearch
docker-compose -f docker-compose.dev.yml up elasticsearch opensearch
```

This allows you to test plugin compatibility across both platforms simultaneously.

## Development Scripts

### `scripts/dev-setup.sh`

Complete development environment setup:

```bash
# Setup Elasticsearch environment
./scripts/dev-setup.sh elasticsearch

# Setup OpenSearch environment  
./scripts/dev-setup.sh opensearch

# Setup both environments
./scripts/dev-setup.sh both

# Skip plugin build (use existing)
BUILD_PLUGIN=false ./scripts/dev-setup.sh elasticsearch

# Skip test data loading
LOAD_DATA=false ./scripts/dev-setup.sh elasticsearch
```

### `scripts/install-plugin.sh`

Install the Zentity plugin into running containers:

```bash
# Install to Elasticsearch
./scripts/install-plugin.sh elasticsearch

# Install to OpenSearch
./scripts/install-plugin.sh opensearch
```

### `scripts/load-test-data.sh`

Load test data and entity models:

```bash
# Load into Elasticsearch
./scripts/load-test-data.sh elasticsearch

# Load into OpenSearch
./scripts/load-test-data.sh opensearch
```

## Development Workflow

### 1. Initial Setup

```bash
# Clone and setup
git clone <repository>
cd zentity
./scripts/dev-setup.sh elasticsearch
```

### 2. Code Changes

```bash
# Make your changes to Java files
vim src/main/java/...

# Rebuild and restart
mvn clean package -DskipTests
./scripts/install-plugin.sh elasticsearch
```

### 3. Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Manual testing with curl
curl http://localhost:9200/_zentity
```

### 4. Migration Testing

```bash
# Start both environments
./scripts/dev-setup.sh both

# Test plugin in both
curl http://localhost:9200/_zentity  # Elasticsearch
curl http://localhost:9201/_zentity  # OpenSearch
```

## Docker Compose Services

### Core Services

- **elasticsearch**: Elasticsearch 8.17.0 with security disabled
- **opensearch**: OpenSearch 2.17.0 with security disabled
- **kibana**: Kibana for Elasticsearch visualization
- **opensearch-dashboards**: OpenSearch Dashboards for visualization

### Tool Services (Profile: tools)

- **test-data-loader**: Utility container with curl for data loading
- **dev-tools**: Maven/Java container for building and testing

```bash
# Start tool services
docker-compose -f docker-compose.dev.yml --profile tools up test-data-loader dev-tools

# Use dev-tools container
docker exec -it zentity-dev-tools bash
mvn clean package
```

## Environment Variables

You can customize the setup using environment variables:

```bash
# Set versions
export ELASTICSEARCH_VERSION=8.17.0
export OPENSEARCH_VERSION=2.17.0
export ZENTITY_VERSION=1.8.3

# Use in docker-compose
docker-compose -f docker-compose.dev.yml up elasticsearch
```

## Testing Entity Resolution

### Basic Test

```bash
# Setup test data (if not already loaded)
./scripts/load-test-data.sh elasticsearch

# Test entity resolution
curl -X POST 'http://localhost:9200/_zentity/resolution/zentity_test_entity_a' \
     -H 'Content-Type: application/json' \
     -d '{"attributes":{"attribute_a":["a1"]}}'
```

### Model Management

```bash
# List all entity models
curl http://localhost:9200/_zentity/models

# Get specific model
curl http://localhost:9200/_zentity/models/zentity_test_entity_a

# Create/update model
curl -X POST 'http://localhost:9200/_zentity/models/my_model' \
     -H 'Content-Type: application/json' \
     -d @my-model.json
```

## Debugging

### View Logs

```bash
# View all logs
docker-compose -f docker-compose.dev.yml logs -f

# View specific service logs
docker-compose -f docker-compose.dev.yml logs -f elasticsearch
docker-compose -f docker-compose.dev.yml logs -f opensearch
```

### Container Access

```bash
# Access Elasticsearch container
docker exec -it zentity-elasticsearch bash

# Access OpenSearch container
docker exec -it zentity-opensearch bash

# Check plugin installation
docker exec zentity-elasticsearch elasticsearch-plugin list
docker exec zentity-opensearch opensearch-plugin list
```

### Health Checks

```bash
# Check cluster health
curl http://localhost:9200/_cluster/health
curl http://localhost:9201/_cluster/health

# Check plugin status
curl http://localhost:9200/_zentity
curl http://localhost:9201/_zentity

# Check indices
curl http://localhost:9200/_cat/indices
curl http://localhost:9201/_cat/indices
```

## Troubleshooting

### Plugin Installation Issues

```bash
# Check if plugin file exists
ls -la target/releases/

# Manually install plugin
docker exec zentity-elasticsearch elasticsearch-plugin install file:///releases/zentity-1.8.3-elasticsearch-8.17.0.zip

# Restart container after installation
docker restart zentity-elasticsearch
```

### Memory Issues

If you encounter memory issues, adjust the heap size:

```bash
# Edit docker-compose.dev.yml
# Change ES_JAVA_OPTS or OPENSEARCH_JAVA_OPTS
# Example: "ES_JAVA_OPTS=-Xms2g -Xmx2g"
```

### Port Conflicts

If ports are already in use:

```bash
# Stop conflicting services
sudo lsof -i :9200
sudo lsof -i :5601

# Or change ports in docker-compose.dev.yml
```

## Clean Up

```bash
# Stop all services
docker-compose -f docker-compose.dev.yml down

# Remove volumes (deletes data)
docker-compose -f docker-compose.dev.yml down -v

# Remove images
docker-compose -f docker-compose.dev.yml down --rmi all
```

## Integration with IDE

### IntelliJ IDEA

1. Import as Maven project
2. Set Project SDK to Java 17
3. Configure remote debugging:
   - Add JVM options to docker-compose: `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`
   - Create Remote Debug configuration pointing to `localhost:5005`

### VS Code

1. Install Java Extension Pack
2. Open project folder
3. Use integrated terminal for running scripts
4. Configure launch.json for remote debugging

## Contributing

1. Make changes in feature branch
2. Test with both Elasticsearch and OpenSearch
3. Run full test suite: `mvn clean verify`
4. Update documentation if needed
5. Submit pull request

## Resources

- [Zentity Documentation](https://zentity.io)
- [Elasticsearch Plugin Development](https://www.elastic.co/guide/en/elasticsearch/plugins/current/plugin-authors.html)
- [OpenSearch Plugin Development](https://opensearch.org/docs/latest/plugin-development/)
- [Docker Compose Reference](https://docs.docker.com/compose/) 