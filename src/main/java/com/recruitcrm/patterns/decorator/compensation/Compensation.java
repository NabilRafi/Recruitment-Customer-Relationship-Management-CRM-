package com.recruitcrm.patterns.decorator.compensation;

import java.util.List;


public interface Compensation {

    /** Human-readable summary, accumulated as each decorator is applied. */
    String getDescription();

    /** The total monthly figure in BDT, accumulated by each decorator. */
    double getMonthlyAmount();

    /** Itemised list, one entry per component, for the offer letter. */
    List<String> getBreakdown();
}
