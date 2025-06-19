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
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.xcontent.DeprecationHandler;
import org.opensearch.core.xcontent.NamedXContentRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class for JSON operations using OpenSearch's XContent API instead of Jackson.
 * This avoids classloader issues with Jackson dependencies.
 */
public class XContentJson {
    
    /**
     * Parse a JSON string and return a map representation.
     * 
     * @param jsonString the JSON string to parse
     * @return map representation of the JSON
     * @throws IOException if parsing fails
     */
    public static Map<String, Object> parseToMap(String jsonString) throws IOException {
        try (XContentParser parser = XContentType.JSON.xContent()
                .createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, jsonString)) {
            
            return parseObject(parser);
        }
    }
    
    /**
     * Parse a JSON object from the current parser position.
     * 
     * @param parser the XContent parser
     * @return map representation of the JSON object
     * @throws IOException if parsing fails
     */
    public static Map<String, Object> parseObject(XContentParser parser) throws IOException {
        Map<String, Object> map = new HashMap<>();
        
        XContentParser.Token token = parser.currentToken();
        if (token == null) {
            token = parser.nextToken();
        }
        
        if (token != XContentParser.Token.START_OBJECT) {
            throw new IllegalArgumentException("Expected JSON object, got: " + token);
        }
        
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                String fieldName = parser.currentName();
                token = parser.nextToken();
                Object value = parseValue(parser, token);
                map.put(fieldName, value);
            }
        }
        
        return map;
    }
    
    /**
     * Parse a JSON array from the current parser position.
     * 
     * @param parser the XContent parser
     * @return list representation of the JSON array
     * @throws IOException if parsing fails
     */
    public static List<Object> parseArray(XContentParser parser) throws IOException {
        List<Object> list = new ArrayList<>();
        
        XContentParser.Token token = parser.currentToken();
        if (token != XContentParser.Token.START_ARRAY) {
            throw new IllegalArgumentException("Expected JSON array, got: " + token);
        }
        
        while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
            Object value = parseValue(parser, token);
            list.add(value);
        }
        
        return list;
    }
    
    /**
     * Parse a value from the current parser position.
     * 
     * @param parser the XContent parser
     * @param token the current token
     * @return the parsed value
     * @throws IOException if parsing fails
     */
    public static Object parseValue(XContentParser parser, XContentParser.Token token) throws IOException {
        switch (token) {
            case VALUE_STRING:
                return parser.text();
            case VALUE_NUMBER:
                return parser.numberValue();
            case VALUE_BOOLEAN:
                return parser.booleanValue();
            case VALUE_NULL:
                return null;
            case START_OBJECT:
                return parseObject(parser);
            case START_ARRAY:
                return parseArray(parser);
            default:
                throw new IllegalArgumentException("Unexpected token: " + token);
        }
    }
    
    /**
     * Parse a JSON string and return a map of string values.
     * 
     * @param jsonString the JSON string to parse
     * @return map of field names to string values
     * @throws IOException if parsing fails
     */
    public static Map<String, String> toStringMap(String jsonString) throws IOException {
        Map<String, String> map = new TreeMap<>();
        
        try (XContentParser parser = XContentType.JSON.xContent()
                .createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, jsonString)) {
            
            if (parser.nextToken() != XContentParser.Token.START_OBJECT) {
                throw new IllegalArgumentException("Expected JSON object");
            }
            
            while (parser.nextToken() != XContentParser.Token.END_OBJECT) {
                if (parser.currentToken() == XContentParser.Token.FIELD_NAME) {
                    String fieldName = parser.currentName();
                    parser.nextToken();
                    
                    switch (parser.currentToken()) {
                        case VALUE_STRING:
                            map.put(fieldName, parser.text());
                            break;
                        case VALUE_NUMBER:
                            map.put(fieldName, parser.text());
                            break;
                        case VALUE_BOOLEAN:
                            map.put(fieldName, Boolean.toString(parser.booleanValue()));
                            break;
                        case VALUE_NULL:
                            map.put(fieldName, "null");
                            break;
                        case START_OBJECT:
                        case START_ARRAY:
                            // For complex objects/arrays, convert to JSON string
                            try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
                                builder.copyCurrentStructure(parser);
                                map.put(fieldName, builder.toString());
                            }
                            break;
                        default:
                            map.put(fieldName, parser.text());
                    }
                }
            }
        }
        
        return map;
    }
    
    /**
     * Convert a map to JSON string.
     * 
     * @param map the map to convert
     * @return JSON string representation
     * @throws IOException if conversion fails
     */
    public static String mapToJson(Map<String, Object> map) throws IOException {
        try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
            builder.startObject();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                builder.field(entry.getKey(), entry.getValue());
            }
            builder.endObject();
            return builder.toString();
        }
    }
    
    /**
     * Pretty-print a JSON string.
     * 
     * @param json the JSON string to pretty-print
     * @return the pretty-printed JSON string
     * @throws IOException if parsing/formatting fails
     */
    public static String pretty(String json) throws IOException {
        try (XContentParser parser = XContentType.JSON.xContent()
                .createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, json);
             XContentBuilder builder = XContentFactory.jsonBuilder().prettyPrint()) {
            
            builder.copyCurrentStructure(parser);
            return builder.toString();
        }
    }
    
    /**
     * Quote a string for JSON.
     * 
     * @param value the string to quote
     * @return the quoted string
     */
    public static String quoteString(String value) {
        if (value == null) {
            return "null";
        }
        
        try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
            builder.startObject();
            builder.field("value", value);
            builder.endObject();
            String result = builder.toString();
            // Extract just the quoted value part
            int start = result.indexOf(":") + 1;
            int end = result.lastIndexOf("}");
            return result.substring(start, end).trim();
        } catch (IOException e) {
            // Fallback to simple quoting
            return "\"" + value.replace("\"", "\\\"") + "\"";
        }
    }
    
    /**
     * Check if a map represents a valid JSON object structure.
     * 
     * @param map the map to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidObjectMap(Map<String, Object> map) {
        if (map == null) {
            return false;
        }
        
        // Check if all required fields are present and are objects
        String[] requiredFields = {"attributes", "resolvers", "matchers", "indices"};
        for (String field : requiredFields) {
            if (!map.containsKey(field)) {
                return false;
            }
            Object value = map.get(field);
            if (!(value instanceof Map)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get a nested map from a parent map.
     * 
     * @param parentMap the parent map
     * @param key the key to look up
     * @return the nested map, or empty map if not found
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getNestedMap(Map<String, Object> parentMap, String key) {
        Object value = parentMap.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }
    
    /**
     * Get a string value from a map.
     * 
     * @param map the map
     * @param key the key
     * @param defaultValue the default value if key not found
     * @return the string value
     */
    public static String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        if (value != null) {
            return value.toString();
        }
        return defaultValue;
    }
    
    /**
     * Check if a map contains a key and the value is not null.
     * 
     * @param map the map
     * @param key the key
     * @return true if key exists and value is not null
     */
    public static boolean hasNonNullValue(Map<String, Object> map, String key) {
        return map.containsKey(key) && map.get(key) != null;
    }
} 