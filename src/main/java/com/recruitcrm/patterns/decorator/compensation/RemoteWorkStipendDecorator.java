package com.recruitcrm.patterns.decorator.compensation;

import java.util.List;

/**
 * Concrete Decorator: a flat stipend covering internet and home-office
 * costs for remote or hybrid roles.
 */
public class RemoteWorkStipendDecorator extends CompensationDecorator {

    private final double amount;

    public RemoteWorkStipendDecorator(Compensation wrapped, double amount) {
        super(wrapped);
        this.amount = amount;
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription() + " + remote work stipend";
    }

    @Override
    public double getMonthlyAmount() {
        return wrapped.getMonthlyAmount() + amount;
    }

    @Override
    public List<String> getBreakdown() {
        return addLine("Remote work stipend", amount);
    }
}
