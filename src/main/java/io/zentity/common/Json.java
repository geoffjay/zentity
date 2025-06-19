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
package io.zentity.common;

import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.common.xcontent.json.JsonXContent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class Json {

    // Temporary compatibility fields for gradual migration
    // TODO: Remove these after completing full XContent migration
    public static final CompatibilityMapper MAPPER = new CompatibilityMapper();
    public static final CompatibilityMapper ORDERED_MAPPER = new CompatibilityMapper();

    /**
     * Temporary compatibility class to help with gradual migration from Jackson.
     * This returns actual Jackson JsonNode types to maintain compatibility with existing code.
     */
    public static class CompatibilityMapper {
        private final ObjectMapper objectMapper = new ObjectMapper();
        
        public JsonNode readTree(String jsonString) throws IOException {
            return objectMapper.readTree(jsonString);
        }
        
        public String writeValueAsString(Object value) throws IOException {
            return objectMapper.writeValueAsString(value);
        }
        
        public ArrayNode createArrayNode() {
            return objectMapper.createArrayNode();
        }
        
        public ObjectNode createObjectNode() {
            return objectMapper.createObjectNode();
        }
        
        /**
         * Helper method to add Object values to ArrayNode.
         * Handles type conversion from Object to appropriate JsonNode types.
         */
        public void addToArrayNode(ArrayNode arrayNode, Object value) {
            if (value == null) {
                arrayNode.addNull();
            } else if (value instanceof String) {
                arrayNode.add((String) value);
            } else if (value instanceof Integer) {
                arrayNode.add((Integer) value);
            } else if (value instanceof Long) {
                arrayNode.add((Long) value);
            } else if (value instanceof Double) {
                arrayNode.add((Double) value);
            } else if (value instanceof Float) {
                arrayNode.add((Float) value);
            } else if (value instanceof Boolean) {
                arrayNode.add((Boolean) value);
            } else if (value instanceof JsonNode) {
                arrayNode.add((JsonNode) value);
            } else {
                // Fallback: convert to string
                arrayNode.add(value.toString());
            }
        }
    }

    /**
     * Quote a string value for JSON output.
     *
     * @param value The string value to quote.
     * @return The quoted string.
     */
    public static String quoteString(String value) {
        return jsonStringFormat(value);
    }

    /**
     * Escape a string value for JSON.
     *
     * @param value The string value to escape.
     * @return The escaped string.
     */
    private static String jsonStringEscape(String value) {
        if (value == null)
            return "null";
        
        // Simple JSON string escaping
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Add quotes around a string value.
     *
     * @param value The string value to quote.
     * @return The quoted string.
     */
    private static String jsonStringQuote(String value) {
        return "\"" + value + "\"";
    }

    /**
     * Format a string value for JSON output.
     *
     * @param value The string value to format.
     * @return The formatted JSON string.
     */
    private static String jsonStringFormat(String value) {
        return jsonStringQuote(jsonStringEscape(value));
    }

    /**
     * Converts a Map to a TreeMap of strings for consistent ordering.
     *
     * @param map The input map.
     * @return The string map representation.
     * @throws IOException If there is an issue processing the map.
     */
    public static Map<String, String> toStringMap(Map<String, Object> map) throws IOException {
        Map<String, String> stringMap = new TreeMap<>();
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null) {
                stringMap.put(key, "null");
            } else if (value instanceof String) {
                stringMap.put(key, (String) value);
            } else if (value instanceof Number || value instanceof Boolean) {
                stringMap.put(key, value.toString());
            } else if (value instanceof Map || value instanceof Iterable) {
                // For complex objects, convert to JSON string
                try (XContentBuilder builder = JsonXContent.contentBuilder()) {
                    builder.value(value);
                    stringMap.put(key, builder.toString());
                }
            } else {
                stringMap.put(key, value.toString());
            }
        }
        
        return stringMap;
    }

    /**
     * Converts a JSON string to a Map of strings.
     *
     * @param jsonString The JSON string to parse.
     * @return The string map representation.
     * @throws IOException If there is an issue parsing the JSON.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> toStringMap(String jsonString) throws IOException {
        try (XContentParser parser = JsonXContent.jsonXContent.createParser(null, null, jsonString)) {
            Map<String, Object> map = parser.map();
            return toStringMap(map);
        }
    }

    /**
     * Pretty-print a JSON string.
     *
     * @param json The JSON string to format.
     * @return The pretty-printed JSON string.
     * @throws IOException If there is an issue parsing or formatting the JSON.
     */
    public static String pretty(String json) throws IOException {
        try (XContentParser parser = JsonXContent.jsonXContent.createParser(null, null, json)) {
            try (XContentBuilder builder = JsonXContent.contentBuilder().prettyPrint()) {
                builder.copyCurrentStructure(parser);
                return builder.toString();
            }
        }
    }

    /**
     * Parse a JSON string to a Map.
     *
     * @param jsonString The JSON string to parse.
     * @return The parsed map.
     * @throws IOException If there is an issue parsing the JSON.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseToMap(String jsonString) throws IOException {
        try (XContentParser parser = JsonXContent.jsonXContent.createParser(null, null, jsonString)) {
            return parser.map();
        }
    }
}
