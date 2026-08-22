package com.recruitcrm.notification;

import com.recruitcrm.domain.Application;

/**
 * One email template per application status.
 *
 * Same shape as the Strategy pattern: a family of interchangeable
 * implementations behind one interface, chosen at runtime. Here the
 * varying behaviour is "how do we word this message", and the choice is
 * driven by the application's new status.
 */
public interface EmailTemplate {
    String subject(Application application);
    String body(Application application);
}
