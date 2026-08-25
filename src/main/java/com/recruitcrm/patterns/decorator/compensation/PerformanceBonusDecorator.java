package com.recruitcrm.patterns.decorator.compensation;

import java.util.List;


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
