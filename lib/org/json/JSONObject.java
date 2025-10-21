package org.json;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JSONObject extends HashMap<String, Object> {
	public JSONObject() {
		super();
	}

	public JSONObject(Map<String, Object> map) {
		super(map);
	}

	public static JSONObject parseObject(String json) {
		Object value = JsonParser.parse(json);
		if (value instanceof JSONObject obj) {
			return obj;
		}
		throw new IllegalArgumentException("JSON value is not an object");
	}

	public JSONObject putValue(String key, Object value) {
		super.put(key, value);
		return this;
	}

	public String optString(String key) {
		return optString(key, "");
	}

	public String optString(String key, String def) {
		Object value = get(key);
		return value == null ? def : value.toString();
	}

	public boolean optBoolean(String key, boolean def) {
		Object value = get(key);
		if (value instanceof Boolean b) {
			return b;
		}
		return value == null ? def : Boolean.parseBoolean(value.toString());
	}

	public int optInt(String key, int def) {
		Object value = get(key);
		if (value instanceof Number n) {
			return n.intValue();
		}
		return value == null ? def : Integer.parseInt(value.toString());
	}

	public JSONObject getJSONObject(String key) {
		Object value = get(key);
		if (value instanceof JSONObject obj) {
			return obj;
		}
		throw new IllegalArgumentException("Value for key '" + key + "' is not a JSONObject");
	}

	public JSONArray getJSONArray(String key) {
		Object value = get(key);
		if (value instanceof JSONArray arr) {
			return arr;
		}
		throw new IllegalArgumentException("Value for key '" + key + "' is not a JSONArray");
	}

	public JSONObject optJSONObject(String key) {
		Object value = get(key);
		return value instanceof JSONObject obj ? obj : null;
	}

	public JSONArray optJSONArray(String key) {
		Object value = get(key);
		return value instanceof JSONArray arr ? arr : null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		boolean first = true;
		Set<Entry<String, Object>> entries = entrySet();
		for (Entry<String, Object> entry : entries) {
			if (!first) {
				sb.append(',');
			}
			first = false;
			sb.append('"').append(escape(entry.getKey())).append('"').append(':');
			sb.append(valueToString(entry.getValue()));
		}
		sb.append('}');
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
