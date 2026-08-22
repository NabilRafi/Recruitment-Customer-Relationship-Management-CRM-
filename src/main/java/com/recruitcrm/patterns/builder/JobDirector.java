package com.recruitcrm.patterns.builder;

import com.recruitcrm.domain.Job;
import com.recruitcrm.domain.JobType;

/**
 * BUILDER PATTERN — the "Director" role.
 *
 * The Director knows recipes: the exact sequence of builder calls needed
 * to produce a common, standard kind of Job. Callers who want one of
 * these standard shapes ask the Director instead of remembering the
 * steps themselves.
 *
 * The Director never calls "new Job(...)" — it only drives a Builder.
 * That separation is the point of the pattern: the Director decides
 * WHAT sequence of steps to run, the Builder knows HOW to perform each
 * step and how to assemble the final object.
 *
 * Callers who need something non-standard can still use JobBuilder
 * directly and skip the Director entirely.
 */
public class JobDirector {

    /**
     * A standard internship: fixed type, short deadline, unpaid unless
     * stated, and never marked Featured by default.
     */
    public Job buildInternship(JobBuilder builder, String title, String company, String location) {
        return builder
                .title(title)
                .company(company)
                .type(JobType.INTERNSHIP)
                .location(location)
                .salaryRange("Unpaid / stipend")
                .deadline("30 days from posting")
                .description("Internship position. Training provided; no prior industry experience required.")
                .build();
    }

    /**
     * An urgent full-time hire: marked Urgent so the Decorator chain
     * lifts its visibility score, with a short deadline to match.
     */
    public Job buildUrgentFullTimeRole(JobBuilder builder, String title, String company,
                                       String location, String salaryRange) {
        return builder
                .title(title)
                .company(company)
                .type(JobType.FULL_TIME)
                .location(location)
                .salaryRange(salaryRange)
                .deadline("14 days from posting")
                .urgent(true)
                .description("We are hiring urgently for this role. Immediate joiners preferred.")
                .build();
    }
}
