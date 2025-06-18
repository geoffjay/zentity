# OpenSearch Migration Execution Plan for Zentity Plugin

## Executive Summary

This document outlines a comprehensive, phased approach to migrating the Zentity entity resolution plugin from Elasticsearch 8.17.0 to OpenSearch 2.17.0. The migration follows a systematic approach with thorough testing at each phase to ensure stability, compatibility, and functionality preservation.

**Target**: OpenSearch 2.17.0 (stable, mature, backward compatible)  
**Timeline**: 8-12 weeks  
**Risk Level**: Medium (systematic approach with extensive testing)

## Migration Overview

### Current State Analysis

**Codebase Structure:**
- **Main Plugin Classes**: 13 Java files in `org.elasticsearch.plugin.zentity` package
- **Core Logic**: Entity resolution engine in `io.zentity` packages (no changes needed)
- **REST Actions**: 4 main endpoints (Home, Models, Resolution, Setup)
- **Integration Tests**: Comprehensive test suite with Docker Compose
- **Dependencies**: Heavy reliance on Elasticsearch APIs and client libraries

**Key Components Requiring Migration:**
1. **Package Declarations**: 13 files with `org.elasticsearch.plugin.zentity` package
2. **Import Statements**: 200+ Elasticsearch imports across all Java files
3. **Plugin Descriptor**: Configuration and version references
4. **Maven Dependencies**: Build system and artifact management
5. **Docker Configuration**: Test infrastructure and CI/CD
6. **Integration Tests**: Test containers and API validation

## Phase 1: Environment Setup and Preparation (Week 1-2)

### 1.1 Development Environment Setup

**Deliverables:**
- [x] Docker Compose development environment (completed)
- [x] Build scripts and automation (completed)
- [x] Documentation for development workflow (completed)

**Tasks:**
```bash
# Already completed - verify functionality
./scripts/dev-setup.sh both
curl http://localhost:9200/_zentity  # Elasticsearch
curl http://localhost:9201/_zentity  # OpenSearch (will fail initially)
```

### 1.2 Create Migration Branch

**Tasks:**
```bash
# Create dedicated migration branch
git checkout -b feature/opensearch-migration
git push -u origin feature/opensearch-migration

# Create milestone tracking
mkdir -p migration-tracking
echo "Phase 1: Environment Setup - COMPLETED" > migration-tracking/phase1.md
```

### 1.3 Dependency Analysis and Planning

**Tasks:**
- [ ] Complete audit of all Elasticsearch dependencies
- [ ] Map Elasticsearch APIs to OpenSearch equivalents
- [ ] Identify potential breaking changes and compatibility issues
- [ ] Create detailed file-by-file migration checklist

**Deliverables:**
- `migration-tracking/dependency-audit.md`
- `migration-tracking/api-mapping.md`
- `migration-tracking/file-checklist.md`

## Phase 2: Core Migration Implementation (Week 3-6)

### 2.1 Maven Configuration Updates

**Priority**: Critical  
**Risk**: Low  
**Estimated Time**: 1 day

**Tasks:**
```xml
<!-- Update pom.xml dependencies -->
<elasticsearch.version>8.17.0</elasticsearch.version>
<!-- Replace with -->
<opensearch.version>2.17.0</opensearch.version>

<!-- Update dependencies -->
<dependency>
    <groupId>org.opensearch</groupId>
    <artifactId>opensearch</artifactId>
    <version>${opensearch.version}</version>
    <scope>provided</scope>
</dependency>
```

**Files to Update:**
- `pom.xml`
- `src/main/resources/plugin-descriptor.properties`

**Validation:**
```bash
mvn clean compile
# Should compile without Elasticsearch dependencies
```

### 2.2 Package Namespace Migration

**Priority**: Critical  
**Risk**: Medium  
**Estimated Time**: 2-3 days

**Strategy**: Systematic replacement using IDE refactoring tools

**Phase 2.2.1: Plugin Package Structure**
```java
// FROM:
package org.elasticsearch.plugin.zentity;

// TO:
package org.opensearch.plugin.zentity;
```

