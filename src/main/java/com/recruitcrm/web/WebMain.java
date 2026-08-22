package com.recruitcrm.web;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;


public class WebMain {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/auth", new AuthHandler());
        server.createContext("/api/jobs", new JobsHandler());
        server.createContext("/api/applications", new ApplicationsHandler());
        server.createContext("/api/accounts", new AccountsHandler());
        server.createContext("/", new StaticFileHandler("public"));
        server.setExecutor(null);
        server.start();

        System.out.println("Recruitment CRM running at http://localhost:" + port);
    }
}
