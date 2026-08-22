package com.recruitcrm.patterns.observer;

import com.recruitcrm.domain.Application;


public class AuditLogObserver implements ApplicationObserver {
    @Override
    public void onStatusChanged(Application application) {
        System.out.println("[AUDIT LOG] Application " + application.getId()
                + " -> " + application.getStatus());
    }
}
