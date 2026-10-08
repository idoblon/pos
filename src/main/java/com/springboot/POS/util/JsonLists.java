package com.springboot.POS.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Lenient JSON (de)serialization for LONGTEXT attribute columns
 * (product variants / bulk tiers / modifiers, order-item serials).
 * Reads never throw: bad or blank JSON yields an empty collection so a
 * single malformed row cannot break catalog or order endpoints.
 */
public final class JsonLists {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final TypeReference<List<String>> STRING_LIST =
            new TypeReference<List<String>>() {};
    private static final TypeReference<List<Map<String, Object>>> MAP_LIST =
            new TypeReference<List<Map<String, Object>>>() {};

    private JsonLists() {}

    public static String toJson(Object value) {
        if (value == null) return null;
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Value is not JSON serializable", e);
        }
    }

    public static List<String> stringList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<String> list = MAPPER.readValue(json, STRING_LIST);
            return list != null ? list : Collections.emptyList();
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public static List<Map<String, Object>> mapList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<Map<String, Object>> list = MAPPER.readValue(json, MAP_LIST);
            return list != null ? list : Collections.emptyList();
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }
}
