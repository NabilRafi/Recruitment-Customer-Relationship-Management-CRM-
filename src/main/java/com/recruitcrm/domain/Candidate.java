package com.recruitcrm.domain;

import java.io.Serializable;

public class Candidate implements UserAccount, Serializable {
    private final String name;
    private final String email;
    private final String resumeLink;

    public Candidate(String name, String email, String resumeLink) {
        this.name = name;
        this.email = email;
        this.resumeLink = resumeLink;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getRole() {
        return "CANDIDATE";
    }

    public String getResumeLink() {
        return resumeLink;
    }

    @Override
    public String describe() {
        return "Candidate[" + name + ", " + email + ", resume=" + resumeLink + "]";
    }
}
