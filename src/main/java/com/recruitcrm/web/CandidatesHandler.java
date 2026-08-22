package com.recruitcrm.web;

import com.recruitcrm.domain.Candidate;
import com.recruitcrm.domain.UserAccount;
import com.recruitcrm.patterns.proxy.CandidateProfile;
import com.recruitcrm.patterns.proxy.ProtectedCandidateProfile;
import com.recruitcrm.patterns.singleton.DataStore;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Serves a candidate's profile — always through the PROXY.
 *
 * Route: GET /api/candidates/{email}
 *
 * Note this handler never reads the Candidate's fields directly. It
 * builds a ProtectedCandidateProfile and asks that. Whether the caller
 * gets the real email or a masked placeholder is decided entirely inside
 * the proxy, so the access rules live in one place rather than being
 * repeated in every handler that touches candidate data.
 */
public class CandidatesHandler implements HttpHandler {

    private final DataStore store = DataStore.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equals("GET")) {
                RequestUtil.sendError(exchange, 405, "Use GET");
                return;
            }

            String[] segments = exchange.getRequestURI().getPath().split("/");
            // "", "api", "candidates", "{email}"
            if (segments.length != 4) {
                RequestUtil.sendError(exchange, 404, "Candidate not found");
                return;
            }

            String email = URLDecoder.decode(segments[3], StandardCharsets.UTF_8);

            UserAccount viewer = AuthUtil.requireUser(exchange);
            UserAccount account = store.getAccount(email);

            if (!(account instanceof Candidate candidate)) {
                RequestUtil.sendError(exchange, 404, "No candidate with that email");
                return;
            }

            // The proxy decides what this particular viewer may read.
            CandidateProfile profile = new ProtectedCandidateProfile(candidate, viewer);

            RequestUtil.sendJson(exchange, 200, JsonWriter.obj()
                    .put("name", profile.getName())
                    .put("email", profile.getEmail())
                    .put("resumeLink", profile.getResumeLink())
                    .put("accessNote", profile.getAccessNote())
                    .toString());

        } catch (AuthFailure e) {
            // AuthUtil already sent the 401/403.
        } catch (Exception e) {
            RequestUtil.sendError(exchange, 500, "Server error: " + e.getMessage());
        }
    }
}
