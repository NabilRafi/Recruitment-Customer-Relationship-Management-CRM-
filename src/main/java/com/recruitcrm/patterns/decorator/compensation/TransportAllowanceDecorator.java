package com.recruitcrm.patterns.decorator.compensation;

import java.util.List;


public class TransportAllowanceDecorator extends CompensationDecorator {

    private final double amount;

    public TransportAllowanceDecorator(Compensation wrapped, double amount) {
        super(wrapped);
        this.amount = amount;
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription() + " + transport allowance";
    }

    @Override
    public double getMonthlyAmount() {
        return wrapped.getMonthlyAmount() + amount;
    }

    @Override
    public List<String> getBreakdown() {
        return addLine("Transport allowance", amount);
    }
}
