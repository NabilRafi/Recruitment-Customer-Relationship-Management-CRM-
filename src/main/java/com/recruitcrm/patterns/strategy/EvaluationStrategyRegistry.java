package com.recruitcrm.patterns.strategy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds every available evaluation strategy, keyed by an identifier.
 *
 * Selecting a strategy is a map lookup, not an if-else chain - the same
 * approach used by UserAccountFactoryRegistry, applied consistently.
 *
 * register() is public so a recruiter can add a new assessment type at
 * runtime through the UI. That is the Open/Closed Principle working in
 * practice: the system gains a new evaluation metric without a single
 * existing class being modified or recompiled.
 */
public class EvaluationStrategyRegistry {

    private static final EvaluationStrategyRegistry INSTANCE = new EvaluationStrategyRegistry();

    private final Map<String, EvaluationStrategy> strategies = new LinkedHashMap<>();

    private EvaluationStrategyRegistry() {
        register("TECHNICAL", new TechnicalEvaluationStrategy());
        register("HR", new HrEvaluationStrategy());
        register("BEHAVIORAL", new BehavioralEvaluationStrategy());
    }

    public static EvaluationStrategyRegistry getInstance() {
        return INSTANCE;
    }

    public synchronized void register(String key, EvaluationStrategy strategy) {
        strategies.put(key.trim().toUpperCase(), strategy);
    }

    /** Returns null when the key is unknown, meaning "no evaluation this time". */
    public EvaluationStrategy get(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return strategies.get(key.trim().toUpperCase());
    }

    public boolean exists(String key) {
        return key != null && strategies.containsKey(key.trim().toUpperCase());
    }

    /** Keys and display names, for populating the recruiter's dropdown. */
    public List<String[]> listForDisplay() {
        List<String[]> out = new ArrayList<>();
        for (Map.Entry<String, EvaluationStrategy> e : strategies.entrySet()) {
            out.add(new String[]{ e.getKey(), e.getValue().getName() });
        }
        return out;
    }
}
