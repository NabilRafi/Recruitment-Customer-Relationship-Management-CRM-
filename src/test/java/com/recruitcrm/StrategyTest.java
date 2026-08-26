package com.recruitcrm;

import com.recruitcrm.domain.*;
import com.recruitcrm.patterns.strategy.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * STRATEGY PATTERN tests.
 *
 * The central point these prove: the SAME raw score produces DIFFERENT
 * outcomes in different strategies. That is what makes them genuine
 * algorithms rather than pass-through wrappers around a number the
 * recruiter typed.
 */
class StrategyTest {

    private Application sampleApplication() {
        Candidate candidate = new Candidate("Demo Candidate", "candidate@demo.com", "cv.pdf");
        Job job = new JobBuilderStub();
        return new Application("app-1", candidate, job);
    }

    /** Minimal Job for testing - avoids depending on the builder here. */
    private static class JobBuilderStub extends Job {
        JobBuilderStub() {
            super("job-1", "Backend Engineer", "TechNova Ltd", JobType.FULL_TIME, "Java APIs");
        }
    }

    @Test
    @DisplayName("A score of 65 PASSES the HR bar but FAILS the technical bar")
    void sameScoreDifferentVerdicts() {
        Application app = sampleApplication();

        EvaluationResult technical = new TechnicalEvaluationStrategy().evaluate(app, 65);
        EvaluationResult hr        = new HrEvaluationStrategy().evaluate(app, 65);

        // Technical passes at 70, HR at 60 - so 65 lands on opposite sides.
        assertFalse(technical.getRequiredDocument().isBlank(),
                "65 is below the technical bar, so evidence is requested");
        assertTrue(hr.getRequiredDocument().isBlank(),
                "65 clears the HR bar, so nothing further is required");
    }

    @Test
    @DisplayName("A high technical score requires no further document")
    void highTechnicalScoreNeedsNothing() {
        EvaluationResult r = new TechnicalEvaluationStrategy().evaluate(sampleApplication(), 90);

        assertEquals(90, r.getScore());
        assertTrue(r.getRequiredDocument().isBlank());
        assertTrue(r.getSummary().contains("Strong"));
    }

    @Test
    @DisplayName("A low technical score requests a take-home exercise")
    void lowTechnicalScoreRequestsExercise() {
        EvaluationResult r = new TechnicalEvaluationStrategy().evaluate(sampleApplication(), 40);

        assertFalse(r.getRequiredDocument().isBlank());
        assertTrue(r.getRequiredDocument().toLowerCase().contains("coding"));
    }

    @Test
    @DisplayName("Each strategy requests DIFFERENT evidence for the same weak score")
    void strategiesRequestDifferentEvidence() {
        Application app = sampleApplication();

        String technicalDoc  = new TechnicalEvaluationStrategy().evaluate(app, 30).getRequiredDocument();
        String hrDoc         = new HrEvaluationStrategy().evaluate(app, 30).getRequiredDocument();
        String behaviouralDoc= new BehavioralEvaluationStrategy().evaluate(app, 30).getRequiredDocument();

        assertNotEquals(technicalDoc, hrDoc);
        assertNotEquals(hrDoc, behaviouralDoc);
        assertTrue(hrDoc.toLowerCase().contains("reference"));
    }

    @Test
    @DisplayName("The score entered by the recruiter is carried into the result")
    void scoreIsCarriedThrough() {
        assertEquals(73, new TechnicalEvaluationStrategy()
                .evaluate(sampleApplication(), 73).getScore());
    }

    @Test
    @DisplayName("The registry resolves the three built-in strategies")
    void registryResolvesBuiltInStrategies() {
        EvaluationStrategyRegistry registry = EvaluationStrategyRegistry.getInstance();

        assertNotNull(registry.get("TECHNICAL"));
        assertNotNull(registry.get("HR"));
        assertNotNull(registry.get("BEHAVIORAL"));
    }

    @Test
    @DisplayName("A blank key means 'no evaluation', not an error")
    void blankKeyMeansNoEvaluation() {
        assertNull(EvaluationStrategyRegistry.getInstance().get(""));
        assertNull(EvaluationStrategyRegistry.getInstance().get(null));
    }

    @Test
    @DisplayName("OPEN/CLOSED: a new metric can be registered at runtime")
    void customMetricCanBeAddedAtRuntime() {
        EvaluationStrategyRegistry registry = EvaluationStrategyRegistry.getInstance();

        registry.register("PORTFOLIO",
                new CustomEvaluationStrategy("Design portfolio review", 70,
                        "A portfolio of three recent projects"));

        EvaluationStrategy added = registry.get("PORTFOLIO");
        assertNotNull(added, "the new metric is immediately available");
        assertEquals("Design portfolio review", added.getName());

        // Below the pass mark -> the document is requested.
        EvaluationResult weak = added.evaluate(sampleApplication(), 55);
        assertTrue(weak.getRequiredDocument().contains("portfolio"));

        // At or above the pass mark -> nothing further.
        EvaluationResult strong = added.evaluate(sampleApplication(), 85);
        assertTrue(strong.getRequiredDocument().isBlank());
    }
}
