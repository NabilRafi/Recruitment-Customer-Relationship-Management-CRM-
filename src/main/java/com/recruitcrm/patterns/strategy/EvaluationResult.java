package com.recruitcrm.patterns.strategy;

/**
 * The outcome of running one evaluation strategy.
 *
 * Carries three things: the numeric score, a human-readable summary, and
 * the document the candidate must supply next. That last field is what
 * lets different assessment types demand different evidence.
 */
public class EvaluationResult {

    private final int score;
    private final String summary;
    private final String requiredDocument;

    public EvaluationResult(int score, String summary, String requiredDocument) {
        this.score = score;
        this.summary = summary;
        this.requiredDocument = requiredDocument == null ? "" : requiredDocument;
    }

    public int getScore() {
        return score;
    }

    public String getSummary() {
        return summary;
    }

    /** Empty when nothing further is required from the candidate. */
    public String getRequiredDocument() {
        return requiredDocument;
    }

    @Override
    public String toString() {
        return summary + " (score: " + score + "/100)";
    }
}
