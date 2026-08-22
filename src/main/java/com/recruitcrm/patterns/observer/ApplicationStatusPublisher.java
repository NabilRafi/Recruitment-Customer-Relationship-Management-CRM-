package com.recruitcrm.patterns.observer;

import com.recruitcrm.domain.Application;

import java.util.ArrayList;
import java.util.List;


public class ApplicationStatusPublisher {
    private final List<ApplicationObserver> observers = new ArrayList<>();

    public void registerObserver(ApplicationObserver observer) {
        observers.add(observer);
    }

    public void unregisterObserver(ApplicationObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(Application application) {
        for (ApplicationObserver observer : observers) {
            observer.onStatusChanged(application);
        }
    }
}
