package com.recruitcrm.notification;

import com.recruitcrm.domain.Application;

public class InterviewEmail implements EmailTemplate {

    @Override
    public String subject(Application a) {
        return "Interview invitation — " + a.getJob().getTitle() + " at " + a.getJob().getCompanyName();
    }

    @Override
    public String body(Application a) {
        String details = a.getInterviewDetails() == null || a.getInterviewDetails().isBlank()
                ? "  Our team will contact you shortly with the exact date and time."
                : a.getInterviewDetails();

        return "Dear " + a.getCandidate().getName() + ",\n\n"
             + "We are pleased to invite you to an interview for the position of "
             + a.getJob().getTitle() + " at " + a.getJob().getCompanyName() + ".\n\n"
             + "INTERVIEW DETAILS\n"
             + "-----------------\n"
             + details + "\n\n"
             + "How to prepare:\n"
             + "  - Please arrive or join five minutes early.\n"
             + "  - Bring a copy of your CV and any relevant portfolio work.\n"
             + "  - Be ready to discuss your experience relating to this role.\n\n"
             + "If the proposed time does not suit you, reply to this email and we will "
             + "arrange an alternative.\n\n"
             + "Application reference: " + a.getId() + "\n\n"
             + "Kind regards,\n"
             + a.getJob().getCompanyName() + " Recruitment Team\n"
             + "Sent via Cadre Recruitment CRM";
    }
}
