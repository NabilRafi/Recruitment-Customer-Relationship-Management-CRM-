package com.recruitcrm.notification;

import com.recruitcrm.domain.Application;

public class ShortlistedEmail implements EmailTemplate {

    @Override
    public String subject(Application a) {
        return "You have been shortlisted — " + a.getJob().getTitle();
    }

    @Override
    public String body(Application a) {
        String evaluation = a.getLastEvaluationSummary() == null || a.getLastEvaluationSummary().isBlank()
                ? ""
                : "Assessment summary: " + a.getLastEvaluationSummary() + "\n\n";

        return "Dear " + a.getCandidate().getName() + ",\n\n"
             + "Good news. Following our initial review, you have been SHORTLISTED for the position of "
             + a.getJob().getTitle() + " at " + a.getJob().getCompanyName() + ".\n\n"
             + evaluation
             + "What happens next:\n"
             + "  - Our team will be in touch to arrange an interview.\n"
             + "  - Please keep an eye on this inbox, including your spam folder.\n"
             + "  - You can review your application status at any time in the Cadre portal.\n\n"
             + "Application reference: " + a.getId() + "\n\n"
             + "Kind regards,\n"
             + a.getJob().getCompanyName() + " Recruitment Team\n"
             + "Sent via Cadre Recruitment CRM";
    }
}
