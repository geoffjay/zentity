# OpenSearch Migration Research for Zentity Plugin

## Executive Summary

This document provides comprehensive research and recommendations for migrating the Zentity entity resolution plugin from Elasticsearch 8.17.0 to OpenSearch. Zentity is an entity resolution plugin for Elasticsearch that provides advanced search capabilities for connecting and analyzing related entities across multiple data sources.

## Current Project Status

### Project Overview
- **Project**: Zentity - Entity resolution for Elasticsearch
- **Current Version**: 1.8.3 (for Elasticsearch 8.17.0)
- **License**: Apache License 2.0
- **Language**: Java
- **Build System**: Maven
- **Architecture**: Standard Elasticsearch plugin extending `Plugin` and implementing `ActionPlugin`

### Current Dependencies
- **Elasticsearch**: 8.17.0
- **Jackson Core**: 2.17.0
- **Jackson Databind**: 2.17.0
- **JDK Version**: 17
- **Maven**: Standard Maven-based build system

### Key Components
1. **Main Plugin Class**: `ZentityPlugin` extending Elasticsearch's `Plugin` class
2. **REST Actions**: HomeAction, ModelsAction, ResolutionAction, SetupAction
3. **Core Logic**: Entity resolution algorithms in `Job.java`
4. **Testing**: Comprehensive integration tests using Docker Compose with Elasticsearch containers

## OpenSearch Compatibility Analysis

### Version Compatibility Matrix

| Component | Current (Elasticsearch) | Target (OpenSearch) | Compatibility Status |
|-----------|------------------------|-------------------|---------------------|
| Core Platform | Elasticsearch 8.17.0 | OpenSearch 2.x/3.x | ✅ Supported with modifications |
| Java API | `org.elasticsearch.*` | `org.opensearch.*` | ⚠️ Package names changed |
| REST APIs | Elasticsearch REST API | OpenSearch REST API | ✅ Backward compatible |
| Plugin Architecture | Elasticsearch Plugin API | OpenSearch Plugin API | ✅ Compatible with changes |
| Build System | Maven | Maven | ✅ Fully compatible |
| License | Apache 2.0 | Apache 2.0 | ✅ Fully compatible |

### OpenSearch Version Recommendations

#### OpenSearch 2.x (Recommended for Initial Migration)
- **Latest**: OpenSearch 2.17 (released 2024)
- **Advantages**: 
  - Backward compatible with Elasticsearch 7.10 APIs
  - Mature and stable
  - Extensive documentation and community support
  - Lower migration complexity
- **Considerations**:
  - Uses Lucene 9.x
  - Requires JDK 11+
  - Well-established plugin ecosystem

#### OpenSearch 3.0 (Future Consideration)
- **Status**: Recently released (January 2025)
- **Major Changes**:
  - Upgraded to Lucene 10
  - Requires JDK 21+
  - Significant performance improvements
  - Breaking changes from Lucene 10 upgrade
- **Recommendation**: Consider for future migration after 2.x migration is stable

## Migration Requirements

### 1. Dependency Migration

#### Maven POM Updates
```xml
<!-- Replace Elasticsearch dependencies -->
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

#### Version Property Updates
```xml
<properties>
    <opensearch.version>2.17.0</opensearch.version>
    <!-- Remove elasticsearch.version -->
</properties>
```

### 2. Package Name Migration

#### Required Import Changes
```java
// FROM (Elasticsearch)
import org.elasticsearch.plugin.zentity.*;
import org.elasticsearch.rest.*;
import org.elasticsearch.cluster.*;
import org.elasticsearch.client.*;

