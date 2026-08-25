package com.recruitcrm.patterns.decorator.compensation;

import java.util.List;

/**
 * Concrete Decorator: a flat monthly transport allowance.
 *
 * A FIXED-amount decorator, in contrast to the percentage ones. Its
 * contribution does not depend on what it wraps, so moving it around the
 * chain does not change its own figure - though it does change what any
 * percentage decorator above it calculates from.
 */
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
