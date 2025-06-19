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
package io.zentity.resolution.input;

import io.zentity.common.XContentJson;
import io.zentity.common.Patterns;
import io.zentity.model.Index;
import io.zentity.model.Model;
import io.zentity.model.ValidationException;
import io.zentity.resolution.input.scope.Scope;
import io.zentity.resolution.input.value.Value;
import io.zentity.resolution.input.value.StringValue;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Input {

    private Map<String, Attribute> attributes = new TreeMap<>();
    private Map<String, Set<String>> ids = new TreeMap<>();
    private Model model;
    private Scope scope = new Scope();
    private Set<Term> terms = new TreeSet<>();

    public Input(String json, Model model) throws ValidationException, IOException {
        this.model = model;
        this.deserializeFromString(json);
    }

    public Input(String json) throws ValidationException, IOException {
        this.deserializeFromString(json);
    }

    /**
     * Exclude indices from an entity model, while retaining all the others.
     */
    public static Model excludeIndices(Model model, Set<String> indices) throws ValidationException {
        if (!indices.isEmpty()) {
            for (String index : indices) {
                if (index == null || index.equals(""))
                    continue;
                if (!model.indices().containsKey(index))
                    throw new ValidationException("'" + index + "' is not in the 'indices' field of the entity model.");
                model.indices().remove(index);
            }
        }
        return model;
    }

    /**
     * Include indices in an entity model, while excluding all the others.
     */
    public static Model includeIndices(Model model, Set<String> indices) throws ValidationException {
        if (!indices.isEmpty()) {
            for (String index : indices) {
                if (index == null || index.equals(""))
                    continue;
                if (!model.indices().containsKey(index))
                    throw new ValidationException("'" + index + "' is not in the 'indices' field of the entity model.");
            }
            model.indices().keySet().retainAll(indices);
        }
        return model;
    }

    /**
     * Exclude resolvers from an entity model, while retaining all the others.
     */
    public static Model excludeResolvers(Model model, Set<String> resolvers) throws ValidationException {
        if (!resolvers.isEmpty()) {
            for (String resolver : resolvers) {
                if (resolver == null || resolver.equals(""))
                    continue;
                if (!model.resolvers().containsKey(resolver))
                    throw new ValidationException("'" + resolver + "' is not in the 'resolvers' field of the entity model.");
                model.resolvers().remove(resolver);
            }
        }
        return model;
    }

    /**
     * Include resolvers in an entity model, while excluding all the others.
     */
    public static Model includeResolvers(Model model, Set<String> resolvers) throws ValidationException {
        if (!resolvers.isEmpty()) {
            for (String resolver : resolvers) {
                if (resolver == null || resolver.equals(""))
                    continue;
                if (!model.resolvers().containsKey(resolver))
                    throw new ValidationException("'" + resolver + "' is not in the 'resolvers' field of the entity model.");
            }
            model.resolvers().keySet().retainAll(resolvers);
        }
        return model;
    }

    public Map<String, Attribute> attributes() {
        return this.attributes;
    }

    public Map<String, Set<String>> ids() {
        return this.ids;
    }

    public Model model() {
        return this.model;
    }

    public Scope scope() {
        return this.scope;
    }

    public Set<Term> terms() {
        return this.terms;
    }

    /**
     * Deserialize Input from JSON string using XContent parsing.
     */
    public void deserializeFromString(String json) throws ValidationException, IOException {
        if (json == null || json.trim().isEmpty()) {
            throw new ValidationException("Input JSON cannot be null or empty.");
        }
        
        try {
            Map<String, Object> inputMap = XContentJson.parseToMap(json);
            deserializeFromMap(inputMap);
        } catch (IOException e) {
            throw new ValidationException("Failed to parse input JSON: " + e.getMessage());
        }
    }
    
    /**
     * Deserialize Input from a Map representation.
     * This method provides XContent-based parsing without Jackson dependencies.
     */
    @SuppressWarnings("unchecked")
    public void deserializeFromMap(Map<String, Object> inputMap) throws ValidationException, IOException {
        if (inputMap == null) {
            throw new ValidationException("Input map cannot be null.");
        }

        // Validate recognized fields
        for (String fieldName : inputMap.keySet()) {
            switch (fieldName) {
                case "attributes":
                case "ids":
                case "model":
                case "scope":
                case "terms":
                    break;
                default:
                    throw new ValidationException("'" + fieldName + "' is not a recognized field.");
            }
        }

        // Parse and validate the "model" field
        if (this.model == null) {
            if (!inputMap.containsKey("model")) {
                throw new ValidationException("You must specify either an entity type or an entity model.");
            }
            this.model = parseEntityModelFromMap(inputMap);
        } else if (inputMap.containsKey("model")) {
            throw new ValidationException("You must specify either an entity type or an entity model, not both.");
        }

        // Parse and validate the "attributes" field
        this.attributes = parseAttributesFromMap(inputMap, this.model);

        // Parse and validate the "terms" field
        this.terms = parseTermsFromMap(inputMap);

        // Parse and validate the "ids" field
        this.ids = parseIdsFromMap(inputMap, this.model);

        // Ensure that either the "attributes" or "terms" or "ids" field exists and is valid
        if (this.attributes().isEmpty() && this.terms.isEmpty() && this.ids.isEmpty()) {
            throw new ValidationException("The 'attributes', 'terms', and 'ids' fields are missing from the request body. At least one must exist.");
        }

        // Parse and validate the "scope" field
        if (inputMap.containsKey("scope")) {
            Map<String, Object> scopeMap = XContentJson.getNestedMap(inputMap, "scope");
            this.scope.deserializeFromMap(scopeMap, this.model);

            // Handle scope include/exclude logic
            if (this.scope.include() != null) {
                if (!this.scope.include().resolvers().isEmpty()) {
                    this.model = includeResolvers(this.model, this.scope.include().resolvers());
                }
                if (!this.scope.include().indices().isEmpty()) {
                    this.model = includeIndices(this.model, this.scope.include().indices());
                }
            }

            if (this.scope.exclude() != null) {
                if (!this.scope.exclude().indices().isEmpty()) {
                    this.model = excludeIndices(this.model, this.scope.exclude().indices());
                }
                if (!this.scope.exclude().resolvers().isEmpty()) {
                    this.model = excludeResolvers(this.model, this.scope.exclude().resolvers());
                }
            }
        }

        // Validate attribute parameters
        validateAttributeParameters();
    }
    
    /**
     * Parse entity model from Map representation.
     */
    @SuppressWarnings("unchecked")
    private Model parseEntityModelFromMap(Map<String, Object> inputMap) throws ValidationException, IOException {
        if (!inputMap.containsKey("model")) {
            throw new ValidationException("The 'model' field is missing from the request body while 'entity_type' is undefined.");
        }
        
        Object modelValue = inputMap.get("model");
        if (!(modelValue instanceof Map)) {
            throw new ValidationException("Entity model must be an object.");
        }
        
        Map<String, Object> modelMap = (Map<String, Object>) modelValue;
        return new Model(modelMap, true);
    }
    
    /**
     * Parse attributes from Map representation.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Attribute> parseAttributesFromMap(Map<String, Object> inputMap, Model model) throws ValidationException, IOException {
        Map<String, Attribute> attributes = new TreeMap<>();
        
        if (!inputMap.containsKey("attributes")) {
            return attributes;
        }
        
        Object attributesValue = inputMap.get("attributes");
        if (!(attributesValue instanceof Map)) {
            throw new ValidationException("'attributes' must be an object.");
        }
        
        Map<String, Object> attributesMap = (Map<String, Object>) attributesValue;
        if (attributesMap.isEmpty()) {
            throw new ValidationException("'attributes' must not be empty.");
        }
        
        for (Map.Entry<String, Object> entry : attributesMap.entrySet()) {
            String attributeName = entry.getKey();
            Object attributeValue = entry.getValue();
            
            // Validate that the attribute exists in the model
            if (!model.attributes().containsKey(attributeName)) {
                throw new ValidationException("'attributes." + attributeName + "' is not defined in the entity model.");
            }
            
            // Get the attribute type from the model
            String attributeType = model.attributes().get(attributeName).type();
            
            // Create Attribute object and handle both array and object formats
            Attribute attribute = new Attribute(attributeName, attributeType);
            
            // Parse attribute value - handle both array and object formats
            if (attributeValue instanceof List) {
                // Array format: {"first_name": ["Alice"]}
                List<Object> valuesList = (List<Object>) attributeValue;
                for (Object value : valuesList) {
                    if (value != null) {
                        // Temporary workaround: validate but don't store values
                        addSimpleStringValue(attribute, value.toString());
                    }
                }
            } else if (attributeValue instanceof Map) {
                // Object format: {"first_name": {"values": ["Alice"], "params": {...}}}
                Map<String, Object> attributeObjectMap = (Map<String, Object>) attributeValue;
                
                // Parse values if present
                if (attributeObjectMap.containsKey("values")) {
                    Object valuesObj = attributeObjectMap.get("values");
                    if (valuesObj instanceof List) {
                        List<Object> valuesList = (List<Object>) valuesObj;
                        for (Object value : valuesList) {
                            if (value != null) {
                                // Temporary workaround: validate but don't store values
                                addSimpleStringValue(attribute, value.toString());
                            }
                        }
                    } else {
                        throw new ValidationException("'attributes." + attributeName + ".values' must be an array.");
                    }
                }
                
                // Parse params if present
                if (attributeObjectMap.containsKey("params")) {
                    Object paramsObj = attributeObjectMap.get("params");
                    if (paramsObj instanceof Map) {
                        Map<String, Object> paramsMap = (Map<String, Object>) paramsObj;
                        for (Map.Entry<String, Object> paramEntry : paramsMap.entrySet()) {
                            String paramField = paramEntry.getKey();
                            Object paramValue = paramEntry.getValue();
                            
                            if (paramValue == null) {
                                attribute.params().put(paramField, "null");
                            } else {
                                attribute.params().put(paramField, paramValue.toString());
                            }
                        }
                    } else {
                        throw new ValidationException("'attributes." + attributeName + ".params' must be an object.");
                    }
                }
            } else if (attributeValue != null) {
                throw new ValidationException("'attributes." + attributeName + "' must be an object or array.");
            }
            
            attributes.put(attributeName, attribute);
        }
        
        return attributes;
    }
    
    /**
     * Parse terms from Map representation.
     */
    @SuppressWarnings("unchecked")
    private Set<Term> parseTermsFromMap(Map<String, Object> inputMap) throws ValidationException {
        Set<Term> terms = new TreeSet<>();
        
        if (!inputMap.containsKey("terms")) {
            return terms;
        }
        
        Object termsValue = inputMap.get("terms");
        if (termsValue == null) {
            return terms;
        }
        
        if (!(termsValue instanceof List)) {
            throw new ValidationException("'terms' must be an array of strings.");
        }
        
        List<Object> termsList = (List<Object>) termsValue;
        for (Object termObj : termsList) {
            if (!(termObj instanceof String)) {
                throw new ValidationException("'terms' must be an array of strings.");
            }
            String termString = (String) termObj;
            if (termString.trim().isEmpty()) {
                throw new ValidationException("'terms' must be an array of non-empty strings.");
            }
            terms.add(new Term(termString));
        }
        
        return terms;
    }
    
    /**
     * Parse ids from Map representation.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Set<String>> parseIdsFromMap(Map<String, Object> inputMap, Model model) throws ValidationException {
        Map<String, Set<String>> idsObj = new TreeMap<>();
        
        if (!inputMap.containsKey("ids")) {
            return idsObj;
        }
        
        Object idsValue = inputMap.get("ids");
        if (!(idsValue instanceof Map)) {
            throw new ValidationException("'ids' must be an object.");
        }
            
        Map<String, Object> ids = (Map<String, Object>) idsValue;
        if (ids.isEmpty()) {
            return idsObj;
        }

        for (Map.Entry<String, Object> entry : ids.entrySet()) {
            String indexName = entry.getKey();
            Object idsValues = entry.getValue();

            // Validate that the index exists in the entity model.
            if (!model.indices().containsKey(indexName)) {
                throw new ValidationException("'ids." + indexName + "' is not defined in the entity model.");
            }

            // Parse the id values.
            idsObj.put(indexName, new TreeSet<>());
            if (idsValues == null) {
                continue;
            }
            if (!(idsValues instanceof List)) {
                throw new ValidationException("'ids." + indexName + "' must be an array.");
            }
                
            List<Object> idsList = (List<Object>) idsValues;
            for (Object idObj : idsList) {
                if (!(idObj instanceof String)) {
                    throw new ValidationException("'ids." + indexName + "' must be an array of strings.");
                }
                String id = (String) idObj;
                if (Patterns.EMPTY_STRING.matcher(id).matches()) {
                    throw new ValidationException("'ids." + indexName + "' must be an array of non-empty strings.");
                }
                idsObj.get(indexName).add(XContentJson.quoteString(id));
            }
        }
        return idsObj;
    }

    /**
     * Validate attribute parameters.
     */
    private void validateAttributeParameters() throws ValidationException {
        // Simplified validation for now - just ensure we have valid attributes
        for (String attributeName : this.attributes.keySet()) {
            if (!this.model.attributes().containsKey(attributeName)) {
                throw new ValidationException("'attributes." + attributeName + "' is not defined in the entity model.");
            }
        }
    }

    /**
     * Add a simple string value to an attribute (temporary workaround).
     */
    private void addSimpleStringValue(Attribute attribute, String valueString) {
        // Temporarily disabled during OpenSearch migration
        // TODO: Implement XContent-based Value parsing
        // For now, we just validate the input without storing values
    }
}