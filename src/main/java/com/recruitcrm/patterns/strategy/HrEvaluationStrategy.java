package com.recruitcrm.patterns.strategy;

import com.recruitcrm.domain.Application;

/**
 * Concrete Strategy: HR and cultural fit.
 *
 * A lower pass bar (60) than the technical assessment, because cultural
 * fit is judged more subjectively and a single score carries less weight.
 * Weaker candidates are asked for references rather than a test.
 */
public class HrEvaluationStrategy implements EvaluationStrategy {

    @Override
    public String getName() {
        return "HR / cultural fit";
    }

    @Override
    public EvaluationResult evaluate(Application application, int rawScore) {
        String band;
        String document;

        if (rawScore >= 80) {
            band = "Excellent cultural alignment";
            document = "";
        } else if (rawScore >= 60) {
            band = "Good cultural fit";
            document = "";
        } else {
            band = "Cultural fit unclear - references requested";
            document = "Two professional reference letters from previous employers";
        }

        return new EvaluationResult(rawScore,
                band + " for " + application.getCandidate().getName(),
                document);
    }
}
