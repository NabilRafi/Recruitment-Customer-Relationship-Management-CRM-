package com.recruitcrm.domain;

import java.io.Serializable;

public class Application implements Serializable {
    private final String id;
    private final Candidate candidate;
    private final Job job;
    private ApplicationStatus status;
    private String lastEvaluationSummary;

    public Application(String id, Candidate candidate, Job job) {
        this.id = id;
        this.candidate = candidate;
        this.job = job;
        this.status = ApplicationStatus.APPLIED;
    }

    public String getId() {
        return id;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public Job getJob() {
        return job;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }


    private String interviewDetails = "";

    public String getInterviewDetails() {
        return interviewDetails;
    }

    public void setInterviewDetails(String interviewDetails) {
        this.interviewDetails = interviewDetails == null ? "" : interviewDetails;
    }
    
        
    private String offerEntitlements = "";

    public String getOfferEntitlements() {
        return offerEntitlements;
    }

    public void setOfferEntitlements(String offerEntitlements) {
        this.offerEntitlements = offerEntitlements == null ? "" : offerEntitlements;
    }

    /** Latest numeric evaluation score, feeding the performance bonus decorator. */
    private int evaluationScore = 0;

    public int getEvaluationScore() {
        return evaluationScore;
    }

    public void setEvaluationScore(int evaluationScore) {
        this.evaluationScore = evaluationScore;
    }

    public String getLastEvaluationSummary() {
        return lastEvaluationSummary;
    }

    public void setLastEvaluationSummary(String summary) {
        this.lastEvaluationSummary = summary;
    }
}
