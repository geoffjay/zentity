/*
 * zentity
 * Copyright © 2018-2025 Dave Moore
 * https://zentity.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opensearch.plugin.zentity;

import org.opensearch.OpenSearchSecurityException;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.node.DiscoveryNodes;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.IndexScopedSettings;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.SettingsFilter;
import org.opensearch.rest.RestController;
import org.opensearch.rest.RestHandler;
import org.opensearch.plugins.ActionPlugin;
import org.opensearch.plugins.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

// Exception classes needed by other components
class NotFoundException extends Exception {
    public NotFoundException(String message) {
        super(message);
    }
}

class NotImplementedException extends Exception {
    NotImplementedException(String message) {
        super(message);
    }
}

class ForbiddenException extends OpenSearchSecurityException {
    ForbiddenException(String message) {
        super(message);
    }
}

public class ZentityPluginMinimal extends Plugin implements ActionPlugin {

    private static final Properties properties = new Properties();

    public ZentityPluginMinimal() throws IOException {
        Properties zentityProperties = new Properties();
        Properties pluginDescriptorProperties = new Properties();
        InputStream zentityStream = this.getClass().getResourceAsStream("/zentity.properties");
        InputStream pluginDescriptorStream = this.getClass().getResourceAsStream("/plugin-descriptor.properties");
        zentityProperties.load(zentityStream);
        pluginDescriptorProperties.load(pluginDescriptorStream);
        properties.putAll(zentityProperties);
        properties.putAll(pluginDescriptorProperties);
    }

    public static Properties properties() {
        return properties;
    }

    public String version() {
        return properties.getProperty("version");
    }

    @Override
    public List<RestHandler> getRestHandlers(
            Settings settings,
            RestController restController,
            ClusterSettings clusterSettings,
            IndexScopedSettings indexScopedSettings,
            SettingsFilter settingsFilter,
            IndexNameExpressionResolver indexNameExpressionResolver,
            Supplier<DiscoveryNodes> nodesInCluster) {
        
        return List.of(
            new HomeAction(),
            new ModelsAction(),
            new ResolutionAction()
        );
    }
    
    // Static utility methods for response handling
    public static void sendResponse(org.opensearch.rest.RestChannel channel, String content) {
        try {
            channel.sendResponse(new org.opensearch.rest.BytesRestResponse(org.opensearch.core.rest.RestStatus.OK, "application/json", content));
        } catch (Exception e) {
            sendResponseError(channel, org.apache.logging.log4j.LogManager.getLogger(ZentityPluginMinimal.class), e);
        }
    }
    
    public static void sendResponse(org.opensearch.rest.RestChannel channel, org.opensearch.core.xcontent.XContentBuilder content) {
        try {
            channel.sendResponse(new org.opensearch.rest.BytesRestResponse(org.opensearch.core.rest.RestStatus.OK, content));
        } catch (Exception e) {
            sendResponseError(channel, org.apache.logging.log4j.LogManager.getLogger(ZentityPluginMinimal.class), e);
        }
    }
    
    public static void sendResponseError(org.opensearch.rest.RestChannel channel, org.apache.logging.log4j.Logger logger, Exception e) {
        try {
            org.opensearch.core.rest.RestStatus status = org.opensearch.core.rest.RestStatus.INTERNAL_SERVER_ERROR;
            
            // Map specific exception types to appropriate HTTP status codes
            if (e instanceof NotFoundException) {
                status = org.opensearch.core.rest.RestStatus.NOT_FOUND;
            } else if (e instanceof ForbiddenException) {
                status = org.opensearch.core.rest.RestStatus.FORBIDDEN;
            } else if (e instanceof NotImplementedException) {
                status = org.opensearch.core.rest.RestStatus.NOT_IMPLEMENTED;
            } else if (e instanceof io.zentity.model.ValidationException) {
                status = org.opensearch.core.rest.RestStatus.BAD_REQUEST;
            }
            
            channel.sendResponse(new org.opensearch.rest.BytesRestResponse(status, e.getMessage()));
        } catch (Exception sendException) {
            logger.error("Failed to send error response", sendException);
        }
    }
} 