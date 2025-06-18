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

import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentType;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class to replace missing or changed OpenSearch Strings functionality.
 * This provides compatibility for OpenSearch 2.17 where some Strings methods
 * have been removed or changed signature.
 */
public class StringsUtil {
    
    /**
     * Invalid filename characters that should not be used in filenames.
     * Based on common filesystem restrictions.
     */
    public static final String INVALID_FILENAME_CHARS = "\\/:*?\"<>|";
    
    /**
     * Validates if a string is a valid filename.
     * 
     * @param filename the filename to validate
     * @return true if the filename is valid, false otherwise
     */
    public static boolean validFileName(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        
        // Check for invalid characters
        for (char c : INVALID_FILENAME_CHARS.toCharArray()) {
            if (filename.indexOf(c) >= 0) {
                return false;
            }
        }
        
        // Check for reserved names on Windows
        String[] reservedNames = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", 
                                 "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", 
                                 "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};
        String upperName = filename.toUpperCase();
        for (String reserved : reservedNames) {
            if (upperName.equals(reserved) || upperName.startsWith(reserved + ".")) {
                return false;
            }
        }
        
        // Check for names that start or end with dot or space
        if (filename.startsWith(".") || filename.endsWith(".") || 
            filename.startsWith(" ") || filename.endsWith(" ")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Converts a ToXContent object to its string representation using JSON format.
     * This replaces the OpenSearch 2.17 Strings.toString() method that now requires MediaType.
     * 
     * @param toXContent the object to convert to string
     * @return the string representation
     * @throws IOException if there's an error during conversion
     */
    public static String toString(ToXContent toXContent) throws IOException {
        try (XContentBuilder builder = XContentType.JSON.contentBuilder()) {
            toXContent.toXContent(builder, ToXContent.EMPTY_PARAMS);
            return builder.toString();
        }
    }
    
    /**
     * Joins an array of strings with a delimiter.
     * 
     * @param array the array of strings to join
     * @param delimiter the delimiter to use
     * @return the joined string
     */
    public static String join(String[] array, String delimiter) {
        if (array == null || array.length == 0) {
            return "";
        }
        
        if (array.length == 1) {
            return array[0] == null ? "" : array[0];
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            if (array[i] != null) {
                sb.append(array[i]);
            }
        }
        return sb.toString();
    }
    
    /**
     * Joins an array of strings without a delimiter.
     * 
     * @param array the array of strings to join
     * @return the joined string
     */
    public static String join(String[] array) {
        return join(array, "");
    }
    
    /**
     * Joins a list of strings with a delimiter.
     * 
     * @param list the list of strings to join
     * @param delimiter the delimiter to use
     * @return the joined string
     */
    public static String join(List<String> list, String delimiter) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        
        if (list.size() == 1) {
            String item = list.get(0);
            return item == null ? "" : item;
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            String item = list.get(i);
            if (item != null) {
                sb.append(item);
            }
        }
        return sb.toString();
    }
    
    /**
     * Joins a list of strings without a delimiter.
     * 
     * @param list the list of strings to join
     * @return the joined string
     */
    public static String join(List<String> list) {
        return join(list, "");
    }
} 