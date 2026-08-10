package com.recruitcrm.patterns.factory;

import com.recruitcrm.domain.Candidate;
import com.recruitcrm.domain.UserAccount;

/** Concrete Creator: only ever builds a Candidate. Zero conditionals. */
public class CandidateFactory implements UserAccountFactory {
    @Override
    public UserAccount createAccount(String name, String email, String extra) {
        // "extra" is the resume link for a candidate
        return new Candidate(name, email, extra);
    }
}
