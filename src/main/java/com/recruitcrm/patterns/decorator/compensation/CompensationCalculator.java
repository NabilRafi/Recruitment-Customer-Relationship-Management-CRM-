package com.recruitcrm.patterns.decorator.compensation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;


public class CompensationCalculator {

    /**
     * Each value wraps the compensation passed in with one more decorator.
     * The Integer argument carries the candidate's evaluation score, which
     * only the performance bonus actually uses.
     */
    private static final Map<String, BiFunction<Compensation, Integer, Compensation>> ENTITLEMENTS
            = new LinkedHashMap<>();

    static {
        // Order of registration = order the chain is built.
        ENTITLEMENTS.put("HOUSING",
                (comp, score) -> new HousingAllowanceDecorator(comp, 40));
        ENTITLEMENTS.put("TRANSPORT",
                (comp, score) -> new TransportAllowanceDecorator(comp, 5_000));
        ENTITLEMENTS.put("REMOTE",
                (comp, score) -> new RemoteWorkStipendDecorator(comp, 3_500));
        ENTITLEMENTS.put("PERFORMANCE",
                (comp, score) -> new PerformanceBonusDecorator(comp, score));
        ENTITLEMENTS.put("FESTIVAL",
                (comp, score) -> new FestivalBonusDecorator(comp, "Eid", 2.0));
    }

    /**
     * Builds the package.
     *
     * @param roleTitle       job title, used in the base salary description
     * @param baseSalary      monthly base pay in BDT
     * @param selectedKeys    comma-separated entitlement keys, e.g. "HOUSING,TRANSPORT"
     * @param evaluationScore the candidate's evaluation score, for the performance bonus
     */
    public static Compensation build(String roleTitle, double baseSalary,
                                     String selectedKeys, int evaluationScore) {

        // Start with the undecorated ConcreteComponent.
        Compensation compensation = new BaseSalary(roleTitle, baseSalary);

        if (selectedKeys == null || selectedKeys.isBlank()) {
            return compensation;   // no entitlements selected - base salary only
        }

        for (String key : selectedKeys.split(",")) {
            BiFunction<Compensation, Integer, Compensation> wrapper
                    = ENTITLEMENTS.get(key.trim().toUpperCase());
            if (wrapper != null) {
                // Reassignment is the chain growing: each decorator wraps
                // everything built so far.
                compensation = wrapper.apply(compensation, evaluationScore);
            }
        }
        return compensation;
    }

    /** Formats the package as an itemised block for the offer letter. */
    public static String formatOffer(Compensation compensation) {
        StringBuilder sb = new StringBuilder();
        for (String line : compensation.getBreakdown()) {
            sb.append("  ").append(line).append("\n");
        }
        sb.append("  ").append("-".repeat(61)).append("\n");
        sb.append(String.format("  %-44s BDT %,12.2f",
                "TOTAL MONTHLY PACKAGE", compensation.getMonthlyAmount()));
        return sb.toString();
    }
}
