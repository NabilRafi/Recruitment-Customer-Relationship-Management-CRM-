package com.recruitcrm.patterns.factory;

import com.recruitcrm.domain.Recruiter;
import com.recruitcrm.domain.UserAccount;

/** Concrete Creator: only ever builds a Recruiter. Zero conditionals. */
public class RecruiterFactory implements UserAccountFactory {
    @Override
    public UserAccount createAccount(String name, String email, String extra) {
        // "extra" is the company name for a recruiter
        return new Recruiter(name, email, extra);
    }
}
