package com.recruitcrm.patterns.proxy;

/**
 * PROXY PATTERN — the "Subject" interface.
 *
 * Both the real profile and the protection proxy implement this, which
 * is what makes them interchangeable: calling code holds a
 * CandidateProfile and cannot tell which one it received.
 */
public interface CandidateProfile {
    String getName();
    String getEmail();
    String getResumeLink();
    String getAccessNote();
}
