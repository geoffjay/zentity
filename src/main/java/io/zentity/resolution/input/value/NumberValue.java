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
package io.zentity.resolution.input.value;

import io.zentity.model.ValidationException;

public class NumberValue extends Value {

    public final String type = "number";

    public NumberValue(Object value) throws ValidationException {
        super(value);
    }

    /**
     * Serialize the attribute value from an Object to a String object.
     *
     * @return
     */
    @Override
    public String serialize(Object value) {
        if (value == null)
            return "null";
        return value.toString();
    }

    /**
     * Validate the value.
     *
     * @param value Attribute value.
     * @throws ValidationException
     */
    @Override
    public void validate(Object value) throws ValidationException {
        if (value != null && !(value instanceof Number))
            throw new ValidationException("Expected '" + this.type + "' attribute data type.");
    }
}
