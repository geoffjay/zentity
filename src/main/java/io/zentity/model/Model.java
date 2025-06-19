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
package io.zentity.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.zentity.common.Json;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.xcontent.DeprecationHandler;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import io.zentity.common.Patterns;
import org.opensearch.OpenSearchException;
import org.opensearch.plugin.zentity.StringsUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;

public class Model {

    public static final Set<String> REQUIRED_FIELDS = new TreeSet<>(
            Arrays.asList("attributes", "resolvers", "matchers", "indices")
    );

    private Map<String, Attribute> attributes = new TreeMap<>();
    private Map<String, Index> indices = new TreeMap<>();
    private Map<String, Matcher> matchers = new TreeMap<>();
    private Map<String, Resolver> resolvers = new TreeMap<>();
    private boolean validateRunnable = false;

    public Model(JsonNode json) throws ValidationException, JsonProcessingException {
        this.deserialize(json);
    }

    public Model(String json) throws ValidationException, IOException {
        this.deserialize(json);
    }

    public Model(JsonNode json, boolean validateRunnable) throws ValidationException, JsonProcessingException {
        this.validateRunnable = validateRunnable;
        this.deserialize(json);
    }

    public Model(String json, boolean validateRunnable) throws ValidationException, IOException {
        this.validateRunnable = validateRunnable;
        this.deserialize(json);
    }
    
    /**
     * Constructor that creates a Model from a Map (from OpenSearch GetResponse.getSourceAsMap()).
     * This is a temporary workaround to avoid Jackson classloader issues during OpenSearch migration.
     */
    public Model(Map<String, Object> modelMap, boolean validateRunnable) throws ValidationException {
        this.validateRunnable = validateRunnable;
        this.deserializeFromMap(modelMap);
    }

    public Map<String, Attribute> attributes() {
        return this.attributes;
    }

    public Map<String, Index> indices() {
        return this.indices;
    }

    public Map<String, Matcher> matchers() {
        return this.matchers;
    }

    public Map<String, Resolver> resolvers() {
        return this.resolvers;
    }

    public static final int MAX_STRICT_NAME_BYTES = 255;

    /**
     * Validate the name of an entity type, attribute, resolver, or matcher.
     * The name requirements are the same as the Elasticsearch index name requirements.
     *
     * @param name  The name of the entity type, attribute, resolver, or matcher.
     * @return an optional ValidationException if the type is not in a valid format.
     * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/7.10/indices-create-index.html#indices-create-api-path-params">Elasticsearch Index Name Requirements</a>
     * @see org.elasticsearch.cluster.metadata.MetadataCreateIndexService#validateIndexOrAliasName
     */
    public static void validateStrictName(String name) throws ValidationException {
        BiFunction<String, String, String> msg = (invalidName, description) -> "Invalid name [" + invalidName + "], " + description;
        if (name == null)
            throw new ValidationException(msg.apply("", "must not be empty"));
        if (Patterns.EMPTY_STRING.matcher(name).matches())
            throw new ValidationException(msg.apply(name, "must not be empty"));
        if (!StringsUtil.validFileName(name))
            throw new ValidationException(msg.apply(name, "must not contain the following characters: " + StringsUtil.INVALID_FILENAME_CHARS));
        if (name.contains("#"))
            throw new ValidationException(msg.apply(name, "must not contain '#'"));
        if (name.contains(":"))
            throw new ValidationException(msg.apply(name, "must not contain ':'"));
        if (name.charAt(0) == '_' || name.charAt(0) == '-' || name.charAt(0) == '+')
            throw new ValidationException(msg.apply(name, "must not start with '_', '-', or '+'"));
        int byteCount = 0;
        try {
            byteCount = name.getBytes("UTF-8").length;
        } catch (UnsupportedEncodingException e) {
            // UTF-8 should always be supported, but rethrow this if it is not for some reason
            throw new OpenSearchException("Unable to determine length of name [" + name + "]", e);
        }
        if (byteCount > MAX_STRICT_NAME_BYTES)
            throw new ValidationException(msg.apply(name, "name is too long, (" + byteCount + " > " + MAX_STRICT_NAME_BYTES + ")"));
        if (name.equals(".") || name.equals(".."))
            throw new ValidationException(msg.apply(name,  "must not be '.' or '..'"));
        if (!name.toLowerCase(Locale.ROOT).equals(name))
            throw new ValidationException(msg.apply(name,  "must be lowercase"));
    }