// TO (OpenSearch)
import org.opensearch.plugin.zentity.*;
import org.opensearch.rest.*;
import org.opensearch.cluster.*;
import org.opensearch.client.*;
```

#### Systematic Package Replacement
- `org.elasticsearch.*` → `org.opensearch.*`
- All imports in Java files need updating
- Configuration file references need updating

### 3. Plugin Configuration Updates

#### Plugin Descriptor Changes
```properties
# src/main/resources/plugin-descriptor.properties
classname=${zentity.classname}
description=${project.description}
opensearch.version=${opensearch.version}  # Changed from elasticsearch.version
java.version=${jdk.version}
name=${project.artifactId}
version=${project.version}
```

#### Class Name Updates
```xml
<!-- pom.xml -->
<zentity.classname>org.opensearch.plugin.zentity.ZentityPlugin</zentity.classname>
```

### 4. API Compatibility Changes

#### REST Handler Updates
```java
// Most REST handlers remain compatible, but package imports change
public class HomeAction extends BaseRestHandler {
    // Implementation mostly unchanged
    // Only import statements need updating
}
```

#### Client API Changes
```java
// NodeClient usage remains largely the same
// OpenSearch maintains API compatibility with Elasticsearch 7.10
// JSON parsing and response handling unchanged
```

### 5. Testing Infrastructure Migration

#### Docker Compose Updates
```yaml
# test/resources/docker-compose.yml
services:
  opensearch:
    image: opensearch/opensearch:2.17.0  # Changed from elasticsearch
    environment:
      - discovery.type=single-node
      - plugins.security.disabled=true  # OpenSearch security config
