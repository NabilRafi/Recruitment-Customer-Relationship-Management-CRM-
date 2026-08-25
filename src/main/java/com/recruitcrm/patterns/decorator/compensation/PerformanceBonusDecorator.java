package com.recruitcrm.patterns.decorator.compensation;

import java.util.List;

/**
 * Concrete Decorator: a performance bonus earned on the candidate's
 * evaluation score.
 *
 * This is a PERCENTAGE decorator, and that matters: it calculates its
 * contribution from whatever is underneath it in the chain, so the ORDER
 * in which decorators are applied changes the final figure. See
 * CompensationCalculator for a worked example.
 *
 * A candidate scoring 78 in their technical evaluation earns a bonus of
 * 78/10 = 7.8% of everything below this decorator.
 */
public class PerformanceBonusDecorator extends CompensationDecorator {

    private final int evaluationScore;

    public PerformanceBonusDecorator(Compensation wrapped, int evaluationScore) {
        super(wrapped);
        this.evaluationScore = evaluationScore;
    }

    /** Score out of 100 converts to a percentage rate: 78 -> 7.8%. */
    private double rate() {
        return evaluationScore / 1000.0;
    }

    private double bonusAmount() {
        return wrapped.getMonthlyAmount() * rate();
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription() + " + performance bonus";
    }

    @Override
    public double getMonthlyAmount() {
        return wrapped.getMonthlyAmount() + bonusAmount();
    }

    @Override
    public List<String> getBreakdown() {
        return addLine(String.format("Performance bonus (%.1f%%, score %d)",
                rate() * 100, evaluationScore), bonusAmount());
    }
}
