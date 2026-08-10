package com.recruitcrm.patterns.observer;

import com.recruitcrm.domain.Application;

/** Concrete Observer: pretends to email the candidate about the status change. */
public class EmailNotificationObserver implements ApplicationObserver {
    @Override
    public void onStatusChanged(Application application) {
        System.out.println("[EMAIL] To " + application.getCandidate().getEmail()
                + ": your application for '" + application.getJob().getTitle()
                + "' is now " + application.getStatus());
    }
}
