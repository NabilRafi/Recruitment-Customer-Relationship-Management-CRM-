package com.recruitcrm.patterns.strategy;

import com.recruitcrm.domain.Application;

/** Concrete Strategy: scores a candidate on HR / culture-fit criteria. */
public class HrEvaluationStrategy implements EvaluationStrategy {
    @Override
    public EvaluationResult evaluate(Application application) {
        int score = 85;
        return new EvaluationResult(score, "HR evaluation for " + application.getCandidate().getName());
    }
}
