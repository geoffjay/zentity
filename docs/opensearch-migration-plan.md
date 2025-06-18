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

### Version Strategy and Future Planning

**Primary Target: OpenSearch 2.17.0**
- **Rationale**: Mature, stable, backward compatible with Elasticsearch 7.10 APIs
- **Advantages**: Lower migration complexity, extensive documentation, established plugin ecosystem
- **Technical Requirements**: JDK 11+, Lucene 9.x compatibility

**Future Consideration: OpenSearch 3.0**
- **Status**: Recently released (January 2025)
- **Major Changes**: Upgraded to Lucene 10, requires JDK 21+, significant performance improvements
- **Breaking Changes**: More extensive due to major version upgrade and Lucene 10
- **Recommendation**: Plan separate migration after 2.x migration is stable and proven

**Migration Path Strategy:**
1. **Phase 1**: Migrate to OpenSearch 2.17.0 (current plan)
2. **Phase 2**: Assess OpenSearch 3.0 migration (6-12 months post-2.x deployment)
3. **Maintain**: Dual compatibility during transition periods

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
- [ ] Analyze security configuration differences
- [ ] Document version check implications

**Deliverables:**
- `migration-tracking/dependency-audit.md`
- `migration-tracking/api-mapping.md`
- `migration-tracking/file-checklist.md`
- `migration-tracking/security-config-analysis.md`

**Critical API Mappings to Document:**
```java
// Exception handling changes
ElasticsearchException → OpenSearchException

// Client API changes  
org.elasticsearch.client.internal.node.NodeClient → org.opensearch.client.node.NodeClient

// XContent API changes (critical for JSON processing)
org.elasticsearch.xcontent.XContentBuilder → org.opensearch.core.xcontent.XContentBuilder

// Common utilities
org.elasticsearch.common.Strings → org.opensearch.common.Strings
```

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

<!-- Update test dependencies -->
<dependency>
    <groupId>org.opensearch.client</groupId>
    <artifactId>opensearch-java</artifactId>
    <version>${opensearch.version}</version>
    <scope>test</scope>
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

### 2.5 Security Configuration Handling

**Priority**: High  
**Risk**: Medium  
**Estimated Time**: 1 day

**Key Difference**: OpenSearch has security enabled by default, unlike Elasticsearch

**Tasks:**
- [ ] Update Docker configurations to disable security for testing
- [ ] Document security implications for production deployments
- [ ] Test plugin functionality with security enabled
- [ ] Create security configuration guide for users

**Docker Configuration Updates:**
```yaml
# Development environment - security disabled
environment:
  - "DISABLE_INSTALL_DEMO_CONFIG=true"
  - "DISABLE_SECURITY_PLUGIN=true"

# Production considerations - document security requirements
# - Authentication/authorization impact on API endpoints
# - SSL/TLS configuration requirements
# - Role-based access control for entity resolution
```

### 2.6 Version Check and Client Compatibility

**Priority**: Medium  
**Risk**: Medium  
**Estimated Time**: 1 day

**Potential Issues:**
- Version string checks in client code
- Cluster settings name changes
- Deprecated API usage

**Tasks:**
- [ ] Audit code for version-specific checks
- [ ] Test with different OpenSearch client versions
- [ ] Validate cluster setting compatibility
- [ ] Document version compatibility matrix

### 2.7 Core Logic Verification

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
- Handle OpenSearch security defaults

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
- Handle security configuration differences

**Phase 3.2.2: Test Data and Fixtures**
- Verify test data compatibility
- Update API endpoint references
- Validate entity model loading
- Test with security disabled configuration

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
- Handle security plugin considerations in build

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

### 4.4 Security Configuration Testing

**Priority**: High  
**Risk**: Medium  
**Estimated Time**: 1 day

**Test Scenarios:**
1. **Security Disabled** (development/testing)
2. **Security Enabled** (production simulation)
3. **Mixed Environments** (partial security)

**Tasks:**
```bash
# Test with security disabled
export OPENSEARCH_SECURITY=false
./scripts/dev-setup.sh opensearch

# Test with security enabled
export OPENSEARCH_SECURITY=true
./scripts/dev-setup.sh opensearch

# Validate authentication requirements
# Test role-based access control
# Document security configuration requirements
```

### 4.5 Regression Testing

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
- Document security configuration requirements
- Create OpenSearch 3.0 future migration considerations

