package com.recruitcrm.patterns.decorator.compensation;

import java.util.List;


public class FestivalBonusDecorator extends CompensationDecorator {

    private final String occasion;
    private final double monthsPerYear;

    public FestivalBonusDecorator(Compensation wrapped, String occasion, double monthsPerYear) {
        super(wrapped);
        this.occasion = occasion;
        this.monthsPerYear = monthsPerYear;
    }

    private double monthlyEquivalent() {
        return wrapped.getMonthlyAmount() * monthsPerYear / 12.0;
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription() + " + " + occasion + " bonus";
    }

    @Override
    public double getMonthlyAmount() {
        return wrapped.getMonthlyAmount() + monthlyEquivalent();
    }

    @Override
    public List<String> getBreakdown() {
        return addLine(String.format("%s bonus (%.1f month/yr, averaged)", occasion, monthsPerYear),
                monthlyEquivalent());
    }
}
