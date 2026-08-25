package com.recruitcrm.patterns.decorator.compensation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Assembles a decorator chain from the entitlements a recruiter selects.
 *
 * Each entry in ENTITLEMENTS is a function that takes the compensation
 * built so far and returns it wrapped in one more decorator. Selecting
 * entitlements therefore becomes a fold over that map - no if-else or
 * switch decides which decorator class to construct, matching the same
 * registry approach used by UserAccountFactoryRegistry.
 *
 * WHY ORDER MATTERS (a good viva point)
 * -------------------------------------
 * Percentage decorators calculate from whatever sits beneath them, so the
 * sequence changes the total. With a base of 50,000:
 *
 *   base -> +10% housing -> +5,000 transport
 *       = 50,000 + 5,000 + 5,000            = 60,000
 *
 *   base -> +5,000 transport -> +10% housing
 *       = 50,000 + 5,000 + 5,500            = 60,500
 *
 * Same components, different order, different figure. A LinkedHashMap is
 * used precisely because it preserves insertion order, so the chain is
 * always built in a defined, repeatable sequence.
 */
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

        for (String entry : selectedKeys.split(",")) {
            if (entry.isBlank()) continue;

            // An entry is either "KEY" (fixed-rule entitlement) or
            // "BONUS:<reason>:<amount>" (a discretionary merit bonus).
            if (entry.trim().toUpperCase().startsWith("BONUS:")) {
                String[] parts = entry.split(":", 3);
                if (parts.length == 3) {
                    try {
                        double amount = Double.parseDouble(parts[2].trim());
                        compensation = new MeritBonusDecorator(compensation, parts[1].trim(), amount);
                    } catch (NumberFormatException e) {
                        // Unparseable amount - skip this bonus rather than fail the offer.
                    }
                }
                continue;
            }

            BiFunction<Compensation, Integer, Compensation> wrapper
                    = ENTITLEMENTS.get(entry.trim().toUpperCase());
            if (wrapper != null) {
                // Reassignment is the chain growing: each decorator wraps
                // everything built so far.
                compensation = wrapper.apply(compensation, evaluationScore);
            }
        }
        return compensation;
    }

    /** True when the package contains anything beyond base salary. */
    public static boolean hasBonus(String selectedKeys) {
        return selectedKeys != null && !selectedKeys.isBlank();
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
