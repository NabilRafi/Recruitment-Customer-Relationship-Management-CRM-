package com.recruitcrm;

import com.recruitcrm.domain.*;
import com.recruitcrm.patterns.proxy.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PROXY PATTERN tests.
 *
 * These prove the proxy genuinely WITHHOLDS data based on who is asking,
 * and that the RealSubject is unaffected - it always knows the true
 * values, because permission logic lives only in the proxy.
 */
class ProxyTest {

    private final Candidate candidate =
            new Candidate("Demo Candidate", "candidate@demo.com", "https://example.com/cv");

    private final UserAccount recruiter =
            new Recruiter("Rita", "recruiter@demo.com", "TechNova Ltd");

    private final UserAccount otherCandidate =
            new Candidate("Someone Else", "other@demo.com", "other-cv.pdf");

    @Test
    @DisplayName("A recruiter sees the real email and resume")
    void recruiterSeesEverything() {
        CandidateProfile profile = new ProtectedCandidateProfile(candidate, recruiter);

        assertEquals("candidate@demo.com", profile.getEmail());
        assertEquals("https://example.com/cv", profile.getResumeLink());
    }

    @Test
    @DisplayName("A candidate sees their OWN real details")
    void candidateSeesOwnRecord() {
        CandidateProfile profile = new ProtectedCandidateProfile(candidate, candidate);

        assertEquals("candidate@demo.com", profile.getEmail());
        assertEquals("https://example.com/cv", profile.getResumeLink());
    }

    @Test
    @DisplayName("A DIFFERENT candidate gets masked contact details")
    void otherCandidateIsMasked() {
        CandidateProfile profile = new ProtectedCandidateProfile(candidate, otherCandidate);

        assertNotEquals("candidate@demo.com", profile.getEmail());
        assertTrue(profile.getEmail().contains("hidden"));
        assertTrue(profile.getResumeLink().contains("hidden"));
    }

    @Test
    @DisplayName("An anonymous viewer gets masked contact details")
    void anonymousIsMasked() {
        CandidateProfile profile = new ProtectedCandidateProfile(candidate, null);

        assertTrue(profile.getEmail().contains("hidden"));
    }

    @Test
    @DisplayName("The name is never masked - it is not sensitive")
    void nameIsAlwaysVisible() {
        assertEquals("Demo Candidate",
                new ProtectedCandidateProfile(candidate, null).getName());
        assertEquals("Demo Candidate",
                new ProtectedCandidateProfile(candidate, otherCandidate).getName());
    }

    @Test
    @DisplayName("The access note explains WHY fields are hidden")
    void accessNoteExplainsTheDecision() {
        assertTrue(new ProtectedCandidateProfile(candidate, recruiter)
                .getAccessNote().toLowerCase().contains("full"));

        assertTrue(new ProtectedCandidateProfile(candidate, otherCandidate)
                .getAccessNote().toLowerCase().contains("limited"));
    }

    @Test
    @DisplayName("The RealSubject contains NO permission logic - it always tells the truth")
    void realSubjectHasNoPermissionLogic() {
        CandidateProfile real = new RealCandidateProfile(candidate);

        assertEquals("candidate@demo.com", real.getEmail(),
                "the real profile never masks; that is the proxy's job alone");
    }

    @Test
    @DisplayName("Email comparison is case-insensitive, so casing cannot defeat the check")
    void ownershipCheckIsCaseInsensitive() {
        UserAccount sameCandidateDifferentCase =
                new Candidate("Demo Candidate", "CANDIDATE@DEMO.COM", "cv.pdf");

        CandidateProfile profile =
                new ProtectedCandidateProfile(candidate, sameCandidateDifferentCase);

        assertEquals("candidate@demo.com", profile.getEmail());
    }

    @Test
    @DisplayName("Proxy and RealSubject are interchangeable through the Subject interface")
    void bothImplementTheSameInterface() {
        CandidateProfile viaProxy = new ProtectedCandidateProfile(candidate, recruiter);
        CandidateProfile viaReal  = new RealCandidateProfile(candidate);

        // Identical calls; the caller cannot tell which it holds.
        assertEquals(viaReal.getName(), viaProxy.getName());
        assertEquals(viaReal.getEmail(), viaProxy.getEmail());
    }
}
