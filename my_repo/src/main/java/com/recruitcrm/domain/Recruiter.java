package com.recruitcrm.domain;

import java.io.Serializable;

public class Recruiter implements UserAccount, Serializable {
    private final String name;
    private final String email;
    private final String companyName;

    public Recruiter(String name, String email, String companyName) {
        this.name = name;
        this.email = email;
        this.companyName = companyName;
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
        return "RECRUITER";
    }

    public String getCompanyName() {
        return companyName;
    }

    @Override
    public String describe() {
        return "Recruiter[" + name + ", " + email + ", company=" + companyName + "]";
    }
}
