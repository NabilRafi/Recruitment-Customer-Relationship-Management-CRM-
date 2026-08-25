package com.recruitcrm.patterns.strategy;

import com.recruitcrm.domain.Application;

/**
 * Concrete Strategy: behavioural assessment.
 *
 * Asks for a written situational response when the score is mid-range,
 * because behaviour is better evidenced in writing than by a number.
 */
public class BehavioralEvaluationStrategy implements EvaluationStrategy {

    @Override
    public String getName() {
        return "Behavioural assessment";
    }

    @Override
    public EvaluationResult evaluate(Application application, int rawScore) {
        String band;
        String document;

        if (rawScore >= 75) {
            band = "Strong behavioural indicators";
            document = "";
        } else if (rawScore >= 55) {
            band = "Adequate - written follow-up requested";
            document = "A written response (300 words) describing how you handled a "
                     + "difficult situation with a colleague or client";
        } else {
            band = "Behavioural concerns raised";
            document = "A written response (500 words) plus a reference who can speak "
                     + "to your teamwork";
        }

        return new EvaluationResult(rawScore,
                band + " for " + application.getCandidate().getName(),
                document);
    }
}
