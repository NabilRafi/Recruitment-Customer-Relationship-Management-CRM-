package com.recruitcrm.web;

import com.recruitcrm.domain.Job;
import com.recruitcrm.domain.JobType;
import com.recruitcrm.patterns.decorator.BasicJobPosting;
import com.recruitcrm.patterns.decorator.FeaturedJobDecorator;
import com.recruitcrm.patterns.decorator.JobPostingComponent;
import com.recruitcrm.patterns.decorator.UrgentJobDecorator;
import com.recruitcrm.patterns.singleton.DataStore;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Exposes Job data over HTTP. Every job's display fields are computed by
 * wrapping it in the Decorator chain (see patterns.decorator) based on
 * its persisted featured/urgent flags — the API doesn't duplicate that
 * logic, it just asks the decorators for the result.
 *
 * Routes:
 *   GET  /api/jobs                -> list all jobs
 *   POST /api/jobs                -> create a job
 *   POST /api/jobs/{id}/feature   -> toggle Featured
 *   POST /api/jobs/{id}/urgent    -> toggle Urgent
 */
public class JobsHandler implements HttpHandler {
    private final DataStore store = DataStore.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String[] segments = exchange.getRequestURI().getPath().split("/");
        // "", "api", "jobs" [, "{id}", "{action}"]

        try {
            if (method.equals("GET") && segments.length == 3) {
                listJobs(exchange);
            } else if (method.equals("POST") && segments.length == 3) {
                AuthUtil.requireRole(exchange, "RECRUITER");
                createJob(exchange);
            } else if (method.equals("POST") && segments.length == 5 && segments[4].equals("feature")) {
                AuthUtil.requireRole(exchange, "RECRUITER");
                toggleFlag(exchange, segments[3], true);
            } else if (method.equals("POST") && segments.length == 5 && segments[4].equals("urgent")) {
                AuthUtil.requireRole(exchange, "RECRUITER");
                toggleFlag(exchange, segments[3], false);
            } else {
                RequestUtil.sendError(exchange, 404, "Unknown route");
            }
        } catch (IllegalArgumentException e) {
            RequestUtil.sendError(exchange, 400, e.getMessage());
        } catch (IllegalStateException e) {
            // AuthUtil already sent 401/403
        } catch (Exception e) {
            RequestUtil.sendError(exchange, 500, "Server error: " + e.getMessage());
        }
    }

    private void listJobs(HttpExchange exchange) throws IOException {
        List<String> jobJsons = new ArrayList<>();
        for (Job job : store.getAllJobs()) {
            jobJsons.add(toJson(job));
        }
        RequestUtil.sendJson(exchange, 200, JsonWriter.array(jobJsons));
    }

    private void createJob(HttpExchange exchange) throws IOException {
        Map<String, String> form = RequestUtil.parseFormBody(exchange);
        String title = require(form, "title");
        String company = require(form, "company");
        String description = form.getOrDefault("description", "");

        JobType type;
        try {
            type = JobType.valueOf(form.getOrDefault("type", "FULL_TIME"));
        } catch (IllegalArgumentException e) {
            type = JobType.FULL_TIME;
        }

        String id = "job-" + UUID.randomUUID().toString().substring(0, 8);
        Job job = new Job(id, title, company, type, description);
        var recruiter = AuthUtil.currentUser(exchange).orElse(null);
        store.saveJob(job, recruiter != null ? recruiter.getEmail() : null);
        RequestUtil.sendJson(exchange, 201, toJson(job));
    }

    private void toggleFlag(HttpExchange exchange, String jobId, boolean isFeatureFlag) throws IOException {
        Job job = store.getJob(jobId);
        if (job == null) {
            RequestUtil.sendError(exchange, 404, "No job with id " + jobId);
            return;
        }
        if (isFeatureFlag) {
            job.setFeatured(!job.isFeatured());
        } else {
            job.setUrgent(!job.isUrgent());
        }
        store.saveJob(job); // persist the toggle
        RequestUtil.sendJson(exchange, 200, toJson(job));
    }

    /** Builds the decorator chain from a job's persisted flags and reads the result back. */
    private String toJson(Job job) {
        JobPostingComponent view = new BasicJobPosting(job);
        if (job.isFeatured()) {
            view = new FeaturedJobDecorator(view);
        }
        if (job.isUrgent()) {
            view = new UrgentJobDecorator(view);
        }
        return JsonWriter.obj()
                .put("id", job.getId())
                .put("title", job.getTitle())
                .put("company", job.getCompanyName())
                .put("type", job.getType().name())
                .put("description", job.getDescription())
                .put("featured", job.isFeatured())
                .put("urgent", job.isUrgent())
                .put("displayTitle", view.getDisplayTitle())
                .put("visibilityScore", view.getVisibilityScore())
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
