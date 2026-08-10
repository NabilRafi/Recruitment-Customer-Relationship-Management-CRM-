package com.recruitcrm.patterns.factory;

import com.recruitcrm.domain.UserAccount;

/**
 * FACTORY METHOD PATTERN — "Creator" role.
 *
 * Each concrete factory below overrides createAccount() to build exactly
 * ONE type of UserAccount. There is no branching here: which concrete
 * class gets used is decided by WHICH factory object you call, not by
 * an if-else / switch on a type string inside one big method.
 *
 * This is the direct fix for a "Simple Factory" that used if-else (or
 * PHP's match) on a type string to decide what to instantiate.
 */
public interface UserAccountFactory {
    UserAccount createAccount(String name, String email, String extra);
}
