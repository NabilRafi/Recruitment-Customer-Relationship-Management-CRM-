package com.recruitcrm;

import com.recruitcrm.domain.*;
import com.recruitcrm.patterns.builder.JobBuilder;
import com.recruitcrm.patterns.builder.JobDirector;
import com.recruitcrm.patterns.decorator.compensation.*;
import com.recruitcrm.patterns.factory.*;
import com.recruitcrm.patterns.observer.*;
import com.recruitcrm.patterns.proxy.*;
import com.recruitcrm.patterns.strategy.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the nine design patterns.
 *
 * Each test states the PROPERTY the pattern is supposed to guarantee,
 * rather than merely calling methods. A test that only checks a getter
 * returns what was passed in proves nothing about the pattern.
 */
@DisplayName("Design pattern behaviour")
class PatternTests {

    // ---------------------------------------------------------------
    // FACTORY METHOD
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Factory Method")
    class FactoryMethodTests {

        @Test
        @DisplayName("each registered type produces its own concrete class")
        void producesCorrectConcreteType() {
            UserAccountFactoryRegistry registry = UserAccountFactoryRegistry.getInstance();

            UserAccount candidate = registry.getFactory("CANDIDATE")
                    .createAccount("Ayesha", "a@test.com", "cv.pdf");
            UserAccount recruiter = registry.getFactory("RECRUITER")
                    .createAccount("Rita", "r@test.com", "TechNova");
            UserAccount company = registry.getFactory("COMPANY")
                    .createAccount("TechNova", "hr@test.com", "Software");

            assertInstanceOf(Candidate.class, candidate);
            assertInstanceOf(Recruiter.class, recruiter);
            assertInstanceOf(Company.class, company);
        }

        @Test
        @DisplayName("role string is case-insensitive")
        void lookupIsCaseInsensitive() {
            UserAccountFactoryRegistry registry = UserAccountFactoryRegistry.getInstance();
            assertNotNull(registry.getFactory("candidate"));
            assertNotNull(registry.getFactory("Candidate"));
            assertNotNull(registry.getFactory("CANDIDATE"));
        }

        @Test
        @DisplayName("unknown type is rejected rather than silently returning null")
        void unknownTypeThrows() {
            UserAccountFactoryRegistry registry = UserAccountFactoryRegistry.getInstance();
            assertThrows(IllegalArgumentException.class,
                    () -> registry.getFactory("ADMIN"));
        }

        @Test
        @DisplayName("OPEN/CLOSED: a new type can be registered without changing existing code")
        void newTypeCanBeRegisteredAtRuntime() {
            UserAccountFactoryRegistry registry = UserAccountFactoryRegistry.getInstance();

            // A brand new account type, defined entirely here in the test.
            registry.register("TESTROLE", (name, email, extra) ->
                    new Candidate(name + " [test]", email, extra));

            UserAccount created = registry.getFactory("TESTROLE")
                    .createAccount("Nabil", "n@test.com", "cv.pdf");

            assertEquals("Nabil [test]", created.getName());
            // No existing factory class was edited to make this pass.
        }

        @Test
        @DisplayName("each account type reports its own role and extra field")
        void polymorphicRoleAndExtra() {
            UserAccount candidate = new Candidate("A", "a@t.com", "cv.pdf");
            UserAccount recruiter = new Recruiter("R", "r@t.com", "TechNova");
            UserAccount company = new Company("C", "c@t.com", "Software");

            assertEquals("CANDIDATE", candidate.getRole());
            assertEquals("RECRUITER", recruiter.getRole());
            assertEquals("COMPANY", company.getRole());

            // getExtra() is polymorphic - no instanceof needed by the caller
            assertEquals("cv.pdf", candidate.getExtra());
            assertEquals("TechNova", recruiter.getExtra());
            assertEquals("Software", company.getExtra());
        }
    }

    // ---------------------------------------------------------------
    // SINGLETON
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Singleton")
    class SingletonTests {

        @Test
        @DisplayName("registry returns the identical object every time")
        void registryIsSingleton() {
            UserAccountFactoryRegistry a = UserAccountFactoryRegistry.getInstance();
            UserAccountFactoryRegistry b = UserAccountFactoryRegistry.getInstance();
            assertSame(a, b);   // reference identity, not equals()
        }

        @Test
        @DisplayName("strategy registry returns the identical object every time")
        void strategyRegistryIsSingleton() {
            assertSame(EvaluationStrategyRegistry.getInstance(),
                       EvaluationStrategyRegistry.getInstance());
        }

