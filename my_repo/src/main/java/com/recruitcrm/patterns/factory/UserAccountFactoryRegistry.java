package com.recruitcrm.patterns.factory;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry that maps an account-type key to the right factory. This is
 * what replaces if-else/switch: choosing a factory becomes a single map
 * lookup instead of a chain of conditionals deciding which class to build.
 *
 * This class is also a Singleton (private constructor + static
 * getInstance, no parameters) — see patterns.singleton.DataStore for a
 * second, independent Singleton if your instructor wants two clearly
 * separate examples instead of reusing this one.
 */
public class UserAccountFactoryRegistry {
    private static final UserAccountFactoryRegistry INSTANCE = new UserAccountFactoryRegistry();

    private final Map<String, UserAccountFactory> factories = new HashMap<>();

    private UserAccountFactoryRegistry() {
        // Register every known factory ONCE, here, at startup.
        register("CANDIDATE", new CandidateFactory());
        register("RECRUITER", new RecruiterFactory());
        register("COMPANY", new CompanyFactory());
    }

    public static UserAccountFactoryRegistry getInstance() {
        return INSTANCE;
    }

    public void register(String type, UserAccountFactory factory) {
        factories.put(type.toUpperCase(), factory);
    }

    public UserAccountFactory getFactory(String type) {
        UserAccountFactory factory = factories.get(type.toUpperCase());
        if (factory == null) {
            // This is a "did I find it" guard, not a type-dispatch chain —
            // very different from branching to decide WHICH class to build.
            throw new IllegalArgumentException("No factory registered for type: " + type);
        }
        return factory;
    }
}
