package com.recruitcrm.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;


public class StaticFileHandler implements HttpHandler {
    private final Path root;

    public StaticFileHandler(String rootDir) {
        this.root = new File(rootDir).toPath().normalize().toAbsolutePath();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
        Path filePath = root.resolve(path.substring(1)).normalize();

        if (!filePath.startsWith(root) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
            filePath = root.resolve("index.html"); // simple single-page-app fallback
        }

        byte[] bytes = Files.readAllBytes(filePath);
        exchange.getResponseHeaders().set("Content-Type", contentType(filePath.toString()));
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String contentType(String filename) {
        if (filename.endsWith(".html")) return "text/html; charset=utf-8";
        if (filename.endsWith(".css")) return "text/css; charset=utf-8";
        if (filename.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (filename.endsWith(".svg")) return "image/svg+xml";
        if (filename.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }
}
