package com.recruitcrm;

import com.recruitcrm.domain.*;
import com.recruitcrm.patterns.observer.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** OBSERVER PATTERN tests: subscribe, broadcast, unsubscribe, decoupling. */
class ObserverTest {

    /** A test double that simply records what it was told. */
    private static class RecordingObserver implements ApplicationObserver {
        final List<String> seen = new ArrayList<>();
        @Override
        public void onStatusChanged(Application application) {
            seen.add(application.getId() + ":" + application.getStatus());
        }
    }

    private Application sampleApplication() {
        Candidate candidate = new Candidate("Demo Candidate", "candidate@demo.com", "cv.pdf");
        Job job = new Job("job-1", "Backend Engineer", "TechNova Ltd", JobType.FULL_TIME, "Java");
        return new Application("app-1", candidate, job);
    }

    @Test
    @DisplayName("A registered observer is notified")
    void registeredObserverIsNotified() {
        ApplicationStatusPublisher publisher = new ApplicationStatusPublisher();
        RecordingObserver observer = new RecordingObserver();
        publisher.registerObserver(observer);

        publisher.notifyObservers(sampleApplication());

        assertEquals(1, observer.seen.size());
        assertEquals("app-1:APPLIED", observer.seen.get(0));
    }

    @Test
    @DisplayName("EVERY registered observer receives the same single event")
    void allObserversNotified() {
        ApplicationStatusPublisher publisher = new ApplicationStatusPublisher();
        RecordingObserver first = new RecordingObserver();
        RecordingObserver second = new RecordingObserver();
        RecordingObserver third = new RecordingObserver();

        publisher.registerObserver(first);
        publisher.registerObserver(second);
        publisher.registerObserver(third);

        publisher.notifyObservers(sampleApplication());

        assertEquals(1, first.seen.size());
        assertEquals(1, second.seen.size());
        assertEquals(1, third.seen.size(), "one event reaches all three independently");
    }

    @Test
    @DisplayName("An unregistered observer stops receiving events")
    void unregisteredObserverStopsReceiving() {
        ApplicationStatusPublisher publisher = new ApplicationStatusPublisher();
        RecordingObserver observer = new RecordingObserver();

        publisher.registerObserver(observer);
        publisher.notifyObservers(sampleApplication());
        publisher.unregisterObserver(observer);
        publisher.notifyObservers(sampleApplication());

        assertEquals(1, observer.seen.size(), "only the first notification arrived");
    }

    @Test
    @DisplayName("A publisher with no observers does not fail")
    void noObserversIsSafe() {
        ApplicationStatusPublisher publisher = new ApplicationStatusPublisher();
        assertDoesNotThrow(() -> publisher.notifyObservers(sampleApplication()));
    }

    @Test
    @DisplayName("Observers see the CURRENT status, not the status at registration time")
    void observersSeeCurrentStatus() {
        ApplicationStatusPublisher publisher = new ApplicationStatusPublisher();
        RecordingObserver observer = new RecordingObserver();
        publisher.registerObserver(observer);

        Application app = sampleApplication();
        app.setStatus(ApplicationStatus.SHORTLISTED);
        publisher.notifyObservers(app);

        assertEquals("app-1:SHORTLISTED", observer.seen.get(0));
    }

    @Test
    @DisplayName("EXTENSIBILITY: adding a new observer type needs no change to the publisher")
    void newObserverTypeNeedsNoPublisherChange() {
        ApplicationStatusPublisher publisher = new ApplicationStatusPublisher();
        List<String> smsLog = new ArrayList<>();

        // A brand new observer, defined here, never seen by the publisher's code.
        publisher.registerObserver(app -> smsLog.add("SMS to " + app.getCandidate().getName()));
        publisher.notifyObservers(sampleApplication());

        assertEquals(1, smsLog.size());
        assertTrue(smsLog.get(0).contains("Demo Candidate"));
    }
}
