package com.recruitcrm.patterns.strategy;

import com.recruitcrm.domain.Application;

/**
 * Concrete Strategy: technical assessment.
 *
 * Uses a high pass bar (70), because technical ability is the primary
 * requirement for engineering roles. A candidate below the bar is asked
 * for a code sample so the decision can be reviewed on real work rather
 * than a single test score.
 */
public class TechnicalEvaluationStrategy implements EvaluationStrategy {

    @Override
    public String getName() {
        return "Technical assessment";
    }

    @Override
    public EvaluationResult evaluate(Application application, int rawScore) {
        String band;
        String document;

        if (rawScore >= 85) {
            band = "Strong technical performance";
            document = "";                                   // nothing further needed
        } else if (rawScore >= 70) {
            band = "Meets the technical bar";
            document = "";
        } else if (rawScore >= 50) {
            band = "Borderline - further evidence required";
            document = "A code sample or portfolio link demonstrating recent work";
        } else {
            band = "Below the technical bar";
            document = "A completed take-home coding exercise";
        }

        return new EvaluationResult(rawScore,
                band + " for " + application.getCandidate().getName(),
                document);
    }
}