        @Test
        @DisplayName("concurrent access still yields exactly one instance")
        void singletonSurvivesConcurrentAccess() throws InterruptedException {
            List<UserAccountFactoryRegistry> seen =
                    java.util.Collections.synchronizedList(new ArrayList<>());
            List<Thread> threads = new ArrayList<>();

            for (int i = 0; i < 20; i++) {
                Thread t = new Thread(() -> seen.add(UserAccountFactoryRegistry.getInstance()));
                threads.add(t);
                t.start();
            }
            for (Thread t : threads) t.join();

            assertEquals(20, seen.size());
            assertEquals(1, seen.stream().distinct().count(),
                    "all threads must have received the same instance");
        }
    }

    // ---------------------------------------------------------------
    // BUILDER
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("only the supplied fields are set; the rest take defaults")
        void optionalFieldsDefault() {
            Job job = new JobBuilder()
                    .title("Backend Engineer")
                    .company("TechNova")
                    .build();

            assertEquals("Backend Engineer", job.getTitle());
            assertEquals(JobType.FULL_TIME, job.getType());   // default
            assertEquals("", job.getLocation());              // never null
            assertEquals("", job.getSalaryRange());
            assertEquals(0.0, job.getBaseSalary());
        }

        @Test
        @DisplayName("all fields can be supplied in any order")
        void fluentChainingSetsEveryField() {
            Job job = new JobBuilder()
                    .baseSalary(50_000)
                    .location("Dhaka")
                    .company("TechNova")
                    .title("Backend Engineer")
                    .type(JobType.CONTRACT)
                    .deadline("30 September 2026")
                    .salaryRange("50k-70k")
                    .description("Java APIs")
                    .featured(true)
                    .urgent(true)
                    .build();

            assertEquals("Backend Engineer", job.getTitle());
            assertEquals("Dhaka", job.getLocation());
            assertEquals(JobType.CONTRACT, job.getType());
            assertEquals(50_000, job.getBaseSalary());
            assertTrue(job.isFeatured());
            assertTrue(job.isUrgent());
        }

