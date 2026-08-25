package com.recruitcrm.notification;

import com.recruitcrm.domain.Application;
import com.recruitcrm.patterns.decorator.compensation.Compensation;
import com.recruitcrm.patterns.decorator.compensation.CompensationCalculator;

public class OfferEmail implements EmailTemplate {

    @Override
    public String subject(Application a) {
        return "Offer of employment — " + a.getJob().getTitle() + " at " + a.getJob().getCompanyName();
    }

    @Override
    public String body(Application a) {
        // DECORATOR PATTERN: the compensation package is assembled by
        // wrapping a BaseSalary in one decorator per entitlement the
        // recruiter selected. Each wrapper adds a real monetary amount and
        // its own line to the itemised breakdown below.
        Compensation offerPackage = CompensationCalculator.build(
                a.getJob().getTitle(),
                a.getJob().getBaseSalary(),
                a.getOfferEntitlements(),
                a.getEvaluationScore());

        String compensationBlock = a.getJob().getBaseSalary() > 0
                ? CompensationCalculator.formatOffer(offerPackage)
                : "  To be confirmed in your formal contract";

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
             + "\n"
             + "COMPENSATION PACKAGE\n"
             + "--------------------\n"
             + compensationBlock + "\n\n"
             + "Next steps:\n"
             + "  1. Reply to this email to confirm whether you accept.\n"
             + "  2. On acceptance we will send your formal contract for signature.\n"
             + "  3. Our HR team will then arrange your start date and onboarding.\n\n"
             + "Please note this email is a preliminary offer. Full terms, benefits and "
             + "conditions of employment will be set out in your written contract.\n\n"
             + "Application reference: " + a.getId() + "\n\n"
             + "Kind regards,\n"
             + a.getJob().getCompanyName() + " Recruitment Team\n"
             + "Sent via Cadre Recruitment CRM";
    }
}