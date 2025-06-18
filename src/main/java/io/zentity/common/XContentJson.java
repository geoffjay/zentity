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
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class for JSON operations using OpenSearch's XContent API instead of Jackson.
 * This avoids classloader issues with Jackson dependencies.
 */
public class XContentJson {
    
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
} 