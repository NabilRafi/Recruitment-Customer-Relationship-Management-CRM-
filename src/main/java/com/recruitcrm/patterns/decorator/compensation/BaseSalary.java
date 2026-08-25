package com.recruitcrm.patterns.decorator.compensation;

import java.util.ArrayList;
import java.util.List;

/**
 * DECORATOR PATTERN — the "ConcreteComponent".
 *
 * The undecorated object: the salary written on the job posting, with no
 * allowances or bonuses attached. Every decorator chain starts here.
 *
 * It knows nothing about bonuses, allowances or festivals. That is the
 * point - new entitlements can be introduced without this class ever
 * changing.
 */
public class BaseSalary implements Compensation {

    private final double monthlyAmount;
    private final String roleTitle;

    public BaseSalary(String roleTitle, double monthlyAmount) {
        this.roleTitle = roleTitle;
        this.monthlyAmount = monthlyAmount;
    }

    @Override
    public String getDescription() {
        return "Base salary for " + roleTitle;
    }

    @Override
    public double getMonthlyAmount() {
        return monthlyAmount;
    }

    @Override
    public List<String> getBreakdown() {
        List<String> lines = new ArrayList<>();
        lines.add(String.format("%-44s BDT %,12.2f", "Base salary", monthlyAmount));
        return lines;
    }
}
