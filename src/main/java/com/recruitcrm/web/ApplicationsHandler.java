package com.recruitcrm.web;

import com.recruitcrm.domain.Application;
import com.recruitcrm.domain.ApplicationStatus;
import com.recruitcrm.domain.Candidate;
import com.recruitcrm.domain.Job;
import com.recruitcrm.domain.UserAccount;
import com.recruitcrm.patterns.facade.RecruitmentFacade;
import com.recruitcrm.patterns.factory.UserAccountFactory;
import com.recruitcrm.patterns.factory.UserAccountFactoryRegistry;
import com.recruitcrm.patterns.observer.AuditLogObserver;
import com.recruitcrm.notification.EmailConfig;
import com.recruitcrm.patterns.observer.RealEmailObserver;
import com.recruitcrm.patterns.singleton.DataStore;
import com.recruitcrm.patterns.strategy.BehavioralEvaluationStrategy;
import com.recruitcrm.patterns.strategy.EvaluationStrategy;
import com.recruitcrm.patterns.strategy.HrEvaluationStrategy;
import com.recruitcrm.patterns.strategy.TechnicalEvaluationStrategy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class ApplicationsHandler implements HttpHandler {
    private static final Map<String, EvaluationStrategy> STRATEGIES = Map.of(
            "TECHNICAL", new TechnicalEvaluationStrategy(),
            "HR", new HrEvaluationStrategy(),
            "BEHAVIORAL", new BehavioralEvaluationStrategy()
    );

    private final DataStore store = DataStore.getInstance();
    private final RecruitmentFacade facade = new RecruitmentFacade();

    public ApplicationsHandler() {
        facade.addObserver(new RealEmailObserver(EmailConfig.buildSender()));
        facade.addObserver(new AuditLogObserver());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String[] segments = exchange.getRequestURI().getPath().split("/");
        // "", "api", "applications" [, "{id}", "status"]

        try {
            if (method.equals("GET") && segments.length == 3) {
                listApplications(exchange);
            } else if (method.equals("POST") && segments.length == 3) {
                AuthUtil.requireUser(exchange);
                submitApplication(exchange);
            } else if (method.equals("POST") && segments.length == 5 && segments[4].equals("status")) {
                AuthUtil.requireRole(exchange, "RECRUITER");
                updateStatus(exchange, segments[3]);
            } else {
                RequestUtil.sendError(exchange, 404, "Unknown route");
            }
        } catch (IllegalArgumentException e) {
            RequestUtil.sendError(exchange, 400, e.getMessage());
        } catch (AuthFailure e) {
            // AuthUtil already sent the 401/403 - nothing further to send
        } catch (Exception e) {
            RequestUtil.sendError(exchange, 500, "Server error: " + e.getMessage());
        }
    }

    private void listApplications(HttpExchange exchange) throws IOException {
        UserAccount user = AuthUtil.requireUser(exchange);
        boolean isRecruiter = user.getRole().equals("RECRUITER");

        List<String> jsons = new ArrayList<>();
        for (Application app : store.getAllApplications()) {
            boolean ownedByThisUser = app.getCandidate().getEmail().equalsIgnoreCase(user.getEmail());
            if (isRecruiter || ownedByThisUser) {
                jsons.add(toJson(app));
            }
        }
        RequestUtil.sendJson(exchange, 200, JsonWriter.array(jsons));
    }

    private void submitApplication(HttpExchange exchange) throws IOException {
        Map<String, String> form = RequestUtil.parseFormBody(exchange);
        String name = require(form, "candidateName");
        String email = require(form, "candidateEmail").toLowerCase();
        String resumeLink = form.getOrDefault("resumeLink", "");
        String jobId = require(form, "jobId");

        Job job = store.getJob(jobId);
        if (job == null) {
            throw new IllegalArgumentException("No job with id " + jobId);
        }

        UserAccount currentUser = AuthUtil.requireRole(exchange, "CANDIDATE");

        if (!currentUser.getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("Use your logged-in email when applying");
        }
        Candidate candidate = (Candidate) currentUser;

        Application application = facade.submitApplication(candidate, job);
        RequestUtil.sendJson(exchange, 201, toJson(application));
    }

    private void updateStatus(HttpExchange exchange, String applicationId) throws IOException {
        Map<String, String> form = RequestUtil.parseFormBody(exchange);
        String statusRaw = require(form, "status");
        String strategyRaw = form.getOrDefault("strategy", "");

        Application application = store.getApplication(applicationId);
        if (application == null) {
            throw new IllegalArgumentException("No application with id " + applicationId);
        }

        ApplicationStatus newStatus;
        try {
            newStatus = ApplicationStatus.valueOf(statusRaw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown status: " + statusRaw);
        }

            
        String entitlements = form.getOrDefault("entitlements", "");
        if (!entitlements.isBlank()) {
            application.setOfferEntitlements(entitlements);
        }
        String interviewDetails = form.getOrDefault("interviewDetails", "");
        if (!interviewDetails.isBlank()) {
            application.setInterviewDetails(interviewDetails);
        }
        EvaluationStrategy strategy = STRATEGIES.get(strategyRaw); // null is fine - means "no evaluation this time"
        facade.updateStatus(application, newStatus, strategy);
        RequestUtil.sendJson(exchange, 200, toJson(application));
    }

    private String toJson(Application app) {
        return JsonWriter.obj()
                .put("id", app.getId())
                .put("candidateName", app.getCandidate().getName())
                .put("candidateEmail", app.getCandidate().getEmail())
                .put("jobId", app.getJob().getId())
                .put("jobTitle", app.getJob().getTitle())
                .put("status", app.getStatus().name())
                .put("evaluationSummary", app.getLastEvaluationSummary() == null ? "" : app.getLastEvaluationSummary())
                .toString();
    }

    private String require(Map<String, String> form, String key) {
        String value = form.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return value;
    }
}
