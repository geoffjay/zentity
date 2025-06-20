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
package io.zentity.resolution.input.scope;


import io.zentity.model.Model;
import io.zentity.model.ValidationException;
import io.zentity.resolution.input.Attribute;
import io.zentity.resolution.input.value.Value;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Include extends ScopeField {

    public Include() {
        super();
    }


    
    @Override
    @SuppressWarnings("unchecked")
    public void deserializeFromMap(Map<String, Object> scopeMap, Model model) throws ValidationException, IOException {
        if (scopeMap == null) {
            return;
        }

        // Parse and validate the "scope.include" fields
        for (Map.Entry<String, Object> entry : scopeMap.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            
            switch (name) {
                case "attributes":
                    if (value instanceof Map) {
                        Map<String, Object> attributesMap = (Map<String, Object>) value;
                        this.attributes = parseAttributesFromMap("include", model, attributesMap);
                    } else if (value != null) {
                        throw new ValidationException("'scope.include.attributes' must be an object.");
                    }
                    break;
                case "resolvers":
                    this.resolvers = parseResolversFromValue("include", value);
                    break;
                case "indices":
                    this.indices = parseIndicesFromValue("include", value);
                    break;
                default:
                    throw new ValidationException("'scope.include." + name + "' is not a recognized field.");
            }
        }
    }
    
    /**
     * Parse attributes from Map representation for scope include/exclude.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Attribute> parseAttributesFromMap(String scopeType, Model model, Map<String, Object> attributesMap) throws ValidationException {
        Map<String, Attribute> attributesObj = new TreeMap<>();
        
        if (attributesMap == null || attributesMap.isEmpty()) {
            return attributesObj;
        }
        
        for (Map.Entry<String, Object> entry : attributesMap.entrySet()) {
            String attributeName = entry.getKey();
            Object attributeValue = entry.getValue();
            
            // Validate that the attribute exists in the entity model
            if (!model.attributes().containsKey(attributeName)) {
                throw new ValidationException("'" + attributeName + "' is not defined in the entity model.");
            }
            
            // Get the attribute type from the model
            String attributeType = model.attributes().get(attributeName).type();
            
            // Create Attribute object and handle both array and object formats
            Attribute attribute = new Attribute(attributeName, attributeType);
            
            // Parse attribute value - handle both array and object formats
            if (attributeValue instanceof List) {
                // Array format: {"attribute_name": ["value1", "value2"]}
                List<Object> valuesList = (List<Object>) attributeValue;
                for (Object value : valuesList) {
                    if (value != null) {
                        addSimpleValueWithValidation(attribute, value, attributeType, attributeName, scopeType);
                    }
                }
            } else if (attributeValue instanceof Map) {
                // Object format: {"attribute_name": {"values": ["value1"], "params": {...}}}
                Map<String, Object> attributeObjectMap = (Map<String, Object>) attributeValue;
                
                // Parse values if present
                if (attributeObjectMap.containsKey("values")) {
                    Object valuesObj = attributeObjectMap.get("values");
                    if (valuesObj instanceof List) {
                        List<Object> valuesList = (List<Object>) valuesObj;
                        for (Object value : valuesList) {
                            if (value != null) {
                                addSimpleValueWithValidation(attribute, value, attributeType, attributeName, scopeType);
                            }
                        }
                    } else {
                        throw new ValidationException("'scope." + scopeType + ".attributes." + attributeName + ".values' must be an array.");
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
                        throw new ValidationException("'scope." + scopeType + ".attributes." + attributeName + ".params' must be an object.");
                    }
                }
            } else if (attributeValue != null) {
                throw new ValidationException("'scope." + scopeType + ".attributes." + attributeName + "' must be an object or array.");
            }
            
            attributesObj.put(attributeName, attribute);
        }
        
        return attributesObj;
    }
    
    /**
     * Add a value to an attribute with proper type validation.
     */
    private void addSimpleValueWithValidation(Attribute attribute, Object valueObject, String attributeType, String attributeName, String scopeType) throws ValidationException {
        // Create the appropriate Value object based on the attribute type
        // This will perform type validation and throw ValidationException if types don't match
        Value value = Value.create(attributeType, valueObject);
        attribute.values().add(value);
    }
    
    /**
     * Parse resolvers from various value types.
     */
    @SuppressWarnings("unchecked")
    private Set<String> parseResolversFromValue(String scopeType, Object value) throws ValidationException {
        Set<String> resolvers = new TreeSet<>();
        
        if (value == null) {
            return resolvers;
        }
        
        if (value instanceof String) {
            String resolver = (String) value;
            if (resolver.isEmpty()) {
                throw new ValidationException("'scope." + scopeType + ".resolvers' must not have empty strings.");
            }
            resolvers.add(resolver);
        } else if (value instanceof List) {
            List<Object> resolversList = (List<Object>) value;
            for (Object resolverValue : resolversList) {
                if (!(resolverValue instanceof String)) {
                    throw new ValidationException("'scope." + scopeType + ".resolvers' must be a string or an array of strings.");
                }
                String resolver = (String) resolverValue;
                if (resolver.isEmpty()) {
                    throw new ValidationException("'scope." + scopeType + ".resolvers' must not have empty strings.");
                }
                resolvers.add(resolver);
            }
        } else {
            throw new ValidationException("'scope." + scopeType + ".resolvers' must be a string or an array of strings.");
        }
        
        return resolvers;
    }
    
    /**
     * Parse indices from various value types.
     */
    @SuppressWarnings("unchecked")
    private Set<String> parseIndicesFromValue(String scopeType, Object value) throws ValidationException {
        Set<String> indices = new TreeSet<>();
        
        if (value == null) {
            return indices;
        }
        
        if (value instanceof String) {
            String index = (String) value;
            if (index.isEmpty()) {
                throw new ValidationException("'scope." + scopeType + ".indices' must not have empty strings.");
            }
            indices.add(index);
        } else if (value instanceof List) {
            List<Object> indicesList = (List<Object>) value;
            for (Object indexValue : indicesList) {
                if (!(indexValue instanceof String)) {
                    throw new ValidationException("'scope." + scopeType + ".indices' must be a string or an array of strings.");
                }
                String index = (String) indexValue;
                if (index.isEmpty()) {
                    throw new ValidationException("'scope." + scopeType + ".indices' must not have empty strings.");
                }
                indices.add(index);
            }
        } else {
            throw new ValidationException("'scope." + scopeType + ".indices' must be a string or an array of strings.");
        }
        
        return indices;
    }

}
