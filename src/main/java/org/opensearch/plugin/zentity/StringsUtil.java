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
package org.opensearch.plugin.zentity;

import org.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.List;

/**
 * String utility methods for OpenSearch migration.
 */
public class StringsUtil {

    public static final String INVALID_FILENAME_CHARS = "\\/:*?\"<>|";

    /**
     * Join a collection of strings with a delimiter.
     */
    public static String join(List<String> strings, String delimiter) {
        return String.join(delimiter, strings);
    }

    /**
     * Convert XContentBuilder to string.
     */
    public static String toString(XContentBuilder builder) {
        try {
            return builder.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Check if a filename is valid (doesn't contain invalid characters).
     */
    public static boolean validFileName(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        
        for (char c : INVALID_FILENAME_CHARS.toCharArray()) {
            if (filename.indexOf(c) >= 0) {
                return false;
            }
        }
        
        return true;
    }
}