package com.recruitcrm.patterns.builder;

import com.recruitcrm.domain.Job;
import com.recruitcrm.domain.JobType;

import java.util.UUID;

/**
 * BUILDER PATTERN — the "Builder" role.
 *
 * A Job has three required fields (title, company, type) and several
 * optional ones (description, location, salary, deadline, plus the
 * Featured/Urgent flags). Building it with one constructor would mean:
 *
 *     new Job(id, title, company, type, desc, location, salary, deadline)
 *
 * ...eight arguments in a fixed order, most of them String, where
 * swapping two by accident compiles fine and fails silently at runtime.
 *
 * The Builder lets the caller name each part as it is added, supply only
 * the parts it actually has, and produce the finished object in one final
 * build() call. The Job itself stays immutable — its fields are final and
 * there are no setters for them.
 *
 * Each setter returns "this", which is what allows the calls to be
 * chained together (a fluent interface).
 */
public class JobBuilder {

    private String id;
    private String title;
    private String companyName;
    private JobType type = JobType.FULL_TIME;   // sensible default
    private String description = "";
    private String location = "";
    private String salaryRange = "";
    private String deadline = "";
    private boolean featured = false;
    private boolean urgent = false;

    public JobBuilder title(String title) {
        this.title = title;
        return this;
    }

    public JobBuilder company(String companyName) {
        this.companyName = companyName;
        return this;
    }

    public JobBuilder type(JobType type) {
        this.type = type;
        return this;
    }

    public JobBuilder description(String description) {
        this.description = description;
        return this;
    }

    public JobBuilder location(String location) {
        this.location = location;
        return this;
    }

    public JobBuilder salaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
        return this;
    }

    public JobBuilder deadline(String deadline) {
        this.deadline = deadline;
        return this;
    }

    public JobBuilder featured(boolean featured) {
        this.featured = featured;
        return this;
    }

    public JobBuilder urgent(boolean urgent) {
        this.urgent = urgent;
        return this;
    }

    /** Used when loading an existing job from the database. */
    public JobBuilder id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Produces the finished Job.
     *
     * Validation lives here rather than in the Job constructor, so the
     * builder can check the whole object at once — it is the only point
     * at which every part is known.
     */
    public Job build() {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("A job needs a title");
        }
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("A job needs a company name");
        }
        if (id == null) {
            id = "job-" + UUID.randomUUID().toString().substring(0, 8);
        }

        Job job = new Job(id, title, companyName, type, description, location, salaryRange, deadline);
        job.setFeatured(featured);
        job.setUrgent(urgent);
        return job;
    }
}
