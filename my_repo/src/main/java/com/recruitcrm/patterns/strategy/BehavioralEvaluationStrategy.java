package com.recruitcrm.patterns.strategy;

import com.recruitcrm.domain.Application;

/** Concrete Strategy: scores a candidate on behavioral criteria. */
public class BehavioralEvaluationStrategy implements EvaluationStrategy {
    @Override
    public EvaluationResult evaluate(Application application) {
        int score = 90;
        return new EvaluationResult(score, "Behavioral evaluation for " + application.getCandidate().getName());
    }
}
