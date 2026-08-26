package com.recruitcrm;

import com.recruitcrm.notification.*;
import com.recruitcrm.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ADAPTER PATTERN tests.
 *
 * The network itself is not exercised - that would make the tests slow
 * and dependent on a mail server. What IS tested is the thing the pattern
 * exists for: that any implementation of the Target interface can be
 * substituted without the client noticing.
 */
class AdapterTest {

    /** A test double implementing the same Target interface as the real adapter. */
    private static class CapturingSender implements EmailSender {
        final List<String> sent = new ArrayList<>();
        @Override
        public void send(String toAddress, String subject, String body) {
            sent.add(toAddress + " | " + subject);
        }
    }

    private Application sampleApplication(ApplicationStatus status) {
        Candidate candidate = new Candidate("Demo Candidate", "candidate@demo.com", "cv.pdf");
        Job job = new Job("job-1", "Backend Engineer", "TechNova Ltd", JobType.FULL_TIME, "Java");
        Application app = new Application("app-1", candidate, job);
        app.setStatus(status);
        return app;
    }

    @Test
    @DisplayName("Any EmailSender can be substituted - the Target interface is what matters")
    void targetInterfaceIsSubstitutable() {
        EmailSender sender = new CapturingSender();
        sender.send("a@b.com", "Subject", "Body");

        assertEquals(1, ((CapturingSender) sender).sent.size());
    }

    @Test
    @DisplayName("The console fallback satisfies the same interface as the SMTP adapter")
    void consoleSenderIsAValidTarget() {
        EmailSender sender = new ConsoleEmailSender();
        assertDoesNotThrow(() -> sender.send("a@b.com", "Subject", "Body"));
    }

    @Test
    @DisplayName("EmailConfig falls back to the console sender when no credentials are set")
    void configFallsBackWithoutCredentials() {
        // No MAIL_USERNAME / MAIL_PASSWORD in the test environment.
        EmailSender sender = EmailConfig.buildSender();

        assertInstanceOf(ConsoleEmailSender.class, sender,
                "without credentials the app must still run");
    }

    @Test
    @DisplayName("A template exists for every application status that warrants an email")
    void templatesRegisteredForEachStatus() {
        EmailTemplateRegistry registry = EmailTemplateRegistry.getInstance();

        assertNotNull(registry.getTemplate(ApplicationStatus.APPLIED));
        assertNotNull(registry.getTemplate(ApplicationStatus.SHORTLISTED));
        assertNotNull(registry.getTemplate(ApplicationStatus.INTERVIEW_SCHEDULED));
        assertNotNull(registry.getTemplate(ApplicationStatus.HIRED));
        assertNotNull(registry.getTemplate(ApplicationStatus.REJECTED));
    }

    @Test
    @DisplayName("Each status produces a DIFFERENT subject line")
    void eachStatusHasItsOwnSubject() {
        EmailTemplateRegistry registry = EmailTemplateRegistry.getInstance();

        String shortlisted = registry.getTemplate(ApplicationStatus.SHORTLISTED)
                .subject(sampleApplication(ApplicationStatus.SHORTLISTED));
        String rejected = registry.getTemplate(ApplicationStatus.REJECTED)
                .subject(sampleApplication(ApplicationStatus.REJECTED));

        assertNotEquals(shortlisted, rejected);
        assertTrue(shortlisted.toLowerCase().contains("shortlist"));
    }

    @Test
    @DisplayName("The offer letter includes the itemised compensation package")
    void offerLetterIncludesCompensation() {
        Application app = sampleApplication(ApplicationStatus.HIRED);
        app.setOfferEntitlements("HOUSING");

        // A job with a numeric base salary so the Decorator chain runs.
        Job paidJob = new Job("job-2", "Backend Engineer", "TechNova Ltd",
                JobType.FULL_TIME, "Java", "Dhaka", "", "", 50_000);
        Application paidApp = new Application("app-2",
                new Candidate("Demo Candidate", "candidate@demo.com", "cv.pdf"), paidJob);
        paidApp.setStatus(ApplicationStatus.HIRED);
        paidApp.setOfferEntitlements("HOUSING");

        String body = EmailTemplateRegistry.getInstance()
                .getTemplate(ApplicationStatus.HIRED).body(paidApp);

        assertTrue(body.contains("COMPENSATION PACKAGE"));
        assertTrue(body.contains("House rent allowance"),
                "the Decorator chain reached the email");
    }

    @Test
    @DisplayName("An offer with no base salary degrades gracefully")
    void offerWithoutBaseSalaryIsStillValid() {
        String body = EmailTemplateRegistry.getInstance()
                .getTemplate(ApplicationStatus.HIRED)
                .body(sampleApplication(ApplicationStatus.HIRED));

        assertTrue(body.contains("To be confirmed"));
    }
}
