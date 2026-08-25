package com.recruitcrm.patterns.strategy;

import com.recruitcrm.domain.Application;

/**
 * STRATEGY PATTERN.
 *
 * A family of interchangeable evaluation algorithms. The caller
 * (RecruitmentFacade) decides WHICH strategy to run; this interface does
 * not care which one - that is the point of Strategy.
 *
 * The raw score is now supplied by the recruiter rather than hardcoded
 * inside each strategy. Note this does NOT reduce the strategies to
 * pass-through wrappers: each one still owns the real logic, deciding
 * how to BAND the score, what feedback wording to produce, and what
 * supporting document a candidate at that level must supply. Two
 * strategies given the same raw score produce different outcomes.
 */
public interface EvaluationStrategy {

    /** Display name, shown in the recruiter's dropdown. */
    String getName();

    /**
     * @param application the application under review
     * @param rawScore    the score the recruiter entered, 0-100
     */
    EvaluationResult evaluate(Application application, int rawScore);
}