```

#### Integration Test Updates
```java
// Update test container dependencies
// org.testcontainers references may need OpenSearch-specific configurations
// Test data and fixtures should remain compatible
```

## Breaking Changes and Considerations

### Major Breaking Changes
1. **Package Namespace**: All `org.elasticsearch` packages renamed to `org.opensearch`
2. **Plugin Descriptor**: Configuration key changes
3. **Security Plugin**: OpenSearch has security enabled by default
4. **Docker Images**: Different container images and configuration

### Minor Compatibility Issues
1. **Version Checks**: Some clients may check version strings
2. **Cluster Settings**: Some cluster setting names may have changed
3. **Deprecated APIs**: Some APIs deprecated in Elasticsearch may be removed in OpenSearch

### OpenSearch 3.0 Specific Considerations
1. **JDK 21 Requirement**: Minimum JDK version increased
2. **Lucene 10**: Significant internal changes
3. **Breaking Changes**: More extensive breaking changes from major version upgrade

## Migration Strategy

### Phase 1: Analysis and Preparation
1. **Code Analysis**: Inventory all Elasticsearch dependencies and imports
2. **Test Environment Setup**: Create OpenSearch test environment
3. **Compatibility Testing**: Test existing functionality with OpenSearch
4. **Documentation Review**: Review OpenSearch migration guides

### Phase 2: Core Migration
1. **Dependency Updates**: Update all Maven dependencies
2. **Package Migration**: Systematically update all import statements
3. **Configuration Updates**: Update plugin descriptor and configuration files
4. **Build System Updates**: Ensure Maven build works with OpenSearch

### Phase 3: Testing and Validation
1. **Unit Testing**: Ensure all unit tests pass
2. **Integration Testing**: Update and run integration tests
3. **Functional Testing**: Validate entity resolution functionality
4. **Performance Testing**: Compare performance with Elasticsearch version

### Phase 4: Documentation and Deployment
1. **Update Documentation**: Update README and documentation
2. **Migration Guide**: Create migration guide for users
3. **Release Preparation**: Prepare new release for OpenSearch compatibility

## Implementation Roadmap

### Milestone 1: Environment Setup (Week 1)
- [ ] Set up OpenSearch development environment
- [ ] Create test OpenSearch cluster
- [ ] Validate basic connectivity and operations

### Milestone 2: Core Migration (Weeks 2-3)
- [ ] Update Maven dependencies
- [ ] Migrate package imports
- [ ] Update plugin configuration
- [ ] Resolve compilation errors

### Milestone 3: Testing and Validation (Weeks 4-5)
- [ ] Update integration tests
- [ ] Validate entity resolution algorithms
- [ ] Performance testing and optimization
- [ ] Edge case testing

### Milestone 4: Final Integration (Week 6)
- [ ] Documentation updates
- [ ] Release preparation
- [ ] User migration guide
- [ ] Community announcement

## Technical Considerations

### Plugin Architecture Compatibility
- OpenSearch maintains plugin architecture compatibility with Elasticsearch 7.x
- `ActionPlugin` interface remains largely unchanged
- REST handler patterns are preserved
- Node client functionality is maintained

### Entity Resolution Logic Preservation
- Core entity resolution algorithms should remain unchanged
- JSON processing and data structures are compatible
- Search API usage patterns are preserved
- Index operations remain compatible

### Performance Considerations
- OpenSearch 2.x provides similar performance to Elasticsearch 8.x
- OpenSearch 3.0 offers significant performance improvements
- Vector search capabilities enhanced in newer versions
- Memory usage patterns should remain similar

## Risk Assessment

### High Risk Items
1. **Complex Entity Resolution Logic**: Core algorithms must be thoroughly tested
2. **Integration Dependencies**: External system integrations need validation
3. **Custom Configuration**: Any custom Elasticsearch configurations need review

### Medium Risk Items
1. **Performance Changes**: Potential performance differences between platforms
2. **Security Configuration**: OpenSearch security defaults differ from Elasticsearch
3. **Client Libraries**: Third-party client libraries may need updates

### Low Risk Items
1. **Basic REST Operations**: Standard CRUD operations are highly compatible
2. **JSON Processing**: Data formats and processing remain unchanged
3. **Build System**: Maven integration is straightforward

## Resource Requirements

### Development Resources
- **Java Developers**: 2-3 developers familiar with Elasticsearch plugins
- **Testing Resources**: QA engineer for comprehensive testing
- **DevOps Support**: Infrastructure setup and deployment automation

### Time Estimates
- **Total Migration Time**: 6-8 weeks
- **Core Development**: 3-4 weeks
- **Testing and Validation**: 2-3 weeks
- **Documentation and Release**: 1 week

### Infrastructure Requirements
- **Development Environment**: OpenSearch cluster for development
- **Testing Environment**: Staging environment with realistic data
- **CI/CD Updates**: Update build pipelines for OpenSearch

## Success Metrics

### Technical Metrics
1. **Functionality Parity**: 100% of current features working in OpenSearch
2. **Performance Parity**: Performance within 5% of current Elasticsearch implementation
3. **Test Coverage**: Maintain or improve current test coverage levels
4. **Build Success**: Clean builds without warnings or errors

### Business Metrics
1. **Migration Timeline**: Complete migration within planned timeline
2. **User Impact**: Minimal disruption to existing users
3. **Feature Completeness**: All entity resolution features preserved
4. **Documentation Quality**: Comprehensive migration and usage documentation

## Recommendations

### Immediate Actions
1. **Start with OpenSearch 2.x**: Begin migration with mature OpenSearch 2.17 release
2. **Systematic Approach**: Follow phased migration strategy to minimize risks
3. **Comprehensive Testing**: Invest heavily in testing to ensure functionality parity
4. **Documentation Priority**: Create thorough documentation for users and developers

### Future Considerations
1. **OpenSearch 3.0 Migration**: Plan future migration to leverage performance improvements
2. **OpenSearch Ecosystem**: Explore OpenSearch-specific features and enhancements
3. **Community Engagement**: Engage with OpenSearch community for best practices
4. **Continuous Monitoring**: Monitor OpenSearch roadmap for future planning

### Migration Best Practices
1. **Backup Everything**: Complete backup of current working implementation
2. **Parallel Development**: Maintain Elasticsearch version during migration
3. **User Communication**: Keep users informed of migration progress and timelines
4. **Gradual Rollout**: Consider phased rollout to production environments

## Conclusion

The migration of Zentity from Elasticsearch 8.17.0 to OpenSearch is highly feasible and recommended. OpenSearch provides a compatible and legally unencumbered alternative to Elasticsearch with maintained API compatibility, strong community support, and continuous development.

The migration effort is primarily focused on dependency updates and package name changes, with the core entity resolution logic remaining largely unchanged. The estimated 6-8 week timeline provides adequate time for thorough testing and validation while minimizing business disruption.

OpenSearch 2.x provides an excellent target for initial migration, with the option to consider OpenSearch 3.0 in the future for enhanced performance and features. The migration will position Zentity for long-term sustainability and growth within the open-source ecosystem.

### Key Success Factors
1. Systematic approach to dependency and package migration
2. Comprehensive testing across all functionality
3. Clear communication with users and stakeholders
4. Proper documentation of changes and migration procedures

The migration represents not just a technical upgrade but a strategic move toward a more open and community-driven platform that aligns with the project's Apache 2.0 licensing and open-source values. 