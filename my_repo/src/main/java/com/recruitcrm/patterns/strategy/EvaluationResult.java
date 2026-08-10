package com.recruitcrm.patterns.strategy;

public class EvaluationResult {
    private final int score;
    private final String summary;

    public EvaluationResult(int score, String summary) {
        this.score = score;
        this.summary = summary;
    }

    public int getScore() {
        return score;
    }

    public String getSummary() {
        return summary;
    }

    @Override
    public String toString() {
        return summary + " (score: " + score + "/100)";
    }
}
