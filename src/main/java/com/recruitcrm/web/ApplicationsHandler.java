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
import com.recruitcrm.patterns.strategy.EvaluationStrategyRegistry;
import com.recruitcrm.patterns.strategy.CustomEvaluationStrategy;
import com.recruitcrm.patterns.strategy.HrEvaluationStrategy;
import com.recruitcrm.patterns.strategy.TechnicalEvaluationStrategy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Submits and advances Applications through RecruitmentFacade
 * (patterns.facade), which is what actually coordinates the Singleton
 * data store, the chosen Strategy, and the Observer notifications.
 *
 * Which evaluation Strategy runs is picked from STRATEGIES below — a
 * map lookup, not an if-else chain, for the same reason the Factory
 * registry avoids one: consistency, not just where the rule was stated.
 *
 * Routes:
 *   GET  /api/applications                  -> list all applications
 *   POST /api/applications                  -> submit one
 *   POST /api/applications/{id}/status       -> advance status (+ optional strategy)
 */
public class ApplicationsHandler implements HttpHandler {

    private final DataStore store = DataStore.getInstance();
    private final RecruitmentFacade facade = new RecruitmentFacade();

    public ApplicationsHandler() {
        // Real email (or console fallback if no credentials configured).
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
            } else if (method.equals("GET") && segments.length == 4 && segments[3].equals("metrics")) {
                listMetrics(exchange);
            } else if (method.equals("POST") && segments.length == 4 && segments[3].equals("metrics")) {
                addMetric(exchange);
            } else if (method.equals("DELETE") && segments.length == 4) {
                withdrawApplication(exchange, segments[3]);
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

    /**
     * Lists applications, filtered by who is asking.
     *
     *   - Not logged in : 401. Applications are confidential.
     *   - CANDIDATE     : only their own applications.
     *   - RECRUITER     : all applications (the hiring pipeline).
     *
     * This filtering is done on the SERVER on purpose. Hiding the Pipeline
     * tab in the browser is only cosmetic - anyone could otherwise open
     * /api/applications directly and read every candidate's details.
     */
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

        // A PDF uploaded with the application replaces the stored link.
        String uploadedPdf = form.get("resumeFile");
        if (uploadedPdf != null && !uploadedPdf.isBlank()) {
            resumeLink = ResumeStorage.saveBase64Pdf(uploadedPdf);
        }
        String jobId = require(form, "jobId");

        Job job = store.getJob(jobId);
        if (job == null) {
            throw new IllegalArgumentException("No job with id " + jobId);
        }

        // Only a logged-in CANDIDATE may apply. requireRole sends the 401/403
        // itself and throws AuthFailure, which the handler catches.
        //
        // Previously any logged-in user could apply by supplying a candidate
        // email in the form, which let a recruiter apply on someone else's
        // behalf - and silently created accounts for people who never signed up.
        UserAccount currentUser = AuthUtil.requireRole(exchange, "CANDIDATE");

        if (!currentUser.getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("Use your logged-in email when applying");
        }
        Candidate candidate = (Candidate) currentUser;

        // Persist a newly uploaded resume against the account, and use the
        // updated candidate for this application.
        if (!resumeLink.isBlank() && !resumeLink.equals(candidate.getExtra())) {
            AuthRepository.updateExtra(candidate.getEmail(), resumeLink);
            candidate = new Candidate(candidate.getName(), candidate.getEmail(), resumeLink);
        }

        // FR6: block a second application to the same job.
        if (store.applicationExists(candidate.getEmail(), jobId)) {
            throw new IllegalArgumentException("You have already applied to this role");
        }

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

        // Recruiter-supplied interview details, used by the invitation email.
        // Entitlements the recruiter ticked when making the offer. These
        // become the Decorator chain in the offer letter.
        String entitlements = form.getOrDefault("entitlements", "");
        if (!entitlements.isBlank()) {
            application.setOfferEntitlements(entitlements);
        }

        String interviewDetails = form.getOrDefault("interviewDetails", "");
        if (!interviewDetails.isBlank()) {
            application.setInterviewDetails(interviewDetails);
        }

        // Strategy chosen from the registry - a map lookup, and the registry
        // can gain new metrics at runtime without this code changing.
        EvaluationStrategy strategy = EvaluationStrategyRegistry.getInstance().get(strategyRaw);

        int rawScore = 0;
        try {
            rawScore = Integer.parseInt(form.getOrDefault("score", "0").trim());
        } catch (NumberFormatException e) {
            rawScore = 0;
        }
        rawScore = Math.max(0, Math.min(100, rawScore));   // clamp to 0-100

        if (strategy != null && !form.getOrDefault("score", "").isBlank()) {
            // a score was supplied - use it
        } // null is fine - means "no evaluation this time"
        // Forward-only: an application may not return to an earlier stage.
        // Enforced on the server, because hiding options in the browser
        // dropdown is presentation, not a rule.
        if (!isForwardMove(application.getStatus(), newStatus)) {
            throw new IllegalArgumentException(
                    "Cannot move an application back from " + application.getStatus() + " to " + newStatus);
        }

        facade.updateStatus(application, newStatus, strategy, rawScore);
        RequestUtil.sendJson(exchange, 200, toJson(application));
    }

