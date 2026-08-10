package com.recruitcrm.patterns.decorator;

import com.recruitcrm.domain.Job;

/** DECORATOR PATTERN — the "ConcreteComponent": a plain, undecorated posting. */
public class BasicJobPosting implements JobPostingComponent {
    private final Job job;

    public BasicJobPosting(Job job) {
        this.job = job;
    }

    @Override
    public String getDisplayTitle() {
        return job.getTitle() + " @ " + job.getCompanyName();
    }

    @Override
    public int getVisibilityScore() {
        return 10; // baseline visibility
    }
}