    /**
     * Validate the nesting of attribute names. An attribute name is invalid if another attribute name overrides it.
     *
     * Example 1 (valid):   "name.first", "name.last"
     *
     *      Both attributes can hold values in the resolution response because their names do not conflict.
     *
     * Example 2 (invalid): "name.first", "name.last", "name"
     *
     *      The "name" attribute cannot hold values in the resolution response because it must hold the "name.first"
     *      and "name.last" attributes. The "name" attribute should be removed or renamed to something appropriate
     *      such as "name.full".
     *
     *
     * @throws ValidationException
     */
    private void validateAttributeNesting() throws ValidationException {
        List<String> names = new ArrayList<>(this.attributes.keySet());
        int size = names.size();
        for (int a = 0; a < size; a++) {
            String nameA = names.get(a) + ".";
            for (int b = 0; b < size; b++) {
                if (a == b)
                    continue;
                String nameB = names.get(b) + ".";
                if (nameA.startsWith(nameB))
                   throw new ValidationException("'attributes." + names.get(b) + "' is invalid because 'attributes." + names.get(a) + "' overrides its name.");
            }
        }
    }

    /**
     * Validate a top-level field of the entity model.
     *
     * @param json  JSON object.
     * @param field Field name.
     * @throws ValidationException
     */
    private void validateField(JsonNode json, String field) throws ValidationException {
        if (!json.get(field).isObject())
            throw new ValidationException("'" + field + "' must be an object.");
        if (this.validateRunnable) {
            if (json.get(field).size() == 0) {
                // Clarifying "in the entity model" because this exception likely will appear only for resolution requests,
                // and the user might think that the message is referring to the input instead of the entity model.
                throw new ValidationException("'" + field + "' must not be empty in the entity model.");
            }
        }
    }

    /**
     * Validate the object of a top-level field of the entity model.
     *
     * @param field  Field name.
     * @param object JSON object.
     * @throws ValidationException
     */
    private void validateObject(String field, JsonNode object) throws ValidationException {
        if (!object.isObject())
            throw new ValidationException("'" + field + "' must be an object.");
        if (this.validateRunnable) {
            if (object.size() == 0) {
                // Clarifying "in the entity model" because this exception likely will appear only for resolution requests,
                // and the user might think that the message is referring to the input instead of the entity model.
                throw new ValidationException("'" + field + "' must not be empty in the entity model.");
            }
        }

    }

