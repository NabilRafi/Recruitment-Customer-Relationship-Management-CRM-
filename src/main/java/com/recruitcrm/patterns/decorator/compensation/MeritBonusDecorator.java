package com.recruitcrm.patterns.decorator.compensation;

import java.util.List;

/**
 * Concrete Decorator: a discretionary bonus awarded for a stated reason.
 *
 * Unlike the fixed-rule decorators (housing at 40%, transport at 5,000),
 * this one carries BOTH an amount and the justification the recruiter
 * selected - "exceptional technical assessment", "strong cultural fit",
 * and so on. The reason appears in the candidate's offer letter, so the
 * candidate can see what the bonus was awarded for.
 *
 * Several of these can be stacked, each with its own reason, which is
 * exactly the composability the Decorator pattern exists to provide.
 */
public class MeritBonusDecorator extends CompensationDecorator {

    private final String reason;
    private final double amount;

    public MeritBonusDecorator(Compensation wrapped, String reason, double amount) {
        super(wrapped);
        this.reason = reason;
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription() + " + bonus (" + reason + ")";
    }

    @Override
    public double getMonthlyAmount() {
        return wrapped.getMonthlyAmount() + amount;
    }

    @Override
    public List<String> getBreakdown() {
        return addLine("Bonus - " + reason, amount);
    }
}
