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
}
