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

import io.zentity.common.Patterns;
import io.zentity.model.ValidationException;
import io.zentity.resolution.input.value.Value;
import io.zentity.resolution.input.value.BooleanValue;
import io.zentity.resolution.input.value.DateValue;
import io.zentity.resolution.input.value.NumberValue;
import io.zentity.resolution.input.value.StringValue;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Term implements Comparable<Term> {

    private final String term;

    public Term(String term) throws ValidationException {
        validateTerm(term);
        this.term = term;
    }

    private void validateTerm(String term) throws ValidationException {
        if (Patterns.EMPTY_STRING.matcher(term).matches())
            throw new ValidationException("A term must be a non-empty string.");
    }

    public String term() { 
        return this.term; 
    }

    public static boolean isBoolean(String term) {
        String termLowerCase = term.toLowerCase();
        return termLowerCase.equals("true") || termLowerCase.equals("false");
    }

    public static boolean isDate(String term, String format) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(format);
            formatter.setLenient(false);
            formatter.parse(term);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    public static boolean isNumber(String term) {
        return Patterns.NUMBER_STRING.matcher(term).matches();
    }

    /**
     * Check if the term string is a boolean value.
     */
    public boolean isBoolean() {
        return isBoolean(this.term);
    }

    /**
     * Check if the term string is a date value.
     */
    public boolean isDate(String format) {
        return isDate(this.term, format);
    }

    /**
     * Check if the term string is a number value.
     */
    public boolean isNumber() {
        return isNumber(this.term);
    }

    /**
     * Convert term to a BooleanValue.
     */
    public Value booleanValue() throws ValidationException, IOException {
        Boolean value = Boolean.parseBoolean(this.term.toLowerCase());
        return new BooleanValue(value);
    }

    /**
     * Convert term to a DateValue.
     */
    public Value dateValue() throws ValidationException, IOException {
        return new DateValue(this.term);
    }

    /**
     * Convert term to a NumberValue.
     */
    public Value numberValue() throws ValidationException, IOException {
        try {
            if (this.term.contains(".")) {
                Double value = Double.parseDouble(this.term);
                return new NumberValue(value);
            } else {
                Long value = Long.parseLong(this.term);
                return new NumberValue(value);
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid number format: " + this.term);
        }
    }

    /**
     * Convert term to a StringValue.
     */
    public Value stringValue() throws ValidationException, IOException {
        return new StringValue(this.term);
    }

    @Override
    public int compareTo(Term o) {
        return this.term.compareTo(o.term);
    }

    @Override
    public String toString() {
        return this.term;
    }

    @Override
    public boolean equals(Object o) { 
        return this.hashCode() == o.hashCode(); 
    }

    @Override
    public int hashCode() { 
        return this.term.hashCode(); 
    }
}