package com.recruitcrm.patterns.decorator.compensation;

import java.util.ArrayList;
import java.util.List;

/**
 * DECORATOR PATTERN — the abstract "Decorator".
 *
 * Implements the same Component interface it wraps. That is the
 * structural key: because a decorator IS a Compensation, another
 * decorator can wrap it, and the chain can grow to any depth.
 *
 * The wrapped reference is typed as the INTERFACE, not as BaseSalary,
 * so a decorator never knows whether it is sitting directly on the base
 * salary or on top of four other decorators.
 *
 * addLine() is provided here so every concrete decorator appends to the
 * breakdown the same way: take everything underneath, then add its own
 * entry at the end. This keeps the itemised list in the same order the
 * decorators were applied.
 */
public abstract class CompensationDecorator implements Compensation {

    protected final Compensation wrapped;

    protected CompensationDecorator(Compensation wrapped) {
        this.wrapped = wrapped;
    }

    /** Copies the wrapped breakdown and appends one new line item. */
    protected List<String> addLine(String label, double amount) {
        List<String> lines = new ArrayList<>(wrapped.getBreakdown());
        lines.add(String.format("%-44s BDT %,12.2f", label, amount));
        return lines;
    }
}