        @Test
        @DisplayName("build() validates: a job without a title is rejected")
        void missingTitleRejected() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> new JobBuilder().company("TechNova").build());
            assertTrue(e.getMessage().contains("title"));
        }

        @Test
        @DisplayName("build() validates: a job without a company is rejected")
        void missingCompanyRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> new JobBuilder().title("Backend Engineer").build());
        }

        @Test
        @DisplayName("an id is generated when none is supplied, and preserved when one is")
        void idGeneratedOrPreserved() {
            Job generated = new JobBuilder().title("A").company("B").build();
            assertNotNull(generated.getId());
            assertTrue(generated.getId().startsWith("job-"));

            Job loaded = new JobBuilder().id("job-fixed01").title("A").company("B").build();
            assertEquals("job-fixed01", loaded.getId());
        }

        @Test
        @DisplayName("Director produces a standard internship without the caller knowing the steps")
        void directorBuildsInternship() {
            Job job = new JobDirector()
                    .buildInternship(new JobBuilder(), "QA Intern", "TechNova", "Dhaka");

            assertEquals(JobType.INTERNSHIP, job.getType());
            assertEquals("Dhaka", job.getLocation());
            assertFalse(job.getDeadline().isBlank());
            assertFalse(job.getDescription().isBlank());
        }

        @Test
        @DisplayName("Director marks an urgent role so the Decorator chain can react")
        void directorBuildsUrgentRole() {
            Job job = new JobDirector().buildUrgentFullTimeRole(
                    new JobBuilder(), "Backend Engineer", "TechNova", "Dhaka", "80k-120k");

            assertTrue(job.isUrgent());
            assertEquals(JobType.FULL_TIME, job.getType());
        }
    }

    // ---------------------------------------------------------------
    // STRATEGY
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Strategy")
    class StrategyTests {

        private Application sampleApplication() {
            Candidate candidate = new Candidate("Demo Candidate", "c@test.com", "cv.pdf");
            Job job = new JobBuilder().title("Backend Engineer").company("TechNova").build();
            return new Application("app-1", candidate, job);
        }

        @Test
        @DisplayName("THE KEY PROPERTY: the same score yields different verdicts per strategy")
        void sameScoreDiffersByStrategy() {
            Application app = sampleApplication();
            int score = 65;

            EvaluationResult technical  = new TechnicalEvaluationStrategy().evaluate(app, score);
            EvaluationResult hr         = new HrEvaluationStrategy().evaluate(app, score);
            EvaluationResult behavioral = new BehavioralEvaluationStrategy().evaluate(app, score);

            // 65 is below the technical bar (70) but above the HR bar (60)
            assertFalse(technical.getRequiredDocument().isBlank(),
                    "technical should demand evidence at 65");
            assertTrue(hr.getRequiredDocument().isBlank(),
                    "HR should pass at 65");
            assertFalse(behavioral.getSummary().equals(technical.getSummary()),
                    "each strategy must produce its own wording");
        }

        @Test
        @DisplayName("a high score requires no further evidence")
        void highScoreRequiresNoDocument() {
            EvaluationResult result = new TechnicalEvaluationStrategy()
                    .evaluate(sampleApplication(), 90);
            assertEquals(90, result.getScore());
            assertTrue(result.getRequiredDocument().isBlank());
        }

        @Test
        @DisplayName("a low score demands a specific document")
        void lowScoreDemandsDocument() {
            EvaluationResult result = new TechnicalEvaluationStrategy()
                    .evaluate(sampleApplication(), 30);
            assertTrue(result.getRequiredDocument().toLowerCase().contains("coding"));
        }

        @Test
        @DisplayName("the recruiter's score is carried through, not overwritten")
        void scoreIsNotHardcoded() {
            Application app = sampleApplication();
            for (int score : new int[]{0, 45, 78, 100}) {
                assertEquals(score,
                        new TechnicalEvaluationStrategy().evaluate(app, score).getScore());
            }
        }

        @Test
        @DisplayName("OPEN/CLOSED: a new metric can be registered at runtime")
        void customStrategyCanBeRegistered() {
            EvaluationStrategyRegistry registry = EvaluationStrategyRegistry.getInstance();
            registry.register("PORTFOLIO",
                    new CustomEvaluationStrategy("Design portfolio review", 70,
                            "A portfolio of three recent projects"));

            EvaluationStrategy strategy = registry.get("PORTFOLIO");
            assertNotNull(strategy);
            assertEquals("Design portfolio review", strategy.getName());

            EvaluationResult below = strategy.evaluate(sampleApplication(), 50);
            assertFalse(below.getRequiredDocument().isBlank());

            EvaluationResult above = strategy.evaluate(sampleApplication(), 80);
            assertTrue(above.getRequiredDocument().isBlank());
        }

        @Test
        @DisplayName("an unknown or blank key means 'no evaluation', not an error")
        void blankKeyReturnsNull() {
            EvaluationStrategyRegistry registry = EvaluationStrategyRegistry.getInstance();
            assertNull(registry.get(""));
            assertNull(registry.get(null));
            assertNull(registry.get("NOT_A_REAL_METRIC"));
        }
    }

    // ---------------------------------------------------------------
    // OBSERVER
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Observer")
    class ObserverTests {

        /** A test double that records what it was told. */
        static class RecordingObserver implements ApplicationObserver {
            final List<String> received = new ArrayList<>();
            @Override
            public void onStatusChanged(Application application) {
                received.add(application.getId() + ":" + application.getStatus());
            }
        }

        private Application sampleApplication() {
            Candidate candidate = new Candidate("Demo", "c@test.com", "cv.pdf");
            Job job = new JobBuilder().title("Backend Engineer").company("TechNova").build();
            return new Application("app-1", candidate, job);
        }

        @Test
        @DisplayName("every registered observer is notified of one event")
        void allObserversNotified() {
            ApplicationStatusPublisher publisher = new ApplicationStatusPublisher();
            RecordingObserver first = new RecordingObserver();
            RecordingObserver second = new RecordingObserver();
            publisher.registerObserver(first);
            publisher.registerObserver(second);

            publisher.notifyObservers(sampleApplication());

            assertEquals(1, first.received.size());
            assertEquals(1, second.received.size());
            assertEquals("app-1:APPLIED", first.received.get(0));
        }

        @Test
        @DisplayName("an unregistered observer stops receiving events")
        void unregisteredObserverIsSilent() {
            ApplicationStatusPublisher publisher = new ApplicationStatusPublisher();
            RecordingObserver observer = new RecordingObserver();

            publisher.registerObserver(observer);
            publisher.notifyObservers(sampleApplication());
            publisher.unregisterObserver(observer);
            publisher.notifyObservers(sampleApplication());

            assertEquals(1, observer.received.size(),
                    "should not have received the second event");
        }

        @Test
        @DisplayName("DECOUPLING: a brand new observer type works without changing the publisher")
        void newObserverTypeNeedsNoPublisherChange() {
            ApplicationStatusPublisher publisher = new ApplicationStatusPublisher();
            List<String> smsLog = new ArrayList<>();

            // An "SMS observer" invented entirely here in the test.
            publisher.registerObserver(app -> smsLog.add("SMS to " + app.getCandidate().getName()));
            publisher.notifyObservers(sampleApplication());

            assertEquals(1, smsLog.size());
            assertEquals("SMS to Demo", smsLog.get(0));
        }

        @Test
        @DisplayName("a publisher with no observers does not fail")
        void noObserversIsSafe() {
            assertDoesNotThrow(() ->
                    new ApplicationStatusPublisher().notifyObservers(sampleApplication()));
        }
    }

    // ---------------------------------------------------------------
    // DECORATOR
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Decorator")
    class DecoratorTests {

        @Test
        @DisplayName("an undecorated base salary is just the base amount")
        void baseSalaryAlone() {
            Compensation base = new BaseSalary("Backend Engineer", 50_000);
            assertEquals(50_000, base.getMonthlyAmount());
            assertEquals(1, base.getBreakdown().size());
        }

        @Test
        @DisplayName("a fixed decorator adds its exact amount")
        void fixedDecoratorAdds() {
            Compensation c = new TransportAllowanceDecorator(
                    new BaseSalary("Backend Engineer", 50_000), 5_000);
            assertEquals(55_000, c.getMonthlyAmount());
        }

        @Test
        @DisplayName("a percentage decorator computes from what it wraps")
        void percentageDecoratorComputesFromWrapped() {
            Compensation c = new HousingAllowanceDecorator(
                    new BaseSalary("Backend Engineer", 50_000), 40);
            assertEquals(70_000, c.getMonthlyAmount());   // 50,000 + 40%
        }

        @Test
        @DisplayName("decorators stack to any depth")
        void decoratorsStack() {
            Compensation c = new TransportAllowanceDecorator(
                    new HousingAllowanceDecorator(
                            new BaseSalary("Backend Engineer", 50_000), 40), 5_000);

            assertEquals(75_000, c.getMonthlyAmount());
            assertEquals(3, c.getBreakdown().size(),
                    "one line per component");
        }

        @Test
        @DisplayName("THE KEY PROPERTY: order changes the total when percentages are involved")
        void orderMatters() {
            Compensation housingFirst = new TransportAllowanceDecorator(
                    new HousingAllowanceDecorator(
                            new BaseSalary("Backend Engineer", 50_000), 40), 5_000);

            Compensation transportFirst = new HousingAllowanceDecorator(
                    new TransportAllowanceDecorator(
                            new BaseSalary("Backend Engineer", 50_000), 5_000), 40);

            assertEquals(75_000, housingFirst.getMonthlyAmount());
            assertEquals(77_000, transportFirst.getMonthlyAmount());
            assertNotEquals(housingFirst.getMonthlyAmount(),
                            transportFirst.getMonthlyAmount(),
                    "percentage decorators compute from what is beneath them");
        }

        @Test
        @DisplayName("every decorator contributes to description, amount AND breakdown")
        void decoratorContributesToAllThreeMethods() {
            Compensation base = new BaseSalary("Backend Engineer", 50_000);
            Compensation decorated = new MeritBonusDecorator(base, "Exceptional technical assessment", 8_000);

            assertTrue(decorated.getDescription().length() > base.getDescription().length(),
                    "description must grow");
            assertTrue(decorated.getMonthlyAmount() > base.getMonthlyAmount(),
                    "amount must grow");
            assertEquals(base.getBreakdown().size() + 1, decorated.getBreakdown().size(),
                    "breakdown must gain exactly one line");
        }

        @Test
        @DisplayName("a merit bonus carries its justification into the breakdown")
        void meritBonusCarriesReason() {
            Compensation c = new MeritBonusDecorator(
                    new BaseSalary("Backend Engineer", 50_000),
                    "Strong prior experience", 4_500);

            String lastLine = c.getBreakdown().get(c.getBreakdown().size() - 1);
            assertTrue(lastLine.contains("Strong prior experience"));
            assertTrue(c.getDescription().contains("Strong prior experience"));
        }

        @Test
        @DisplayName("the performance bonus scales with the evaluation score")
        void performanceBonusUsesScore() {
            Compensation low = new PerformanceBonusDecorator(
                    new BaseSalary("Backend Engineer", 50_000), 40);
            Compensation high = new PerformanceBonusDecorator(
                    new BaseSalary("Backend Engineer", 50_000), 90);

            assertTrue(high.getMonthlyAmount() > low.getMonthlyAmount(),
                    "a higher score must produce a larger bonus");
        }

        @Test
        @DisplayName("the calculator assembles a chain from selected keys")
        void calculatorBuildsChain() {
            Compensation c = CompensationCalculator.build(
                    "Backend Engineer", 50_000, "HOUSING,TRANSPORT", 78);

            assertEquals(75_000, c.getMonthlyAmount());
            assertEquals(3, c.getBreakdown().size());
        }

        @Test
        @DisplayName("no entitlements selected leaves the base salary undecorated")
        void noEntitlementsLeavesBase() {
            Compensation c = CompensationCalculator.build("Backend Engineer", 50_000, "", 78);
            assertEquals(50_000, c.getMonthlyAmount());
            assertEquals(1, c.getBreakdown().size());
        }

        @Test
        @DisplayName("the calculator parses merit bonuses with reason and amount")
        void calculatorParsesMeritBonus() {
            Compensation c = CompensationCalculator.build(
                    "Backend Engineer", 50_000,
                    "BONUS:Exceptional technical assessment:8000", 78);

            assertEquals(58_000, c.getMonthlyAmount());
            assertTrue(c.getDescription().contains("Exceptional technical assessment"));
        }

        @Test
        @DisplayName("an unknown entitlement key is ignored rather than crashing")
        void unknownKeyIgnored() {
            Compensation c = CompensationCalculator.build(
                    "Backend Engineer", 50_000, "HOUSING,NOT_A_REAL_KEY", 78);
            assertEquals(70_000, c.getMonthlyAmount());
        }
    }

    // ---------------------------------------------------------------
    // PROXY
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Proxy")
    class ProxyTests {

        private final Candidate subject = new Candidate("Demo Candidate", "c@test.com", "cv.pdf");

        @Test
        @DisplayName("the real subject always exposes the true values")
        void realSubjectExposesEverything() {
            CandidateProfile real = new RealCandidateProfile(subject);
            assertEquals("c@test.com", real.getEmail());
            assertEquals("cv.pdf", real.getResumeLink());
        }

        @Test
        @DisplayName("a recruiter sees the real contact details")
        void recruiterSeesEverything() {
            CandidateProfile proxy = new ProtectedCandidateProfile(
                    subject, new Recruiter("Rita", "r@test.com", "TechNova"));

            assertEquals("c@test.com", proxy.getEmail());
            assertEquals("cv.pdf", proxy.getResumeLink());
        }

        @Test
        @DisplayName("a candidate sees their own record in full")
        void ownerSeesOwnRecord() {
            CandidateProfile proxy = new ProtectedCandidateProfile(subject, subject);
            assertEquals("c@test.com", proxy.getEmail());
        }

        @Test
        @DisplayName("THE KEY PROPERTY: a different candidate gets masked values")
        void otherCandidateIsMasked() {
            CandidateProfile proxy = new ProtectedCandidateProfile(
                    subject, new Candidate("Someone Else", "other@test.com", "other.pdf"));

            assertNotEquals("c@test.com", proxy.getEmail());
            assertNotEquals("cv.pdf", proxy.getResumeLink());
            assertTrue(proxy.getEmail().contains("hidden"));
        }

        @Test
        @DisplayName("an anonymous viewer gets masked values")
        void anonymousIsMasked() {
            CandidateProfile proxy = new ProtectedCandidateProfile(subject, null);
            assertTrue(proxy.getEmail().contains("hidden"));
        }

        @Test
        @DisplayName("the name is never masked - the proxy restricts only what warrants it")
        void nameAlwaysVisible() {
            CandidateProfile proxy = new ProtectedCandidateProfile(subject, null);
            assertEquals("Demo Candidate", proxy.getName());
        }

        @Test
        @DisplayName("the access note explains which view the caller received")
        void accessNoteDiffers() {
            CandidateProfile allowed = new ProtectedCandidateProfile(
                    subject, new Recruiter("Rita", "r@test.com", "TechNova"));
            CandidateProfile denied = new ProtectedCandidateProfile(subject, null);

            assertNotEquals(allowed.getAccessNote(), denied.getAccessNote());
        }

        @Test
        @DisplayName("proxy and real subject are interchangeable through the interface")
        void proxyIsSubstitutable() {
            List<CandidateProfile> profiles = List.of(
                    new RealCandidateProfile(subject),
                    new ProtectedCandidateProfile(subject, null));

            // Both satisfy the same contract; calling code cannot tell them apart.
            for (CandidateProfile p : profiles) {
                assertEquals("Demo Candidate", p.getName());
                assertNotNull(p.getEmail());
                assertNotNull(p.getAccessNote());
            }
        }
    }
}
