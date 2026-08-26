package com.recruitcrm;

import com.recruitcrm.domain.*;
import com.recruitcrm.patterns.facade.RecruitmentFacade;
import com.recruitcrm.patterns.observer.ApplicationObserver;
import com.recruitcrm.patterns.strategy.TechnicalEvaluationStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FACADE PATTERN tests.
 *
 * INTEGRATION-LEVEL: the Facade holds the DataStore Singleton, so these
 * touch the real SQLite file. They verify that ONE call correctly drives
 * three subsystems - persistence, strategy and notification.
 */
class FacadeTest {

    private Candidate candidate() {
        return new Candidate("Facade Test Candidate", "facadetest@demo.com", "cv.pdf");
    }

    private Job job() {
        return new Job("job-facade-test", "Backend Engineer", "TechNova Ltd",
                JobType.FULL_TIME, "Java APIs", "Dhaka", "", "", 50_000);
    }

    @Test
    @DisplayName("ONE call to updateStatus drives the Strategy AND the Observers")
    void oneCallDrivesMultipleSubsystems() {
        RecruitmentFacade facade = new RecruitmentFacade();
        List<String> notified = new ArrayList<>();
        facade.addObserver(app -> notified.add(app.getStatus().name()));

        Application application = new Application("app-facade-1", candidate(), job());

        facade.updateStatus(application, ApplicationStatus.SHORTLISTED,
                new TechnicalEvaluationStrategy(), 88);

        // Subsystem: Strategy ran
        assertEquals(88, application.getEvaluationScore());
        assertNotNull(application.getLastEvaluationSummary());

        // The state change happened
        assertEquals(ApplicationStatus.SHORTLISTED, application.getStatus());

        // Subsystem: Observers were notified
        assertEquals(1, notified.size());
        assertEquals("SHORTLISTED", notified.get(0));
    }

    @Test
    @DisplayName("A null strategy is valid - closing an application needs no assessment")
    void nullStrategyIsAllowed() {
        RecruitmentFacade facade = new RecruitmentFacade();
        Application application = new Application("app-facade-2", candidate(), job());

        assertDoesNotThrow(() ->
                facade.updateStatus(application, ApplicationStatus.REJECTED, null, 0));

        assertEquals(ApplicationStatus.REJECTED, application.getStatus());
    }

    @Test
    @DisplayName("Observers are still notified even when no strategy runs")
    void observersNotifiedWithoutStrategy() {
        RecruitmentFacade facade = new RecruitmentFacade();
        List<String> notified = new ArrayList<>();
        facade.addObserver(app -> notified.add(app.getId()));

        facade.updateStatus(new Application("app-facade-3", candidate(), job()),
                ApplicationStatus.REJECTED, null, 0);

        assertEquals(1, notified.size());
    }

    @Test
    @DisplayName("EVERY registered observer is reached through the Facade")
    void facadeReachesAllObservers() {
        RecruitmentFacade facade = new RecruitmentFacade();
        List<String> emailLog = new ArrayList<>();
        List<String> auditLog = new ArrayList<>();

        facade.addObserver(app -> emailLog.add("email"));
        facade.addObserver(app -> auditLog.add("audit"));

        facade.updateStatus(new Application("app-facade-4", candidate(), job()),
                ApplicationStatus.SHORTLISTED, new TechnicalEvaluationStrategy(), 75);

        assertEquals(1, emailLog.size());
        assertEquals(1, auditLog.size());
    }

    @Test
    @DisplayName("The evaluation score is recorded so the Decorator chain can use it later")
    void scoreIsRecordedForTheDecoratorChain() {
        RecruitmentFacade facade = new RecruitmentFacade();
        Application application = new Application("app-facade-5", candidate(), job());

        facade.updateStatus(application, ApplicationStatus.SHORTLISTED,
                new TechnicalEvaluationStrategy(), 78);

        // PerformanceBonusDecorator reads this when the offer is built.
        assertEquals(78, application.getEvaluationScore());
    }

    @Test
    @DisplayName("The required document from the Strategy reaches the Application")
    void requiredDocumentIsRecorded() {
        RecruitmentFacade facade = new RecruitmentFacade();
        Application application = new Application("app-facade-6", candidate(), job());

        // 40 is below the technical bar, so evidence is requested.
        facade.updateStatus(application, ApplicationStatus.SHORTLISTED,
                new TechnicalEvaluationStrategy(), 40);

        assertFalse(application.getRequiredDocument().isBlank());
    }
}