**Files to Update:**
- `src/main/java/org/elasticsearch/plugin/zentity/ZentityPlugin.java`
- `src/main/java/org/elasticsearch/plugin/zentity/HomeAction.java`
- `src/main/java/org/elasticsearch/plugin/zentity/ModelsAction.java`
- `src/main/java/org/elasticsearch/plugin/zentity/ResolutionAction.java`
- `src/main/java/org/elasticsearch/plugin/zentity/SetupAction.java`
- `src/main/java/org/elasticsearch/plugin/zentity/BulkAction.java`
- `src/main/java/org/elasticsearch/plugin/zentity/ParamsUtil.java`
- All corresponding test files (7 files)

**Validation After Each File:**
```bash
mvn compile -Dmaven.test.skip=true
```

### 2.3 Import Statement Migration

**Priority**: Critical  
**Risk**: High  
**Estimated Time**: 4-5 days

**Strategy**: Automated replacement with manual verification

**Phase 2.3.1: Core Elasticsearch Imports**
```java
// Core API imports
import org.elasticsearch.ElasticsearchException;
// TO:
import org.opensearch.OpenSearchException;

import org.elasticsearch.client.internal.node.NodeClient;
// TO:
import org.opensearch.client.node.NodeClient;

import org.elasticsearch.rest.BaseRestHandler;
// TO:
import org.opensearch.rest.BaseRestHandler;
```

**Phase 2.3.2: Action and Client Imports**
```java
import org.elasticsearch.action.ActionListener;
// TO:
import org.opensearch.action.ActionListener;

import org.elasticsearch.action.search.SearchResponse;
// TO:
import org.opensearch.action.search.SearchResponse;
```

**Phase 2.3.3: XContent and Utilities**
```java
import org.elasticsearch.xcontent.XContentBuilder;
// TO:
import org.opensearch.core.xcontent.XContentBuilder;

import org.elasticsearch.common.Strings;
// TO:
import org.opensearch.common.Strings;
```

**Critical Files for Import Migration:**
1. `ZentityPlugin.java` (41 imports)
2. `ModelsAction.java` (49 imports)
3. `ResolutionAction.java` (33 imports)
4. `Job.java` (37 imports)
5. `Query.java` (39 imports)

**Validation Strategy:**
```bash
# After each major file
mvn compile -Dmaven.test.skip=true

# Full compilation check
mvn clean compile
```

### 2.4 Plugin Descriptor Updates

**Priority**: Critical  
**Risk**: Low  
**Estimated Time**: 0.5 days

**Tasks:**
```properties
# src/main/resources/plugin-descriptor.properties
# FROM:
elasticsearch.version=${elasticsearch.version}

# TO:
opensearch.version=${opensearch.version}
```

**Additional Updates:**
```xml
<!-- pom.xml -->
<zentity.classname>org.opensearch.plugin.zentity.ZentityPlugin</zentity.classname>
```

### 2.5 Core Logic Verification

**Priority**: High  
**Risk**: Medium  
**Estimated Time**: 2 days

**Focus Areas:**
- Entity resolution algorithms (should remain unchanged)
- JSON processing and data structures
- Search query construction
- Response parsing and formatting

**Key Files to Verify:**
- `io.zentity.resolution.Job.java` (core resolution logic)
- `io.zentity.resolution.Query.java` (search query building)
- `io.zentity.model.*` (entity model processing)

**Validation:**
```bash
# Compile core logic
mvn compile -pl :zentity -am

# Unit tests for core logic
mvn test -Dtest="*Test" -Dmaven.failsafe.skip=true
```

## Phase 3: Testing Infrastructure Migration (Week 7-8)

### 3.1 Docker Compose Updates

**Priority**: High  
**Risk**: Medium  
**Estimated Time**: 1 day

**Tasks:**
- Update existing `src/test/resources/docker-compose.yml` for OpenSearch
- Ensure development Docker Compose supports both environments
- Update CI/CD pipeline configurations

**New Docker Compose for Tests:**
```yaml
# src/test/resources/docker-compose.yml
version: '3.7'
services:
  opensearch:
    image: "opensearchproject/opensearch:${OPENSEARCH_VERSION}"
    environment:
      - node.name=opensearch
      - cluster.name=zentity-test-cluster
      - discovery.type=single-node
      - "DISABLE_INSTALL_DEMO_CONFIG=true"
      - "DISABLE_SECURITY_PLUGIN=true"
      - bootstrap.memory_lock=true
      - "OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m -ea"
      - action.destructive_requires_name=false
    command:
      - /bin/bash
      - -c
      - "opensearch-plugin install --batch file:///releases/zentity-${ZENTITY_VERSION}-opensearch-${OPENSEARCH_VERSION}.zip && opensearch"
    volumes:
      - opensearch_data:/usr/share/opensearch/data
      - ${BUILD_DIRECTORY}/releases/:/releases
    ports:
      - 9400:9200
    networks:
      - opensearch
```

