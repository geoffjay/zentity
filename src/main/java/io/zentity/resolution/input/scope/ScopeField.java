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
import io.zentity.resolution.input.Attribute;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public abstract class ScopeField {

    protected Map<String, Attribute> attributes = new TreeMap<>();
    protected Set<String> indices = new TreeSet<>();
    protected Set<String> resolvers = new TreeSet<>();

    public ScopeField() {
    }

    public Map<String, Attribute> attributes() {
        return this.attributes;
    }

    public void attributes(Map<String, Attribute> attributes) {
        this.attributes = attributes;
    }

    public Set<String> indices() {
        return this.indices;
    }

    public void indices(Set<String> indices) {
        this.indices = indices;
    }

    public Set<String> resolvers() {
        return this.resolvers;
    }

    public void resolvers(Set<String> resolvers) {
        this.resolvers = resolvers;
    }

    /**
     * Abstract method for deserializing from Map representation.
     * This method provides XContent-based parsing without Jackson dependencies.
     */
    public abstract void deserializeFromMap(Map<String, Object> scopeMap, Model model) throws ValidationException, IOException;

    public void deserialize(String json, Model model) throws ValidationException, IOException {
        try {
            Map<String, Object> scopeMap = XContentJson.parseToMap(json);
            deserializeFromMap(scopeMap, model);
        } catch (IOException e) {
            throw new ValidationException("Failed to parse scope JSON: " + e.getMessage());
        }
    }
}