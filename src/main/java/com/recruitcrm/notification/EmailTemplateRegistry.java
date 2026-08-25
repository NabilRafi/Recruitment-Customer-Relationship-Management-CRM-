package com.recruitcrm.notification;

import com.recruitcrm.domain.ApplicationStatus;

import java.util.HashMap;
import java.util.Map;


public class EmailTemplateRegistry {

    private static final EmailTemplateRegistry INSTANCE = new EmailTemplateRegistry();

    private final Map<ApplicationStatus, EmailTemplate> templates = new HashMap<>();

    private EmailTemplateRegistry() {
        register(ApplicationStatus.APPLIED, new AppliedEmail());
        register(ApplicationStatus.SHORTLISTED, new ShortlistedEmail());
        register(ApplicationStatus.INTERVIEW_SCHEDULED, new InterviewEmail());
        register(ApplicationStatus.HIRED, new OfferEmail());
        register(ApplicationStatus.REJECTED, new RejectionEmail());
    }

    public static EmailTemplateRegistry getInstance() {
        return INSTANCE;
    }

    public void register(ApplicationStatus status, EmailTemplate template) {
        templates.put(status, template);
    }

    public EmailTemplate getTemplate(ApplicationStatus status) {
        return templates.get(status);
    }
}
