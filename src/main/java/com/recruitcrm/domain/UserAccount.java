package com.recruitcrm.domain;

import java.io.Serializable;

/**
 * Common interface implemented by every kind of account in the system.
 * Candidate, Recruiter, and Company are the concrete types — this is the
 * "Product" side of the Factory Method pattern (see patterns.factory).
 */
public interface UserAccount extends Serializable {
    String getName();
    String getEmail();
    String getRole();
    String describe();

    /**
     * The one extra field each account type carries (resume link for a
     * Candidate, company name for a Recruiter, industry for a Company).
     *
     * Declaring it here lets callers read that field WITHOUT asking
     * "what type is this?" first. That is polymorphism doing the job an
     * instanceof / switch chain would otherwise do — the same principle
     * that makes the Factory Method registry work.
     */
    String getExtra();
}
