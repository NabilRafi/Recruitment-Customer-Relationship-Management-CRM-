package com.recruitcrm.patterns.strategy;

import com.recruitcrm.domain.Application;


public class TechnicalEvaluationStrategy implements EvaluationStrategy {
    @Override
    public EvaluationResult evaluate(Application application) {
        // Simplified scoring for the demo — swap in real criteria later.
        int score = 78;
        return new EvaluationResult(score, "Technical evaluation for " + application.getCandidate().getName());
    }
}