    public void deserialize(JsonNode json) throws ValidationException, JsonProcessingException {
        if (!json.isObject())
            throw new ValidationException("Entity model must be an object.");

        // Validate the existence of required fields.
        for (String field : REQUIRED_FIELDS)
            if (!json.has(field))
                throw new ValidationException("Entity model is missing required field '" + field + "'.");

        // Validate and hold the state of fields.
        Iterator<Map.Entry<String, JsonNode>> fields = json.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldObject = field.getValue();
            validateField(json, fieldName);
            validateObject(fieldName, fieldObject);
            Iterator<Map.Entry<String, JsonNode>> children = json.get(fieldName).fields();
            while (children.hasNext()) {
                Map.Entry<String, JsonNode> child = children.next();
                String name = child.getKey();
                JsonNode object = child.getValue();
                switch (fieldName) {
                    case "attributes":
                        this.attributes.put(name, new Attribute(name, object, this.validateRunnable));
                        break;
                    case "indices":
                        this.indices.put(name, new Index(name, object, this.validateRunnable));
                        break;
                    case "matchers":
                        this.matchers.put(name, new Matcher(name, object, this.validateRunnable));
                        break;
                    case "resolvers":
                        this.resolvers.put(name, new Resolver(name, object, this.validateRunnable));
                        break;
                    default:
                        throw new ValidationException("'" + fieldName + "' is not a recognized field.");
                }
            }
        }
        this.validateAttributeNesting();
    }

    public void deserialize(String json) throws ValidationException, IOException {
        // Temporarily disable JSON parsing to avoid Jackson issues
        // TODO: Implement full XContent-based parsing
        throw new ValidationException("Model deserialization from JSON string is temporarily disabled during OpenSearch migration. Please use the JsonNode constructor instead.");
    }
    
    /**
     * Deserialize model from a Map (temporary workaround for OpenSearch migration).
     * This method provides basic validation without full JsonNode processing.
     */
    @SuppressWarnings("unchecked")
    private void deserializeFromMap(Map<String, Object> modelMap) throws ValidationException {
        if (modelMap == null) {
            throw new ValidationException("Entity model cannot be null.");
        }

        // Validate the existence of required fields.
        for (String field : REQUIRED_FIELDS) {
            if (!modelMap.containsKey(field)) {
                throw new ValidationException("Entity model is missing required field '" + field + "'.");
            }
        }

        // Basic validation and parsing of each required field
        for (String fieldName : REQUIRED_FIELDS) {
            Object fieldValue = modelMap.get(fieldName);
            if (!(fieldValue instanceof Map)) {
                throw new ValidationException("'" + fieldName + "' must be an object.");
            }
            
            Map<String, Object> fieldMap = (Map<String, Object>) fieldValue;
            if (this.validateRunnable && fieldMap.isEmpty()) {
                throw new ValidationException("'" + fieldName + "' must not be empty in the entity model.");
            }
            
            // Parse field data with simplified implementations
            switch (fieldName) {
                case "attributes":
                    parseAttributesFromMap(fieldMap);
                    break;
                case "indices":
                    parseIndicesFromMap(fieldMap);
                    break;
                case "matchers":
                    parseMatchersFromMap(fieldMap);
                    break;
                case "resolvers":
                    parseResolversFromMap(fieldMap);
                    break;
                default:
                    throw new ValidationException("'" + fieldName + "' is not a recognized field.");
            }
        }
        
        // Validate attribute nesting
        this.validateAttributeNesting();
    }
    
    /**
     * Parse attributes from Map representation.
     */
    @SuppressWarnings("unchecked")
    private void parseAttributesFromMap(Map<String, Object> attributesMap) throws ValidationException {
        for (Map.Entry<String, Object> entry : attributesMap.entrySet()) {
            String attributeName = entry.getKey();
            Object attributeValue = entry.getValue();
            
            if (!(attributeValue instanceof Map)) {
                throw new ValidationException("'attributes." + attributeName + "' must be an object.");
            }
            
            Map<String, Object> attributeMap = (Map<String, Object>) attributeValue;
            
            // Extract type
            String type = "string"; // Default type
            if (attributeMap.containsKey("type")) {
                Object typeValue = attributeMap.get("type");
                if (typeValue instanceof String) {
                    type = (String) typeValue;
                }
            }
            
            // Extract score if present
            Double score = null;
            if (attributeMap.containsKey("score")) {
                Object scoreValue = attributeMap.get("score");
                if (scoreValue instanceof Number) {
                    score = ((Number) scoreValue).doubleValue();
                }
            }
            
            // Extract params if present
            Map<String, String> params = null;
            if (attributeMap.containsKey("params")) {
                Object paramsValue = attributeMap.get("params");
                if (paramsValue instanceof Map) {
                    params = new TreeMap<>();
                    Map<String, Object> paramsMap = (Map<String, Object>) paramsValue;
                    for (Map.Entry<String, Object> paramEntry : paramsMap.entrySet()) {
                        String paramKey = paramEntry.getKey();
                        Object paramValue = paramEntry.getValue();
                        if (paramValue == null) {
                            params.put(paramKey, "null");
                        } else {
                            params.put(paramKey, paramValue.toString());
                        }
                    }
                }
            }
            
            // Use the new constructor that avoids Jackson entirely
            Attribute attribute = new Attribute(attributeName, type, score, params, this.validateRunnable);
            this.attributes.put(attributeName, attribute);
        }
    }
    
    /**
     * Parse indices from Map representation.
     */
    @SuppressWarnings("unchecked")
    private void parseIndicesFromMap(Map<String, Object> indicesMap) throws ValidationException {
        for (Map.Entry<String, Object> entry : indicesMap.entrySet()) {
            String indexName = entry.getKey();
            Object indexValue = entry.getValue();
            
            if (!(indexValue instanceof Map)) {
                throw new ValidationException("'indices." + indexName + "' must be an object.");
            }
            
            Map<String, Object> indexMap = (Map<String, Object>) indexValue;
            Index index = new Index(indexName, indexMap);
            this.indices.put(indexName, index);
        }
    }
    
    /**
     * Parse matchers from Map representation.
     */
    @SuppressWarnings("unchecked")
    private void parseMatchersFromMap(Map<String, Object> matchersMap) throws ValidationException {
        for (Map.Entry<String, Object> entry : matchersMap.entrySet()) {
            String matcherName = entry.getKey();
            Object matcherValue = entry.getValue();
            
            if (!(matcherValue instanceof Map)) {
                throw new ValidationException("'matchers." + matcherName + "' must be an object.");
            }
            
            Map<String, Object> matcherMap = (Map<String, Object>) matcherValue;
            Matcher matcher = new Matcher(matcherName, matcherMap);
            this.matchers.put(matcherName, matcher);
        }
    }
    
    /**
     * Parse resolvers from Map representation.
     */
    @SuppressWarnings("unchecked")
    private void parseResolversFromMap(Map<String, Object> resolversMap) throws ValidationException {
        for (Map.Entry<String, Object> entry : resolversMap.entrySet()) {
            String resolverName = entry.getKey();
            Object resolverValue = entry.getValue();
            
            if (!(resolverValue instanceof Map)) {
                throw new ValidationException("'resolvers." + resolverName + "' must be an object.");
            }
            
            Map<String, Object> resolverMap = (Map<String, Object>) resolverValue;
            Resolver resolver = new Resolver(resolverName, resolverMap);
            this.resolvers.put(resolverName, resolver);
        }
    }

}
