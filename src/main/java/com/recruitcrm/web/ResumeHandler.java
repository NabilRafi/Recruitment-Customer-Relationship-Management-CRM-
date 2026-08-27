package com.recruitcrm.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Serves uploaded resume PDFs.
 *
 * Route: GET /api/resumes/{filename}
 *
 * Login is required, so a resume cannot be fetched anonymously even if the
 * filename is guessed. Supporting plumbing, not a design pattern.
 */
public class ResumeHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equals("GET")) {
                RequestUtil.sendError(exchange, 405, "Use GET");
                return;
            }

            AuthUtil.requireUser(exchange);

            String[] segments = exchange.getRequestURI().getPath().split("/");
            // "", "api", "resumes", "{filename}"
            if (segments.length != 4) {
                RequestUtil.sendError(exchange, 404, "Resume not found");
                return;
            }

            byte[] pdf = ResumeStorage.read(segments[3]);
            if (pdf == null) {
                RequestUtil.sendError(exchange, 404, "Resume not found");
                return;
            }

            exchange.getResponseHeaders().set("Content-Type", "application/pdf");
            // "inline" opens it in the browser's PDF viewer rather than downloading.
            exchange.getResponseHeaders().set("Content-Disposition", "inline; filename=\"resume.pdf\"");
            exchange.sendResponseHeaders(200, pdf.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(pdf);
            }
        } catch (AuthFailure e) {
            // AuthUtil already sent the 401.
        }
    }
}
