package com.recruitcrm.domain;

import java.io.Serializable;

public class Job implements Serializable {
    private final String id;
    private final String title;
    private final String companyName;
    private final JobType type;
    private final String description;

    // Persisted flags that the Decorator pattern reads at display time
    // (see patterns.decorator) to decide how to render this job.
    private boolean featured = false;
    private boolean urgent = false;

    public Job(String id, String title, String companyName, JobType type, String description) {
        this.id = id;
        this.title = title;
        this.companyName = companyName;
        this.type = type;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCompanyName() {
        return companyName;
    }

    public JobType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }
}