### 3.2 Integration Test Updates

**Priority**: High  
**Risk**: High  
**Estimated Time**: 3-4 days

**Strategy**: Update tests incrementally with parallel validation

**Phase 3.2.1: Test Infrastructure**
- Update `AbstractIT.java` for OpenSearch compatibility
- Modify test container configuration
- Update health check endpoints and timing

**Phase 3.2.2: Test Data and Fixtures**
- Verify test data compatibility
- Update API endpoint references
- Validate entity model loading

**Phase 3.2.3: API Test Updates**
- `HomeActionIT.java`: Update version field references
- `SetupActionIT.java`: Verify index creation and management
- `ModelsActionIT.java`: Test entity model CRUD operations
- `ResolutionActionIT.java`: Comprehensive entity resolution testing

**Critical Test Scenarios:**
```bash
# Home endpoint
curl http://localhost:9400/_zentity

# Setup functionality
curl -X POST http://localhost:9400/_zentity/_setup

# Model management
curl -X POST http://localhost:9400/_zentity/models/test_model -d @test-model.json

# Entity resolution
curl -X POST http://localhost:9400/_zentity/resolution/test_model -d @test-query.json
```

### 3.3 Build System Integration

**Priority**: High  
**Risk**: Medium  
**Estimated Time**: 1 day

**Tasks:**
- Update Maven build profiles for OpenSearch
- Modify CI/CD pipeline for dual building
- Update release artifact naming

**Build Configuration:**
```xml
<profiles>
    <profile>
        <id>opensearch</id>
        <properties>
            <opensearch.version>2.17.0</opensearch.version>
            <zentity.classname>org.opensearch.plugin.zentity.ZentityPlugin</zentity.classname>
        </properties>
    </profile>
</profiles>
```

## Phase 4: Comprehensive Testing and Validation (Week 9-10)

### 4.1 Unit Testing

**Priority**: Critical  
**Risk**: Medium  
**Estimated Time**: 2 days

**Test Categories:**
1. **Core Logic Tests** (should pass without changes)
   ```bash
   mvn test -Dtest="io.zentity.**.*Test"
   ```

2. **Plugin Integration Tests**
   ```bash
   mvn test -Dtest="org.opensearch.plugin.zentity.**.*Test"
   ```

3. **Model Validation Tests**
   ```bash
   mvn test -Dtest="*ModelTest,*ValidationTest"
   ```

**Success Criteria:**
- All existing unit tests pass
- No regression in core functionality
- Plugin loading and initialization successful

### 4.2 Integration Testing

**Priority**: Critical  
**Risk**: High  
**Estimated Time**: 3 days

**Test Scenarios:**

**Phase 4.2.1: Basic Functionality**
```bash
# Start OpenSearch test environment
./scripts/dev-setup.sh opensearch

# Plugin installation verification
curl http://localhost:9201/_cat/plugins

# Home endpoint
curl http://localhost:9201/_zentity

# Setup functionality
curl -X POST http://localhost:9201/_zentity/_setup
```

**Phase 4.2.2: Entity Model Management**
```bash
# Create entity model
curl -X POST 'http://localhost:9201/_zentity/models/person' \
     -H 'Content-Type: application/json' \
     -d @src/test/resources/TestEntityModelA.json

# Retrieve entity model
curl http://localhost:9201/_zentity/models/person

# Update entity model
curl -X PUT 'http://localhost:9201/_zentity/models/person' \
     -H 'Content-Type: application/json' \
     -d @src/test/resources/TestEntityModelB.json

# Delete entity model
curl -X DELETE http://localhost:9201/_zentity/models/person
```