    /**
     * Stage order for the forward-only rule. REJECTED is deliberately
     * reachable from anywhere - a candidate can be turned down at any
     * point - but nothing can move backwards.
     */
    private static final List<ApplicationStatus> ORDER = List.of(
            ApplicationStatus.APPLIED,
            ApplicationStatus.SHORTLISTED,
            ApplicationStatus.INTERVIEW_SCHEDULED,
            ApplicationStatus.HIRED);

    private boolean isForwardMove(ApplicationStatus from, ApplicationStatus to) {
        if (to == ApplicationStatus.REJECTED) return true;      // always allowed
        if (from == ApplicationStatus.REJECTED) return false;   // terminal
        if (from == ApplicationStatus.HIRED) return false;      // terminal
        return ORDER.indexOf(to) > ORDER.indexOf(from);
    }

    /** Lists every registered evaluation metric for the recruiter's dropdown. */
    private void listMetrics(HttpExchange exchange) throws IOException {
        List<String> out = new ArrayList<>();
        for (String[] entry : EvaluationStrategyRegistry.getInstance().listForDisplay()) {
            out.add(JsonWriter.obj().put("key", entry[0]).put("name", entry[1]).toString());
        }
        RequestUtil.sendJson(exchange, 200, JsonWriter.array(out));
    }

    /**
     * Registers a brand new evaluation metric at runtime.
     *
     * The recruiter supplies a name, a pass mark, and the document to
     * request when a candidate falls below it. A CustomEvaluationStrategy
     * is built and added to the registry - no class is written, nothing
     * is recompiled, and no existing code changes. This is the Strategy
     * pattern's extensibility demonstrated live.
     */
    private void addMetric(HttpExchange exchange) throws IOException {
        AuthUtil.requireRole(exchange, "RECRUITER");
        Map<String, String> form = RequestUtil.parseFormBody(exchange);

        String name = require(form, "name");
        String key = name.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
        int passMark;
        try {
            passMark = Integer.parseInt(form.getOrDefault("passMark", "60").trim());
        } catch (NumberFormatException e) {
            passMark = 60;
        }
        String document = form.getOrDefault("document", "Supporting evidence for this assessment");

        EvaluationStrategyRegistry.getInstance()
                .register(key, new CustomEvaluationStrategy(name, passMark, document));

        RequestUtil.sendJson(exchange, 201, JsonWriter.obj()
                .put("key", key).put("name", name).put("passMark", passMark).toString());
    }

    /**
     * FR8: a candidate withdraws their own application.
     *
     * Two guards, both server-side: only the owning candidate may withdraw,
     * and only while the application is still in the APPLIED stage. Once a
     * recruiter has begun assessing it, withdrawal is no longer permitted.
     */
    private void withdrawApplication(HttpExchange exchange, String applicationId) throws IOException {
        UserAccount user = AuthUtil.requireRole(exchange, "CANDIDATE");

        Application application = store.getApplication(applicationId);
        if (application == null) {
            throw new IllegalArgumentException("No application with id " + applicationId);
        }
        if (!application.getCandidate().getEmail().equalsIgnoreCase(user.getEmail())) {
            RequestUtil.sendError(exchange, 403, "You can only withdraw your own application");
            return;
        }
        if (application.getStatus() != ApplicationStatus.APPLIED) {
            throw new IllegalArgumentException(
                    "This application can no longer be withdrawn - it is already at the "
                    + application.getStatus() + " stage");
        }

        store.deleteApplication(applicationId);
        RequestUtil.sendJson(exchange, 200,
                JsonWriter.obj().put("withdrawn", true).put("id", applicationId).toString());
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
                .put("requiredDocument", app.getRequiredDocument())
                .put("evaluationScore", app.getEvaluationScore())
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
