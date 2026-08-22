package com.recruitcrm.patterns.proxy;

import com.recruitcrm.domain.Candidate;
import com.recruitcrm.domain.UserAccount;

/**
 * PROXY PATTERN — the "Proxy" (protection proxy variant).
 *
 * Stands in front of a RealCandidateProfile and decides, per caller,
 * which fields may be read. The caller holds a CandidateProfile and
 * calls the same methods either way; the redaction happens invisibly.
 *
 *     Client  ->  CandidateProfile  ->  ProtectedCandidateProfile
 *                    (Subject)                  (Proxy)
 *                                                  |
 *                                                  v
 *                                        RealCandidateProfile
 *                                            (RealSubject)
 *
 * Access rules:
 *   - RECRUITER            : full access (they are doing the hiring)
 *   - the candidate itself : full access to their own record
 *   - anyone else          : name only; email and resume are masked
 *
 * Two other proxy responsibilities are handled here as well:
 *
 *   LAZY CREATION — the RealCandidateProfile is only constructed the
 *   first time a field is actually read. A caller that checks
 *   permissions and then displays nothing never builds it at all. This
 *   is the same idea as the lazy image-loading proxy from the lecture.
 *
 *   ACCESS LOGGING — every denied read is recorded. The RealSubject
 *   stays free of this concern, which is exactly why a proxy is a better
 *   fit here than adding permission checks inside the profile class.
 */
public class ProtectedCandidateProfile implements CandidateProfile {

    private static final String MASKED = "[hidden — recruiter access only]";

    private final Candidate candidate;
    private final UserAccount viewer;

    /** Created on first real access, not in the constructor. */
    private RealCandidateProfile realProfile;

    public ProtectedCandidateProfile(Candidate candidate, UserAccount viewer) {
        this.candidate = candidate;
        this.viewer = viewer;
    }

    private RealCandidateProfile real() {
        if (realProfile == null) {
            realProfile = new RealCandidateProfile(candidate);
            System.out.println("[PROXY] Loaded real profile for " + candidate.getEmail());
        }
        return realProfile;
    }

    private boolean viewerMaySeeContactDetails() {
        if (viewer == null) {
            return false;
        }
        if (viewer.getRole().equals("RECRUITER")) {
            return true;
        }
        // A candidate may always see their own record.
        return viewer.getEmail().equalsIgnoreCase(candidate.getEmail());
    }

    private void logDenied(String field) {
        String who = viewer == null ? "anonymous" : viewer.getEmail() + " (" + viewer.getRole() + ")";
        System.out.println("[PROXY] Denied " + who + " access to " + field
                + " of " + candidate.getEmail());
    }

    // ---- Subject methods -------------------------------------------------

    @Override
    public String getName() {
        // A name is not sensitive — everyone may read it.
        return real().getName();
    }

    @Override
    public String getEmail() {
        if (!viewerMaySeeContactDetails()) {
            logDenied("email");
            return MASKED;
        }
        return real().getEmail();
    }

    @Override
    public String getResumeLink() {
        if (!viewerMaySeeContactDetails()) {
            logDenied("resume");
            return MASKED;
        }
        return real().getResumeLink();
    }

    @Override
    public String getAccessNote() {
        return viewerMaySeeContactDetails()
                ? "Full profile — you are authorised to view contact details."
                : "Limited view. Contact details are visible to recruiters only.";
    }
}
