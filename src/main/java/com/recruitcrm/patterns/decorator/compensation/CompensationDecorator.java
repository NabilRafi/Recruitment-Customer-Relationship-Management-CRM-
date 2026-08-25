package com.recruitcrm.patterns.decorator.compensation;

import java.util.ArrayList;
import java.util.List;


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
