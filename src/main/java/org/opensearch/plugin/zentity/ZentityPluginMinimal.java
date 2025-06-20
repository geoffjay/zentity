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

import io.zentity.common.Json;
import io.zentity.model.ValidationException;
import org.apache.logging.log4j.Logger;
import org.opensearch.OpenSearchException;
import org.opensearch.OpenSearchSecurityException;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.node.DiscoveryNodes;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.IndexScopedSettings;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.SettingsFilter;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestChannel;
import org.opensearch.rest.RestController;
import org.opensearch.rest.RestHandler;
import org.opensearch.plugins.ActionPlugin;
import org.opensearch.plugins.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

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
    public ForbiddenException(String message) {
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
        
        // Return all REST handlers including ResolutionAction
        return Arrays.asList(
                new HomeAction(),
                new ModelsAction(),
                new ResolutionAction(),
                new SetupAction()
        );
    }
    
    /**
     * Return an error response through a RestChannel.
     * This method is used by the action classes in org.opensearch.plugin.zentity.
     *
     * @param channel The REST channel to return the response through.
     * @param logger  The logger to use for logging errors.
     * @param e       The exception object to process and return.
     */
    protected static void sendResponseError(RestChannel channel, Logger logger, Exception e) {
        try {
            // Handle known types of errors.
            if (e instanceof ForbiddenException) {
                channel.sendResponse(new BytesRestResponse(RestStatus.FORBIDDEN, e.getMessage()));
            } else if (e instanceof ValidationException) {
                channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, e.getMessage()));
            } else if (e instanceof NotFoundException) {
                channel.sendResponse(new BytesRestResponse(RestStatus.NOT_FOUND, e.getMessage()));
            } else if (e instanceof NotImplementedException) {
                channel.sendResponse(new BytesRestResponse(RestStatus.NOT_IMPLEMENTED, e.getMessage()));
            } else if (e instanceof OpenSearchException) {
                // Any other OpenSearchException which has its own status code.
                channel.sendResponse(new BytesRestResponse(((OpenSearchException) e).status(), e.getMessage()));
            } else {
                // Log the stack trace for unexpected types of errors.
                logger.catching(e);
                channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
            }
        } catch (Exception ee) {
            // Unexpected error when processing the exception object.
            // Since BytesRestResponse throws IOException, build this response object as a string
            // to handle any IOException that find its way here.
            logger.catching(ee);
            String message = "{\"error\":{\"root_cause\":[{\"type\":\"exception\",\"reason\":" + Json.quoteString(ee.getMessage()) + "}],\"type\":\"exception\",\"reason\":" + Json.quoteString(ee.getMessage()) + "},\"status\":500}";
            channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, "application/json", message));
        }
    }
    
    /**
     * Return a response through a RestChannel.
     * This method is used by the action classes in org.opensearch.plugin.zentity.
     *
     * @param channel The REST channel to return the response through.
     * @param content The XContentBuilder to process and return.
     */
    protected static void sendResponse(RestChannel channel, XContentBuilder content) {
        sendResponse(channel, RestStatus.OK, StringsUtil.toString(content));
    }
    
    /**
     * Return a response through a RestChannel.
     * This method is used by the action classes in org.opensearch.plugin.zentity.
     *
     * @param channel The REST channel to return the response through.
     * @param json    The JSON string to process and return.
     */
    protected static void sendResponse(RestChannel channel, String json) {
        sendResponse(channel, RestStatus.OK, json);
    }
    
    /**
     * Return a response through a RestChannel.
     * This method is used by the action classes in org.opensearch.plugin.zentity.
     *
     * @param channel The REST channel to return the response through.
     * @param statusCode The HTTP status code.
     * @param json    The JSON string to process and return.
     */
    protected static void sendResponse(RestChannel channel, RestStatus statusCode, String json) {
        channel.sendResponse(new BytesRestResponse(statusCode, "application/json", json));
    }
} 