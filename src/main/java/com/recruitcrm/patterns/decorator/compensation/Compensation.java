package com.recruitcrm.patterns.decorator.compensation;

import java.util.List;

/**
 * DECORATOR PATTERN — the "Component" interface.
 *
 * Represents a compensation package that can be built up from a base
 * salary plus any number of additional entitlements.
 *
 * Note what the three methods have in common: every decorator must
 * contribute to ALL of them. Adding a housing allowance changes the
 * description, changes the monthly total, AND adds a line to the
 * breakdown. This is what makes it a genuine Decorator rather than a
 * cosmetic label - each wrapper adds real responsibility to the object,
 * not just an annotation.
 */
public interface Compensation {

    /** Human-readable summary, accumulated as each decorator is applied. */
    String getDescription();

    /** The total monthly figure in BDT, accumulated by each decorator. */
    double getMonthlyAmount();

    /** Itemised list, one entry per component, for the offer letter. */
    List<String> getBreakdown();
}
