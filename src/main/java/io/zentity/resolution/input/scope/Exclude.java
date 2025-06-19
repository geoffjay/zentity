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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Exclude extends ScopeField {

    public Exclude() {
        super();
    }



    @Override
    @SuppressWarnings("unchecked")
    public void deserializeFromMap(Map<String, Object> scopeMap, Model model) throws ValidationException, IOException {
        if (scopeMap == null) {
            return;
        }

        // Parse and validate the "scope.exclude" fields
        for (Map.Entry<String, Object> entry : scopeMap.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            
            switch (name) {
                case "attributes":
                    if (value instanceof Map) {
                        Map<String, Object> attributesMap = (Map<String, Object>) value;
                        this.attributes = parseAttributesFromMap("exclude", model, attributesMap);
                    } else if (value != null) {
                        throw new ValidationException("'scope.exclude.attributes' must be an object.");
                    }
                    break;
                case "resolvers":
                    this.resolvers = parseResolversFromValue("exclude", value);
                    break;
                case "indices":
                    this.indices = parseIndicesFromValue("exclude", value);
                    break;
                default:
                    throw new ValidationException("'scope.exclude." + name + "' is not a recognized field.");
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
            
            // For scope attributes, we need to create a simplified Attribute
            // This is a basic implementation - full parsing would require more work
            String attributeType = model.attributes().get(attributeName).type();
            try {
                Attribute attribute = new Attribute(attributeName, attributeType);
                // TODO: Parse values and params from attributeValue if needed
                attributesObj.put(attributeName, attribute);
            } catch (ValidationException e) {
                throw new ValidationException("Error parsing scope attribute '" + attributeName + "': " + e.getMessage());
            }
        }
        
        return attributesObj;
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
