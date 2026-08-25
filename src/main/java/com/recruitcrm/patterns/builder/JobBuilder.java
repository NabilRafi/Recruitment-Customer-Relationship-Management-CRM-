package com.recruitcrm.patterns.builder;

import com.recruitcrm.domain.Job;
import com.recruitcrm.domain.JobType;

import java.util.UUID;


public class JobBuilder {

    private String id;
    private String title;
    private String companyName;
    private JobType type = JobType.FULL_TIME;   // sensible default
    private String description = "";
    private String location = "";
    private String salaryRange = "";
    private String deadline = "";
    private double baseSalary = 0;
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

    public JobBuilder baseSalary(double baseSalary) {
        this.baseSalary = baseSalary;
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

        Job job = new Job(id, title, companyName, type, description, location, salaryRange, deadline, baseSalary);
        job.setFeatured(featured);
        job.setUrgent(urgent);
        return job;
    }
}
