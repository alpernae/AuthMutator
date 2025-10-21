package org.json;

import java.util.ArrayList;
import java.util.Collection;

public class JSONArray extends ArrayList<Object> {
    public JSONArray() {
        super();
    }

    public JSONArray(Collection<?> c) {
        super(c);
    }

    public static JSONArray parseArray(String json) {
        Object value = JsonParser.parse(json);
        if (value instanceof JSONArray arr) {
            return arr;
        }
        throw new IllegalArgumentException("JSON value is not an array");
    }

    public JSONObject getJSONObject(int index) {
        Object value = get(index);
        if (value instanceof JSONObject obj) {
            return obj;
        }
        throw new IllegalArgumentException("Element at index " + index + " is not a JSONObject");
    }

    public JSONArray put(Object value) {
        add(value);
        return this;
    }

    public int length() {
        return size();
    }

    public JSONObject optJSONObject(int index) {
        if (index < 0 || index >= size()) {
            return null;
        }
        Object value = get(index);
        return value instanceof JSONObject obj ? obj : null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        for (Object value : this) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(valueToString(value));
        }
        sb.append(']');
        return sb.toString();
    }

    private String valueToString(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String str) {
            return '"' + escape(str) + '"';
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof JSONObject obj) {
            return obj.toString();
        }
        if (value instanceof JSONArray arr) {
            return arr.toString();
        }
        return '"' + escape(value.toString()) + '"';
    }

    private String escape(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 32 || ch > 126) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.toString();
    }
}