**Phase 4.2.3: Entity Resolution Testing**
```bash
# Load test data
./scripts/load-test-data.sh opensearch

# Basic entity resolution
curl -X POST 'http://localhost:9201/_zentity/resolution/zentity_test_entity_a' \
     -H 'Content-Type: application/json' \
     -d '{"attributes":{"attribute_a":["a1"]}}'

# Complex resolution with multiple hops
curl -X POST 'http://localhost:9201/_zentity/resolution/zentity_test_entity_a?max_hops=3' \
     -H 'Content-Type: application/json' \
     -d '{"attributes":{"attribute_a":["a1"],"attribute_b":["b1"]}}'

# Bulk resolution operations
curl -X POST 'http://localhost:9201/_zentity/resolution/_bulk' \
     -H 'Content-Type: application/x-ndjson' \
     -d @src/test/resources/bulk-resolution-test.ndjson
```

**Phase 4.2.4: Error Handling and Edge Cases**
```bash
# Invalid model creation
curl -X POST 'http://localhost:9201/_zentity/models/invalid' \
     -H 'Content-Type: application/json' \
     -d '{"invalid": "model"}'

# Resolution with missing indices
curl -X POST 'http://localhost:9201/_zentity/resolution/nonexistent_model' \
     -H 'Content-Type: application/json' \
     -d '{"attributes":{"attr":["value"]}}'

# Malformed query handling
curl -X POST 'http://localhost:9201/_zentity/resolution/zentity_test_entity_a' \
     -H 'Content-Type: application/json' \
     -d '{"malformed": json}'
```

### 4.3 Performance and Compatibility Testing

**Priority**: High  
**Risk**: Medium  
**Estimated Time**: 2 days

**Performance Benchmarks:**
1. **Entity Resolution Performance**
   - Single entity resolution: < 100ms for simple queries
   - Multi-hop resolution: < 500ms for 3-hop queries
   - Bulk operations: > 100 resolutions/second

2. **Memory Usage**
   - Plugin memory footprint: < 50MB baseline
   - Peak memory during large resolutions: < 200MB

3. **Concurrent Operations**
   - Support 50+ concurrent resolution requests
   - No memory leaks during sustained load

**Compatibility Testing:**
```bash
# Test with different OpenSearch versions
export OPENSEARCH_VERSION=2.16.0
./scripts/dev-setup.sh opensearch
# Run test suite

export OPENSEARCH_VERSION=2.17.0
./scripts/dev-setup.sh opensearch
# Run test suite

# Cross-compatibility testing
./scripts/dev-setup.sh both
# Test data migration between Elasticsearch and OpenSearch
```

### 4.4 Regression Testing

**Priority**: Critical  
**Risk**: High  
**Estimated Time**: 2 days

**Strategy**: Comprehensive comparison between Elasticsearch and OpenSearch versions

**Test Framework:**
```bash
# Automated regression test script
#!/bin/bash
# scripts/regression-test.sh

# Start both environments
./scripts/dev-setup.sh both

# Load identical test data
./scripts/load-test-data.sh elasticsearch
./scripts/load-test-data.sh opensearch

# Run identical queries against both
./scripts/run-regression-tests.sh

# Compare results
./scripts/compare-results.sh
```

**Regression Test Categories:**
1. **API Response Compatibility**
   - Identical JSON structure
   - Same field names and types
   - Consistent error messages

2. **Entity Resolution Accuracy**
   - Same entities identified
   - Identical confidence scores
   - Consistent ranking and ordering

3. **Performance Characteristics**
   - Similar response times
   - Comparable memory usage
   - Equivalent throughput

## Phase 5: Documentation and Release Preparation (Week 11-12)

### 5.1 Documentation Updates

**Priority**: Medium  
**Risk**: Low  
**Estimated Time**: 2 days

**Tasks:**
- Update README.md with OpenSearch compatibility
- Create migration guide for existing users
- Update API documentation
- Update Docker Compose and development guides

**Deliverables:**
- `README.md` updates
- `docs/opensearch-compatibility.md`
- `docs/migration-from-elasticsearch.md`
- Updated `DEVELOPMENT.md`

### 5.2 Release Artifact Preparation

**Priority**: High  
**Risk**: Low  
**Estimated Time**: 1 day

**Tasks:**
- Update version numbering scheme
- Prepare dual-build pipeline (Elasticsearch + OpenSearch)
- Update GitHub Actions workflows
- Prepare release notes

**Artifact Naming:**
- Elasticsearch: `zentity-1.8.3-elasticsearch-8.17.0.zip`
- OpenSearch: `zentity-1.8.3-opensearch-2.17.0.zip`

