package com.recruitcrm.patterns.observer;

import com.recruitcrm.domain.Application;

import java.util.ArrayList;
import java.util.List;

/**
 * OBSERVER PATTERN — the "Subject" role.
 *
 * Keeps a list of observers and notifies all of them whenever an
 * application's status changes. It doesn't know or care what each
 * observer actually does (send an email, write a log, etc.) — that
 * decoupling is the whole point of Observer.
 */
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
