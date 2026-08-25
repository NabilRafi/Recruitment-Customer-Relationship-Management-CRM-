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

    /**
     * Numeric monthly base pay in BDT. Separate from the free-text
     * salaryRange shown on the listing, because the Decorator chain that
     * builds an offer package needs a number to calculate from.
     */
    private final double baseSalary;

    // Persisted flags that the Decorator pattern reads at display time.
    private boolean featured = false;
    private boolean urgent = false;

    /** Kept for the simple case. New code should prefer JobBuilder. */
    public Job(String id, String title, String companyName, JobType type, String description) {
        this(id, title, companyName, type, description, "", "", "", 0);
    }

    public Job(String id, String title, String companyName, JobType type, String description,
               String location, String salaryRange, String deadline) {
        this(id, title, companyName, type, description, location, salaryRange, deadline, 0);
    }

    public Job(String id, String title, String companyName, JobType type, String description,
               String location, String salaryRange, String deadline, double baseSalary) {
        this.id = id;
        this.title = title;
        this.companyName = companyName;
        this.type = type;
        this.description = description;
        this.location = location == null ? "" : location;
        this.salaryRange = salaryRange == null ? "" : salaryRange;
        this.deadline = deadline == null ? "" : deadline;
        this.baseSalary = baseSalary;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCompanyName() { return companyName; }
    public JobType getType() { return type; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getSalaryRange() { return salaryRange; }
    public String getDeadline() { return deadline; }
    public double getBaseSalary() { return baseSalary; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public boolean isUrgent() { return urgent; }
    public void setUrgent(boolean urgent) { this.urgent = urgent; }
}