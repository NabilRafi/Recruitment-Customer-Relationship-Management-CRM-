package com.recruitcrm.patterns.observer;

import com.recruitcrm.domain.Application;


public interface ApplicationObserver {
    void onStatusChanged(Application application);
}
