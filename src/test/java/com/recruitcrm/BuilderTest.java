package com.recruitcrm;

import com.recruitcrm.domain.Job;
import com.recruitcrm.domain.JobType;
import com.recruitcrm.patterns.builder.JobBuilder;
import com.recruitcrm.patterns.builder.JobDirector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** BUILDER PATTERN tests: defaults, validation, fluency and the Director. */
class BuilderTest {

    @Test
    @DisplayName("Only the required fields are needed; the rest take defaults")
    void requiredFieldsOnly() {
        Job job = new JobBuilder()
                .title("Backend Engineer")
                .company("TechNova Ltd")
                .build();

        assertEquals("Backend Engineer", job.getTitle());
        assertEquals(JobType.FULL_TIME, job.getType(), "type defaults to FULL_TIME");
        assertEquals("", job.getLocation(), "optional strings default to empty, never null");
        assertEquals("", job.getSalaryRange());
        assertEquals(0, job.getBaseSalary());
    }

    @Test
    @DisplayName("An ID is generated when none is supplied")
    void generatesIdWhenMissing() {
        Job job = new JobBuilder().title("QA Engineer").company("TechNova").build();

        assertNotNull(job.getId());
        assertTrue(job.getId().startsWith("job-"));
    }

    @Test
    @DisplayName("A supplied ID is preserved - used when loading from the database")
    void preservesSuppliedId() {
        Job job = new JobBuilder()
                .id("job-abc123")
                .title("QA Engineer")
                .company("TechNova")
                .build();

        assertEquals("job-abc123", job.getId());
    }

    @Test
    @DisplayName("build() rejects a job with no title")
    void rejectsMissingTitle() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new JobBuilder().company("TechNova").build());

        assertTrue(e.getMessage().toLowerCase().contains("title"));
    }

    @Test
    @DisplayName("build() rejects a job with no company")
    void rejectsMissingCompany() {
        assertThrows(IllegalArgumentException.class,
                () -> new JobBuilder().title("Backend Engineer").build());
    }

    @Test
    @DisplayName("build() rejects a blank title, not just a null one")
    void rejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class,
                () -> new JobBuilder().title("   ").company("TechNova").build());
    }

    @Test
    @DisplayName("Every setter returns the builder, so calls chain")
    void settersAreFluent() {
        JobBuilder builder = new JobBuilder();

        assertSame(builder, builder.title("X"));
        assertSame(builder, builder.company("Y"));
        assertSame(builder, builder.location("Dhaka"));
        assertSame(builder, builder.baseSalary(50_000));
    }

    @Test
    @DisplayName("All optional fields are carried through to the product")
    void allOptionalFieldsCarried() {
        Job job = new JobBuilder()
                .title("Backend Engineer")
                .company("TechNova Ltd")
                .type(JobType.CONTRACT)
                .description("Java APIs")
                .location("Dhaka")
                .salaryRange("80,000 - 120,000 BDT")
                .deadline("30 September 2026")
                .baseSalary(50_000)
                .featured(true)
                .urgent(true)
                .build();

        assertEquals(JobType.CONTRACT, job.getType());
        assertEquals("Dhaka", job.getLocation());
        assertEquals(50_000, job.getBaseSalary());
        assertTrue(job.isFeatured());
        assertTrue(job.isUrgent());
    }

    @Test
    @DisplayName("The Director produces a standard internship without the caller knowing the steps")
    void directorBuildsInternship() {
        Job job = new JobDirector()
                .buildInternship(new JobBuilder(), "Design Intern", "TechNova", "Dhaka");

        assertEquals(JobType.INTERNSHIP, job.getType());
        assertEquals("Dhaka", job.getLocation());
        assertFalse(job.getDeadline().isBlank(), "the Director supplies a deadline");
        assertFalse(job.getDescription().isBlank());
    }

    @Test
    @DisplayName("The Director's urgent recipe marks the job urgent")
    void directorBuildsUrgentRole() {
        Job job = new JobDirector().buildUrgentFullTimeRole(
                new JobBuilder(), "Backend Engineer", "TechNova", "Dhaka", "80k-120k");

        assertEquals(JobType.FULL_TIME, job.getType());
        assertTrue(job.isUrgent(), "the urgent recipe sets the flag the Decorator reads");
    }
}
