package com.recruitcrm.patterns.strategy;

import com.recruitcrm.domain.Application;


public interface EvaluationStrategy {
    EvaluationResult evaluate(Application application);
}
