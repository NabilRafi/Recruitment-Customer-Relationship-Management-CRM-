package com.recruitcrm.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class JsonWriter {

    public static Obj obj() {
        return new Obj();
    }

    public static String array(List<String> jsonObjects) {
        return "[" + String.join(",", jsonObjects) + "]";
    }

    public static String quote(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                default: sb.append(c);
            }
        }
        return sb.append("\"").toString();
    }

    
    public static class Obj {
        private final Map<String, String> fields = new LinkedHashMap<>();

        public Obj put(String key, String value) {
            fields.put(key, quote(value));
            return this;
        }

        public Obj put(String key, int value) {
            fields.put(key, Integer.toString(value));
            return this;
        }

        public Obj put(String key, boolean value) {
            fields.put(key, Boolean.toString(value));
            return this;
        }

        public Obj putRaw(String key, String rawJson) {
            fields.put(key, rawJson);
            return this;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, String> e : fields.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append(quote(e.getKey())).append(":").append(e.getValue());
            }
            return sb.append("}").toString();
        }
    }
}
