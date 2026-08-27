package com.recruitcrm.web;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

/**
 * Entry point for running the project as a web app instead of the
 * console demo in Main.java. Serves the frontend from ./public and the
 * REST API under /api/*, all from one process — no separate frontend
 * server, no CORS setup needed.
 *
 * Reads PORT from the environment (falls back to 8080) because that's
 * the convention most hosting platforms use to tell an app which port
 * to listen on.
 */
public class WebMain {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/auth", new AuthHandler());
        server.createContext("/api/jobs", new JobsHandler());
        server.createContext("/api/applications", new ApplicationsHandler());
        server.createContext("/api/accounts", new AccountsHandler());
        server.createContext("/api/candidates", new CandidatesHandler());
        server.createContext("/api/resumes", new ResumeHandler());
        server.createContext("/", new StaticFileHandler("public"));
        server.setExecutor(null);
        server.start();

        System.out.println("Recruitment CRM running at http://localhost:" + port);
    }
}
