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

import io.zentity.common.XContentJson;
import io.zentity.model.Model;
import io.zentity.model.ValidationException;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class Scope {

    private Exclude exclude = new Exclude();
    private Include include = new Include();

    public Scope() {
    }

    public Exclude exclude() {
        return this.exclude;
    }

    public Include include() {
        return this.include;
    }

    public void deserialize(String json, Model model) throws ValidationException, IOException {
        try {
            Map<String, Object> scopeMap = XContentJson.parseToMap(json);
            deserializeFromMap(scopeMap, model);
        } catch (IOException e) {
            throw new ValidationException("Failed to parse scope JSON: " + e.getMessage());
        }
    }
    
    /**
     * Deserialize scope from a Map representation.
     * This method provides XContent-based parsing without Jackson dependencies.
     */
    @SuppressWarnings("unchecked")
    public void deserializeFromMap(Map<String, Object> scopeMap, Model model) throws ValidationException, IOException {
        if (scopeMap == null) {
            return; // Empty scope is valid
        }

        // Parse and validate the "scope.exclude" and "scope.include" fields
        for (Map.Entry<String, Object> entry : scopeMap.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            
            switch (name) {
                case "exclude":
                    if (value instanceof Map) {
                        Map<String, Object> excludeMap = (Map<String, Object>) value;
                        this.exclude.deserializeFromMap(excludeMap, model);
                    } else if (value != null) {
                        throw new ValidationException("'scope.exclude' must be an object.");
                    }
                    break;
                case "include":
                    if (value instanceof Map) {
                        Map<String, Object> includeMap = (Map<String, Object>) value;
                        this.include.deserializeFromMap(includeMap, model);
                    } else if (value != null) {
                        throw new ValidationException("'scope.include' must be an object.");
                    }
                    break;
                default:
                    throw new ValidationException("'scope." + name + "' is not a recognized field.");
            }
        }
    }
}

