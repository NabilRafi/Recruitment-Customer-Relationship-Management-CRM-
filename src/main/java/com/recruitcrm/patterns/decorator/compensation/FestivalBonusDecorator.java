package com.recruitcrm.patterns.decorator.compensation;

import java.util.List;

/**
 * Concrete Decorator: a festival bonus for a special yearly occasion
 * (Eid, Pohela Boishakh, a year-end bonus, and so on).
 *
 * Expressed as a fraction of one month's pay and spread across the year,
 * so it can be shown alongside the monthly figures. A full month's bonus
 * paid once a year is 1/12 of monthly pay when averaged.
 */
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
