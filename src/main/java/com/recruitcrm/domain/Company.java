package com.recruitcrm.domain;

import java.io.Serializable;

public class Company implements UserAccount, Serializable {
    private final String name;
    private final String email;
    private final String industry;

    public Company(String name, String email, String industry) {
        this.name = name;
        this.email = email;
        this.industry = industry;
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
        return "COMPANY";
    }

    public String getIndustry() {
        return industry;
    }

    @Override
    public String describe() {
        return "Company[" + name + ", " + email + ", industry=" + industry + "]";
    }

    /** The industry is this account type's "extra" field (see UserAccount.getExtra). */
    @Override
    public String getExtra() {
        return getIndustry();
    }
}
