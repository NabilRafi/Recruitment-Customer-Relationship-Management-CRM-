package com.recruitcrm.patterns.strategy;

import com.recruitcrm.domain.Application;


public class BehavioralEvaluationStrategy implements EvaluationStrategy {
    @Override
    public EvaluationResult evaluate(Application application) {
        int score = 90;
        return new EvaluationResult(score, "Behavioral evaluation for " + application.getCandidate().getName());
    }
}
