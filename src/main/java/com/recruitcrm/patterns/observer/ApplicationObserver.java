package com.recruitcrm.patterns.observer;

import com.recruitcrm.domain.Application;

/** OBSERVER PATTERN — the "Observer" role. */
public interface ApplicationObserver {
    void onStatusChanged(Application application);
}
