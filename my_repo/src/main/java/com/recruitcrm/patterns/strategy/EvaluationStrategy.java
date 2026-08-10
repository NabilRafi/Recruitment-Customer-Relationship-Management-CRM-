package com.recruitcrm.patterns.strategy;

import com.recruitcrm.domain.Application;

/**
 * STRATEGY PATTERN.
 *
 * Defines a family of interchangeable evaluation algorithms. The caller
 * (RecruitmentFacade) decides WHICH strategy to run at the call site;
 * this interface doesn't care which one — that's the point of Strategy.
 */
public interface EvaluationStrategy {
    EvaluationResult evaluate(Application application);
}
