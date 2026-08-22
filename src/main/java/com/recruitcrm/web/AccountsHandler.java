package com.recruitcrm.web;

import com.recruitcrm.domain.UserAccount;
import com.recruitcrm.patterns.factory.UserAccountFactory;
import com.recruitcrm.patterns.factory.UserAccountFactoryRegistry;
import com.recruitcrm.patterns.singleton.DataStore;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;


public class AccountsHandler implements HttpHandler {
    private final DataStore store = DataStore.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            RequestUtil.sendError(exchange, 405, "Use POST");
            return;
        }
        try {
            Map<String, String> form = RequestUtil.parseFormBody(exchange);
            String type = require(form, "type");
            String name = require(form, "name");
            String email = require(form, "email");
            String extra = form.getOrDefault("extra", "");

            UserAccountFactory factory = UserAccountFactoryRegistry.getInstance().getFactory(type);
            UserAccount account = factory.createAccount(name, email, extra);
            store.saveAccount(account.getEmail(), account);

            RequestUtil.sendJson(exchange, 201, JsonWriter.obj()
                    .put("name", account.getName())
                    .put("email", account.getEmail())
                    .put("role", account.getRole())
                    .toString());
        } catch (IllegalArgumentException e) {
            RequestUtil.sendError(exchange, 400, e.getMessage());
        }
    }

    private String require(Map<String, String> form, String key) {
        String value = form.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return value;
    }
}
