package com.recruitcrm.patterns.proxy;

import com.recruitcrm.domain.Candidate;

/**
 * PROXY PATTERN — the "RealSubject".
 *
 * Holds and returns the candidate's actual personal data. It contains no
 * permission logic at all: its only job is to know the real values.
 * Deciding WHO may see them belongs to the proxy, which keeps the two
 * concerns separate.
 */
public class RealCandidateProfile implements CandidateProfile {

    private final Candidate candidate;

    public RealCandidateProfile(Candidate candidate) {
        this.candidate = candidate;
    }

    @Override
    public String getName() {
        return candidate.getName();
    }

    @Override
    public String getEmail() {
        return candidate.getEmail();
    }

    @Override
    public String getResumeLink() {
        return candidate.getResumeLink();
    }

    @Override
    public String getAccessNote() {
        return "Full profile.";
    }
}
