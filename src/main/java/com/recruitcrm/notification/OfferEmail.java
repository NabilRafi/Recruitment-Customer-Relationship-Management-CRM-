package com.recruitcrm.notification;

import com.recruitcrm.domain.Application;

public class OfferEmail implements EmailTemplate {

    @Override
    public String subject(Application a) {
        return "Offer of employment — " + a.getJob().getTitle() + " at " + a.getJob().getCompanyName();
    }

    @Override
    public String body(Application a) {
        String salary = a.getJob().getSalaryRange() == null || a.getJob().getSalaryRange().isBlank()
                ? "To be confirmed in your formal contract"
                : a.getJob().getSalaryRange();
        String location = a.getJob().getLocation() == null || a.getJob().getLocation().isBlank()
                ? "To be confirmed"
                : a.getJob().getLocation();

        return "Dear " + a.getCandidate().getName() + ",\n\n"
             + "Congratulations. Following your interview, we are delighted to offer you the "
             + "position of " + a.getJob().getTitle() + " at " + a.getJob().getCompanyName() + ".\n\n"
             + "OFFER SUMMARY\n"
             + "-------------\n"
             + "  Position:      " + a.getJob().getTitle() + "\n"
             + "  Employer:      " + a.getJob().getCompanyName() + "\n"
             + "  Employment:    " + a.getJob().getType().name().replace("_", " ") + "\n"
             + "  Location:      " + location + "\n"
             + "  Compensation:  " + salary + "\n\n"
             + "Next steps:\n"
             + "  1. Reply to this email to confirm whether you accept.\n"
             + "  2. On acceptance we will send your formal contract for signature.\n"
             + "  3. Our HR team will then arrange your start date and onboarding.\n\n"
             + "Please note this email is a preliminary offer. Full terms, benefits and "
             + "conditions of employment will be set out in your written contract.\n\n"
             + "Application reference: " + a.getId() + "\n\n"
             + "Kind regards,\n"
             + a.getJob().getCompanyName() + " Recruitment Team\n"
             + "Sent via Fieldnote Recruitment CRM";
    }
}
