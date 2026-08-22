package com.recruitcrm.patterns.facade;

import com.recruitcrm.domain.Application;
import com.recruitcrm.domain.ApplicationStatus;
import com.recruitcrm.domain.Candidate;
import com.recruitcrm.domain.Job;
import com.recruitcrm.patterns.observer.ApplicationObserver;
import com.recruitcrm.patterns.observer.ApplicationStatusPublisher;
import com.recruitcrm.patterns.singleton.DataStore;
import com.recruitcrm.patterns.strategy.EvaluationResult;
import com.recruitcrm.patterns.strategy.EvaluationStrategy;

import java.util.UUID;


public class RecruitmentFacade {
    private final DataStore dataStore = DataStore.getInstance();
    private final ApplicationStatusPublisher publisher = new ApplicationStatusPublisher();

    public void addObserver(ApplicationObserver observer) {
        publisher.registerObserver(observer);
    }

    public Application submitApplication(Candidate candidate, Job job) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Application application = new Application(id, candidate, job);
        dataStore.saveApplication(application);
        publisher.notifyObservers(application);
        return application;
    }

    public void updateStatus(Application application, ApplicationStatus newStatus, EvaluationStrategy strategy) {
        if (strategy != null) {
            EvaluationResult result = strategy.evaluate(application);
            application.setLastEvaluationSummary(result.toString());
        }
        application.setStatus(newStatus);
        dataStore.saveApplication(application); // re-persist - a field mutation alone doesn't trigger a save
        publisher.notifyObservers(application);
    }
}
