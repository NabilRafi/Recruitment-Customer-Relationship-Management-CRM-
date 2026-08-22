package com.recruitcrm.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


public class RequestUtil {

    /** Reads a form-encoded body ("name=Ayesha&email=a%40b.com") into a Map. */
    public static Map<String, String> parseFormBody(HttpExchange exchange) throws IOException {
        String body = readBody(exchange.getRequestBody());
        return parseFormString(body);
    }

    public static Map<String, String> parseQuery(HttpExchange exchange) {
        String query = exchange.getRequestURI().getRawQuery();
        return parseFormString(query);
    }

    private static Map<String, String> parseFormString(String raw) {
        Map<String, String> result = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            result.put(key, value);
        }
        return result;
    }

    private static String readBody(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int read;
        while ((read = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    public static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        sendJsonWithCookie(exchange, status, json, null);
    }

    public static void sendJsonWithCookie(HttpExchange exchange, int status, String json, String setCookie) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        if (setCookie != null) {
            exchange.getResponseHeaders().add("Set-Cookie", setCookie);
        }
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, JsonWriter.obj().put("error", message).toString());
    }

    public static String getCookie(HttpExchange exchange, String name) {
        var cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies == null) {
            return null;
        }
        for (String header : cookies) {
            for (String part : header.split(";")) {
                String trimmed = part.trim();
                int eq = trimmed.indexOf('=');
                if (eq > 0 && trimmed.substring(0, eq).equals(name)) {
                    return trimmed.substring(eq + 1);
                }
            }
        }
        return null;
    }
}
