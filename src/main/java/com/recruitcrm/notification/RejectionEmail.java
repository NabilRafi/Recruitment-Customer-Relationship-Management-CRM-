package com.recruitcrm.notification;

import com.recruitcrm.domain.Application;

public class RejectionEmail implements EmailTemplate {

    @Override
    public String subject(Application a) {
        return "Update on your application — " + a.getJob().getTitle();
    }

    @Override
    public String body(Application a) {
        return "Dear " + a.getCandidate().getName() + ",\n\n"
             + "Thank you for the time and effort you put into your application for the position of "
             + a.getJob().getTitle() + " at " + a.getJob().getCompanyName() + ".\n\n"
             + "After careful consideration, we have decided not to take your application further "
             + "on this occasion. This was a difficult decision, and it does not reflect on the "
             + "quality of your experience or ability.\n\n"
             + "We would genuinely welcome an application from you for future openings that match "
             + "your background, and your details will remain in our system so that you can apply "
             + "again at any time.\n\n"
             + "We wish you every success in your search.\n\n"
             + "Application reference: " + a.getId() + "\n\n"
             + "Kind regards,\n"
             + a.getJob().getCompanyName() + " Recruitment Team\n"
             + "Sent via Cadre Recruitment CRM";
    }
}
