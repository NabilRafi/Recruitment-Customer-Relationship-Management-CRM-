package com.recruitcrm.domain;

import java.io.Serializable;

public class Job implements Serializable {
    private final String id;
    private final String title;
    private final String companyName;
    private final JobType type;
    private final String description;

    // Optional fields. These are why the Builder pattern exists here: a
    // constructor taking all of them would be a long, unreadable list of
    // arguments where it is easy to swap two by mistake.
    private final String location;
    private final String salaryRange;
    private final String deadline;

    // Persisted flags that the Decorator pattern reads at display time
    // (see patterns.decorator) to decide how to render this job.
    private boolean featured = false;
    private boolean urgent = false;

    /**
     * Kept for the simple case and for older calling code.
     * New code should prefer JobBuilder (see patterns.builder).
     */
    public Job(String id, String title, String companyName, JobType type, String description) {
        this(id, title, companyName, type, description, "", "", "");
    }

    public Job(String id, String title, String companyName, JobType type, String description,
               String location, String salaryRange, String deadline) {
        this.id = id;
        this.title = title;
        this.companyName = companyName;
        this.type = type;
        this.description = description;
        this.location = location == null ? "" : location;
        this.salaryRange = salaryRange == null ? "" : salaryRange;
        this.deadline = deadline == null ? "" : deadline;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCompanyName() { return companyName; }
    public JobType getType() { return type; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getSalaryRange() { return salaryRange; }
    public String getDeadline() { return deadline; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public boolean isUrgent() { return urgent; }
    public void setUrgent(boolean urgent) { this.urgent = urgent; }
}