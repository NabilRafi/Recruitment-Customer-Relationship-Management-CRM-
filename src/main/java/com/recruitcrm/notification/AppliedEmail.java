package com.recruitcrm.notification;

import com.recruitcrm.domain.Application;

public class AppliedEmail implements EmailTemplate {

    @Override
    public String subject(Application a) {
        return "Application received — " + a.getJob().getTitle() + " at " + a.getJob().getCompanyName();
    }

    @Override
    public String body(Application a) {
        return "Dear " + a.getCandidate().getName() + ",\n\n"
             + "Thank you for applying for the position of " + a.getJob().getTitle()
             + " at " + a.getJob().getCompanyName() + ".\n\n"
             + "We have received your application and our recruitment team will review it shortly. "
             + "You will be notified by email at each stage of the process.\n\n"
             + "Application reference: " + a.getId() + "\n\n"
             + "Kind regards,\n"
             + a.getJob().getCompanyName() + " Recruitment Team\n"
             + "Sent via Fieldnote Recruitment CRM";
    }
}
