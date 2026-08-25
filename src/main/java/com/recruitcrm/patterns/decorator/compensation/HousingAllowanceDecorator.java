package com.recruitcrm.patterns.decorator.compensation;

import java.util.List;


public class HousingAllowanceDecorator extends CompensationDecorator {

    private final double percentage;

    public HousingAllowanceDecorator(Compensation wrapped, double percentage) {
        super(wrapped);
        this.percentage = percentage;
    }

    private double allowance() {
        return wrapped.getMonthlyAmount() * (percentage / 100.0);
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription() + " + housing allowance";
    }

    @Override
    public double getMonthlyAmount() {
        return wrapped.getMonthlyAmount() + allowance();
    }

    @Override
    public List<String> getBreakdown() {
        return addLine(String.format("House rent allowance (%.0f%%)", percentage), allowance());
    }
}
