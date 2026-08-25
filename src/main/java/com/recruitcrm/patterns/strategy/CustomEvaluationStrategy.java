package com.recruitcrm.patterns.strategy;

import com.recruitcrm.domain.Application;

/**
 * Concrete Strategy created at runtime for a recruiter-defined metric.
 *
 * When a recruiter adds a new assessment type (for example "Design
 * portfolio review" or "Language proficiency"), one of these is built
 * and registered. No new class has to be written or compiled - which is
 * the extensibility the Strategy pattern is supposed to provide.
 */
public class CustomEvaluationStrategy implements EvaluationStrategy {

    private final String name;
    private final int passMark;
    private final String documentIfBelowPass;

    public CustomEvaluationStrategy(String name, int passMark, String documentIfBelowPass) {
        this.name = name;
        this.passMark = passMark;
        this.documentIfBelowPass = documentIfBelowPass == null ? "" : documentIfBelowPass;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public EvaluationResult evaluate(Application application, int rawScore) {
        boolean passed = rawScore >= passMark;
        String band = passed
                ? name + ": met the required standard"
                : name + ": below the required standard of " + passMark;

        return new EvaluationResult(rawScore,
                band + " (" + application.getCandidate().getName() + ")",
                passed ? "" : documentIfBelowPass);
    }
}