### 5.3 Final Validation and Sign-off

**Priority**: Critical  
**Risk**: Medium  
**Estimated Time**: 2 days

**Final Test Suite:**
```bash
# Complete end-to-end test
./scripts/final-validation.sh

# Includes:
# - Fresh environment setup
# - Plugin installation
# - Complete API test suite
# - Performance benchmarks
# - Memory leak detection
# - Error handling validation
```

**Sign-off Criteria:**
- [ ] All unit tests pass (100%)
- [ ] All integration tests pass (100%)
- [ ] Performance benchmarks meet targets
- [ ] No memory leaks detected
- [ ] Documentation complete and accurate
- [ ] Release artifacts build successfully

## Risk Management and Mitigation

### High-Risk Areas

**1. Import Statement Migration**
- **Risk**: Breaking API changes between Elasticsearch and OpenSearch
- **Mitigation**: Incremental migration with compilation validation
- **Rollback**: Git branch with atomic commits per file

**2. Integration Test Updates**
- **Risk**: Test infrastructure failures blocking validation
- **Mitigation**: Parallel development environment maintenance
- **Rollback**: Maintain Elasticsearch test environment

**3. Core API Compatibility**
- **Risk**: Subtle behavioral differences affecting entity resolution
- **Mitigation**: Comprehensive regression testing
- **Rollback**: Detailed comparison framework

### Medium-Risk Areas

**1. Docker Configuration**
- **Risk**: Container startup and networking issues
- **Mitigation**: Extensive local testing before CI/CD updates
- **Rollback**: Maintain separate Docker configurations

**2. Performance Regression**
- **Risk**: OpenSearch performance differences
- **Mitigation**: Continuous benchmarking during migration
- **Rollback**: Performance baseline documentation

### Contingency Plans

**Plan A: Full Migration Success**
- Release OpenSearch version alongside Elasticsearch version
- Maintain dual compatibility during transition period

**Plan B: Partial Migration Issues**
- Release OpenSearch version as beta/experimental
- Continue Elasticsearch version as stable
- Address issues in subsequent releases

**Plan C: Migration Failure**
- Document compatibility issues
- Maintain Elasticsearch version only
- Plan future migration with additional resources

## Success Metrics

### Technical Metrics
- **Code Coverage**: Maintain >90% test coverage
- **Performance**: <5% performance regression
- **Compatibility**: 100% API compatibility
- **Reliability**: Zero critical bugs in core functionality

### Project Metrics
- **Timeline**: Complete within 12 weeks
- **Quality**: Pass all automated and manual tests
- **Documentation**: Complete user and developer documentation
- **Adoption**: Successful deployment in test environments

## Timeline Summary

| Phase | Duration | Key Deliverables | Risk Level |
|-------|----------|------------------|------------|
| 1: Environment Setup | 2 weeks | Development environment, migration planning | Low |
| 2: Core Migration | 4 weeks | Package updates, import migration, plugin descriptor | High |
| 3: Testing Infrastructure | 2 weeks | Docker updates, integration test migration | Medium |
| 4: Comprehensive Testing | 2 weeks | Full test suite, performance validation | High |
| 5: Documentation & Release | 2 weeks | Documentation, release preparation | Low |

**Total Duration**: 12 weeks  
**Critical Path**: Core Migration → Integration Testing → Comprehensive Testing  
**Key Milestones**: 
- Week 6: Core migration complete, basic compilation successful
- Week 8: Integration tests passing
- Week 10: Full test suite passing
- Week 12: Release ready

## Post-Migration Activities

### Immediate (Week 13-14)
- [ ] Release OpenSearch version
- [ ] Monitor adoption and feedback
- [ ] Address any critical issues
- [ ] Update CI/CD for dual releases

### Short-term (Month 2-3)
- [ ] Gather user feedback
- [ ] Performance optimization
- [ ] Additional OpenSearch feature utilization
- [ ] Enhanced documentation

### Long-term (Month 4-6)
- [ ] OpenSearch 3.0 compatibility assessment
- [ ] Advanced OpenSearch features integration
- [ ] Elasticsearch compatibility deprecation planning
- [ ] Community adoption analysis

This comprehensive migration plan ensures a systematic, well-tested transition from Elasticsearch to OpenSearch while maintaining stability, performance, and functionality of the Zentity plugin. 