**Deliverables:**
- `README.md` updates
- `docs/opensearch-compatibility.md`
- `docs/migration-from-elasticsearch.md`
- `docs/security-configuration.md`
- `docs/opensearch-3.0-planning.md`
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
- Document compatibility matrix

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
# - Security configuration testing
```

**Sign-off Criteria:**
- [ ] All unit tests pass (100%)
- [ ] All integration tests pass (100%)
- [ ] Performance benchmarks meet targets
- [ ] No memory leaks detected
- [ ] Documentation complete and accurate
- [ ] Release artifacts build successfully
- [ ] Security configurations documented and tested

## Risk Management and Mitigation

### High-Risk Areas

**1. Import Statement Migration**
- **Risk**: Breaking API changes between Elasticsearch and OpenSearch
- **Mitigation**: Incremental migration with compilation validation
- **Rollback**: Git branch with atomic commits per file
- **Research Finding**: XContent API changes are particularly critical for JSON processing

**2. Integration Test Updates**
- **Risk**: Test infrastructure failures blocking validation
- **Mitigation**: Parallel development environment maintenance
- **Rollback**: Maintain Elasticsearch test environment
- **Research Finding**: Security configuration differences require special handling

**3. Core API Compatibility**
- **Risk**: Subtle behavioral differences affecting entity resolution
- **Mitigation**: Comprehensive regression testing
- **Rollback**: Detailed comparison framework
- **Research Finding**: OpenSearch maintains API compatibility with Elasticsearch 7.10

**4. Security Configuration Differences**
- **Risk**: OpenSearch security enabled by default causing integration failures
- **Mitigation**: Explicit security configuration in all environments
- **Rollback**: Document security bypass procedures
- **Research Finding**: Major difference requiring careful handling

### Medium-Risk Areas

**1. Docker Configuration**
- **Risk**: Container startup and networking issues
- **Mitigation**: Extensive local testing before CI/CD updates
- **Rollback**: Maintain separate Docker configurations

**2. Performance Regression**
- **Risk**: OpenSearch performance differences
- **Mitigation**: Continuous benchmarking during migration
- **Rollback**: Performance baseline documentation
- **Research Finding**: OpenSearch 2.x provides similar performance to Elasticsearch 8.x

**3. Version Check Dependencies**
- **Risk**: Client code checking version strings
- **Mitigation**: Audit and update version-specific code
- **Rollback**: Version compatibility shim layer
- **Research Finding**: Some clients may check version strings

### Future Migration Considerations

**OpenSearch 3.0 Migration (Future)**
- **JDK 21 Requirement**: Plan JDK upgrade timeline
- **Lucene 10 Changes**: Significant internal changes requiring testing
- **Breaking Changes**: More extensive breaking changes than 2.x migration
- **Performance Benefits**: Significant performance improvements available
- **Timeline**: 6-12 months after 2.x migration stabilization

### Contingency Plans

**Plan A: Full Migration Success**
- Release OpenSearch version alongside Elasticsearch version
- Maintain dual compatibility during transition period
- Gradual user migration with support

**Plan B: Partial Migration Issues**
- Release OpenSearch version as beta/experimental
- Continue Elasticsearch version as stable
- Address issues in subsequent releases
- Document known limitations

**Plan C: Migration Failure**
- Document compatibility issues and blockers
- Maintain Elasticsearch version only
- Plan future migration with additional resources
- Consider OpenSearch 3.0 as alternative path

## Success Metrics

### Technical Metrics
- **Code Coverage**: Maintain >90% test coverage
- **Performance**: <5% performance regression from Elasticsearch version
- **Compatibility**: 100% API compatibility with existing functionality
- **Reliability**: Zero critical bugs in core functionality
- **Security**: Successful operation with both security enabled/disabled

### Project Metrics
- **Timeline**: Complete within 12 weeks
- **Quality**: Pass all automated and manual tests
- **Documentation**: Complete user and developer documentation
- **Adoption**: Successful deployment in test environments
- **User Satisfaction**: Smooth migration experience for existing users

## Timeline Summary

| Phase | Duration | Key Deliverables | Risk Level |
|-------|----------|------------------|------------|
| 1: Environment Setup | 2 weeks | Development environment, migration planning, security analysis | Low |
| 2: Core Migration | 4 weeks | Package updates, import migration, plugin descriptor, security config | High |
| 3: Testing Infrastructure | 2 weeks | Docker updates, integration test migration, security testing | Medium |
| 4: Comprehensive Testing | 2 weeks | Full test suite, performance validation, regression testing | High |
| 5: Documentation & Release | 2 weeks | Documentation, release preparation, migration guides | Low |

**Total Duration**: 12 weeks  
**Critical Path**: Core Migration → Integration Testing → Comprehensive Testing  
**Key Milestones**: 
- Week 6: Core migration complete, basic compilation successful
- Week 8: Integration tests passing, security configurations working
- Week 10: Full test suite passing, performance benchmarks met
- Week 12: Release ready, documentation complete

## Post-Migration Activities

### Immediate (Week 13-14)
- [ ] Release OpenSearch version
- [ ] Monitor adoption and feedback
- [ ] Address any critical issues
- [ ] Update CI/CD for dual releases
- [ ] Provide user migration support

### Short-term (Month 2-3)
- [ ] Gather user feedback and usage analytics
- [ ] Performance optimization based on real-world usage
- [ ] Additional OpenSearch feature utilization
- [ ] Enhanced documentation based on user questions
- [ ] Security configuration optimization

### Long-term (Month 4-6)
- [ ] OpenSearch 3.0 compatibility assessment
- [ ] Advanced OpenSearch features integration (vector search, etc.)
- [ ] Elasticsearch compatibility deprecation planning
- [ ] Community adoption analysis and roadmap planning
- [ ] Performance comparison studies

### Future Planning (Month 6+)
- [ ] OpenSearch 3.0 migration planning (JDK 21, Lucene 10)
- [ ] Elasticsearch support lifecycle decisions
- [ ] Advanced entity resolution features leveraging OpenSearch capabilities
- [ ] Community contribution to OpenSearch ecosystem

This comprehensive migration plan ensures a systematic, well-tested transition from Elasticsearch to OpenSearch while maintaining stability, performance, and functionality of the Zentity plugin. The plan incorporates critical research findings around security configuration differences, API compatibility considerations, and future migration planning for OpenSearch 3.0. 