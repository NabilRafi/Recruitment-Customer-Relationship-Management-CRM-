package com.recruitcrm;

import com.recruitcrm.patterns.factory.UserAccountFactoryRegistry;
import com.recruitcrm.patterns.singleton.DataStore;
import com.recruitcrm.patterns.strategy.EvaluationStrategyRegistry;
import com.recruitcrm.notification.EmailTemplateRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SINGLETON PATTERN tests.
 *
 * NOTE: the DataStore tests touch the real SQLite file, because
 * DataStore's constructor opens the database. They will create
 * data/crm.db if it does not exist. The registry tests are pure.
 */
class SingletonTest {

    @Test
    @DisplayName("DataStore.getInstance() always returns the SAME object")
    void dataStoreIsOneInstance() {
        // assertSame is reference identity (==), not .equals()
        assertSame(DataStore.getInstance(), DataStore.getInstance());
    }

    @Test
    @DisplayName("DataStore's constructor is private, so 'new' is impossible")
    void dataStoreConstructorIsPrivate() {
        Constructor<?>[] constructors = DataStore.class.getDeclaredConstructors();

        assertEquals(1, constructors.length, "exactly one constructor");
        assertTrue(Modifier.isPrivate(constructors[0].getModifiers()),
                "the private constructor is the mechanism that enforces the pattern");
    }

    @Test
    @DisplayName("DataStore is final, so the private constructor cannot be circumvented")
    void dataStoreIsFinal() {
        assertTrue(Modifier.isFinal(DataStore.class.getModifiers()));
    }

    @Test
    @DisplayName("The constructor takes NO parameters - a parameterised one would make it a factory")
    void constructorTakesNoParameters() {
        Constructor<?> constructor = DataStore.class.getDeclaredConstructors()[0];

        assertEquals(0, constructor.getParameterCount(),
                "the lecture notes that a Singleton accepting parameters becomes a factory");
    }

    @Test
    @DisplayName("The factory registry is a Singleton too (eager initialisation)")
    void factoryRegistryIsSingleton() {
        assertSame(UserAccountFactoryRegistry.getInstance(),
                   UserAccountFactoryRegistry.getInstance());
    }

    @Test
    @DisplayName("The strategy registry is a Singleton")
    void strategyRegistryIsSingleton() {
        assertSame(EvaluationStrategyRegistry.getInstance(),
                   EvaluationStrategyRegistry.getInstance());
    }

    @Test
    @DisplayName("The email template registry is a Singleton")
    void templateRegistryIsSingleton() {
        assertSame(EmailTemplateRegistry.getInstance(),
                   EmailTemplateRegistry.getInstance());
    }

    @Test
    @DisplayName("State written through one reference is visible through another")
    void stateIsGenuinelyShared() {
        // If these were two objects, the second lookup would return null.
        UserAccountFactoryRegistry.getInstance().register("TEMP_TYPE",
                (name, email, extra) -> new com.recruitcrm.domain.Candidate(name, email, extra));

        assertNotNull(UserAccountFactoryRegistry.getInstance().getFactory("TEMP_TYPE"),
                "the same shared instance holds the registration");
    }
}
