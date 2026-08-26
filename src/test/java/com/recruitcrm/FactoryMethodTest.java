package com.recruitcrm;

import com.recruitcrm.domain.*;
import com.recruitcrm.patterns.factory.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FACTORY METHOD tests.
 *
 * These prove the registry dispatches to the correct ConcreteCreator
 * without any conditional deciding the class, and that a new type can be
 * registered without modifying existing code (Open/Closed).
 */
class FactoryMethodTest {

    @Test
    @DisplayName("The registry returns a factory that builds a Candidate")
    void registryBuildsCandidate() {
        UserAccountFactory factory =
                UserAccountFactoryRegistry.getInstance().getFactory("CANDIDATE");
        UserAccount account = factory.createAccount("Nabil", "n@test.com", "cv.pdf");

        assertInstanceOf(Candidate.class, account);
        assertEquals("CANDIDATE", account.getRole());
        assertEquals("cv.pdf", account.getExtra(), "extra is the resume link for a candidate");
    }

    @Test
    @DisplayName("The registry returns a factory that builds a Recruiter")
    void registryBuildsRecruiter() {
        UserAccount account = UserAccountFactoryRegistry.getInstance()
                .getFactory("RECRUITER")
                .createAccount("Rita", "r@test.com", "TechNova Ltd");

        assertInstanceOf(Recruiter.class, account);
        assertEquals("RECRUITER", account.getRole());
        assertEquals("TechNova Ltd", account.getExtra(), "extra is the company for a recruiter");
    }

    @Test
    @DisplayName("The registry returns a factory that builds a Company")
    void registryBuildsCompany() {
        UserAccount account = UserAccountFactoryRegistry.getInstance()
                .getFactory("COMPANY")
                .createAccount("TechNova", "hr@test.com", "Software");

        assertInstanceOf(Company.class, account);
        assertEquals("COMPANY", account.getRole());
    }

    @Test
    @DisplayName("Type lookup is case-insensitive")
    void lookupIsCaseInsensitive() {
        assertNotNull(UserAccountFactoryRegistry.getInstance().getFactory("candidate"));
        assertNotNull(UserAccountFactoryRegistry.getInstance().getFactory("Candidate"));
    }

    @Test
    @DisplayName("An unknown type is rejected with a clear message")
    void unknownTypeThrows() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> UserAccountFactoryRegistry.getInstance().getFactory("WIZARD"));

        assertTrue(e.getMessage().contains("WIZARD"));
    }

    @Test
    @DisplayName("OPEN/CLOSED: a new account type can be registered without editing existing code")
    void newTypeCanBeRegisteredAtRuntime() {
        // An anonymous ConcreteCreator - no existing class is modified.
        UserAccountFactoryRegistry.getInstance().register("INTERN",
                (name, email, extra) -> new Candidate(name, email, extra));

        UserAccount account = UserAccountFactoryRegistry.getInstance()
                .getFactory("INTERN")
                .createAccount("Test Intern", "i@test.com", "cv.pdf");

        assertNotNull(account);
        assertEquals("Test Intern", account.getName());
    }

    @Test
    @DisplayName("The registry itself is a Singleton")
    void registryIsSingleton() {
        assertSame(UserAccountFactoryRegistry.getInstance(),
                   UserAccountFactoryRegistry.getInstance());
    }
}
