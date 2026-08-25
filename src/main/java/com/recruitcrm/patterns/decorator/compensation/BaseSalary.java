package com.recruitcrm.patterns.decorator.compensation;

import java.util.ArrayList;
import java.util.List;


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
