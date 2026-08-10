package com.recruitcrm;

import com.recruitcrm.domain.Application;
import com.recruitcrm.domain.ApplicationStatus;
import com.recruitcrm.domain.Candidate;
import com.recruitcrm.domain.Job;
import com.recruitcrm.domain.JobType;
import com.recruitcrm.domain.UserAccount;
import com.recruitcrm.patterns.decorator.BasicJobPosting;
import com.recruitcrm.patterns.decorator.FeaturedJobDecorator;
import com.recruitcrm.patterns.decorator.JobPostingComponent;
import com.recruitcrm.patterns.decorator.UrgentJobDecorator;
import com.recruitcrm.patterns.facade.RecruitmentFacade;
import com.recruitcrm.patterns.factory.UserAccountFactory;
import com.recruitcrm.patterns.factory.UserAccountFactoryRegistry;
import com.recruitcrm.patterns.observer.AuditLogObserver;
import com.recruitcrm.patterns.observer.EmailNotificationObserver;
import com.recruitcrm.patterns.singleton.DataStore;
import com.recruitcrm.patterns.strategy.TechnicalEvaluationStrategy;

/**
 * Console demo that exercises all six design patterns together, the same
 * way each lecture example ends with a main() that prints output. Use
 * this as your walkthrough script for the viva.
 */
public class Main {
    public static void main(String[] args) {

        System.out.println("=== 1) FACTORY METHOD + REGISTRY (fixes the if-else Factory) ===");
        UserAccountFactoryRegistry registry = UserAccountFactoryRegistry.getInstance();

        UserAccountFactory candidateFactory = registry.getFactory("CANDIDATE");
        UserAccount candidateAccount = candidateFactory.createAccount(
                "Ayesha Rahman", "ayesha@example.com", "ayesha_resume.pdf");

        UserAccountFactory companyFactory = registry.getFactory("COMPANY");
        UserAccount companyAccount = companyFactory.createAccount(
                "TechNova Ltd", "hr@technova.com", "Software");

        System.out.println("Created: " + candidateAccount.describe());
        System.out.println("Created: " + companyAccount.describe());

        System.out.println();
        System.out.println("=== 2) SINGLETON (shared in-memory data store) ===");
        DataStore store = DataStore.getInstance();
        store.saveAccount(candidateAccount.getEmail(), candidateAccount);
        store.saveAccount(companyAccount.getEmail(), companyAccount);
        System.out.println("DataStore.getInstance() called twice, same object? "
                + (store == DataStore.getInstance()));

        System.out.println();
        System.out.println("=== 3) DECORATOR (job posting visibility) ===");
        Job job = new Job("job-1", "Backend Engineer", "TechNova Ltd", JobType.FULL_TIME, "Build APIs in Java.");
        store.saveJob(job);

        JobPostingComponent plainPosting = new BasicJobPosting(job);
        JobPostingComponent featuredUrgentPosting = new UrgentJobDecorator(new FeaturedJobDecorator(plainPosting));

        System.out.println(plainPosting.getDisplayTitle() + " | visibility=" + plainPosting.getVisibilityScore());
        System.out.println(featuredUrgentPosting.getDisplayTitle() + " | visibility=" + featuredUrgentPosting.getVisibilityScore());

        System.out.println();
        System.out.println("=== 4) FACADE + OBSERVER + STRATEGY (submit & evaluate an application) ===");
        RecruitmentFacade facade = new RecruitmentFacade();
        facade.addObserver(new EmailNotificationObserver());
        facade.addObserver(new AuditLogObserver());

        Candidate candidate = (Candidate) candidateAccount;
        Application application = facade.submitApplication(candidate, job);
        System.out.println("Submitted application " + application.getId() + ", status=" + application.getStatus());

        facade.updateStatus(application, ApplicationStatus.SHORTLISTED, new TechnicalEvaluationStrategy());
        System.out.println("Evaluation summary: " + application.getLastEvaluationSummary());
        System.out.println("New status: " + application.getStatus());
    }
}
