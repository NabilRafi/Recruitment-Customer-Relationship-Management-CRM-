package com.recruitcrm.patterns.singleton;

import com.recruitcrm.domain.Application;
import com.recruitcrm.domain.ApplicationStatus;
import com.recruitcrm.domain.Candidate;
import com.recruitcrm.domain.Job;
import com.recruitcrm.domain.JobType;
import com.recruitcrm.patterns.builder.JobBuilder;
import com.recruitcrm.domain.UserAccount;
import com.recruitcrm.persistence.Database;
import com.recruitcrm.web.AuthRepository;
import com.recruitcrm.web.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * SINGLETON PATTERN.
 *
 * One shared data access point for the whole application. Persistence
 * is backed by SQLite (see persistence.Database) instead of Java
 * serialization — data survives restarts and is queryable like a
 * real database.
 */
public final class DataStore {
    private static DataStore instance;

    private DataStore() {
        try {
            Database.getConnection();
            seedDemoUsersIfEmpty();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize database", e);
        }
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    public synchronized void saveAccount(String id, UserAccount account) {
        try {
            PasswordUtil.HashResult creds = PasswordUtil.hashPassword("changeme");
            AuthRepository.saveAccount(account, creds.hash(), creds.salt());
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save account", e);
        }
    }

    /** Saves an account created through registration with a real password. */
    public synchronized void saveRegisteredAccount(UserAccount account, String passwordHash, String passwordSalt) {
        try {
            AuthRepository.saveAccount(account, passwordHash, passwordSalt);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save account", e);
        }
    }

    public UserAccount getAccount(String id) {
        try {
            return AuthRepository.loadAccount(id);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load account", e);
        }
    }

    public synchronized void saveJob(Job job) {
        saveJob(job, null);
    }

    public synchronized void saveJob(Job job, String postedBy) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                 INSERT INTO jobs (id, title, company_name, type, description, featured, urgent, posted_by,
                                   location, salary_range, deadline, base_salary)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                 ON CONFLICT(id) DO UPDATE SET
                   title = excluded.title,
                   company_name = excluded.company_name,
                   type = excluded.type,
                   description = excluded.description,
                   featured = excluded.featured,
                   urgent = excluded.urgent,
                   posted_by = COALESCE(excluded.posted_by, jobs.posted_by),
                   location = excluded.location,
                   salary_range = excluded.salary_range,
                   deadline = excluded.deadline,
                   base_salary = excluded.base_salary
                 """)) {
            ps.setString(1, job.getId());
            ps.setString(2, job.getTitle());
            ps.setString(3, job.getCompanyName());
            ps.setString(4, job.getType().name());
            ps.setString(5, job.getDescription());
            ps.setInt(6, job.isFeatured() ? 1 : 0);
            ps.setInt(7, job.isUrgent() ? 1 : 0);
            ps.setString(8, postedBy);
            ps.setString(9, job.getLocation());
            ps.setString(10, job.getSalaryRange());
            ps.setString(11, job.getDeadline());
            ps.setDouble(12, job.getBaseSalary());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save job", e);
        }
    }

    public Job getJob(String id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, title, company_name, type, description, featured, urgent, location, salary_range, deadline, base_salary FROM jobs WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rowToJob(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load job", e);
        }
    }

    public List<Job> getAllJobs() {
        List<Job> jobs = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, title, company_name, type, description, featured, urgent, location, salary_range, deadline, base_salary FROM jobs ORDER BY title");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                jobs.add(rowToJob(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not list jobs", e);
        }
        return jobs;
    }

    public synchronized void saveApplication(Application application) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                 INSERT INTO applications (id, candidate_email, job_id, status, evaluation_summary,
                                           evaluation_score, offer_entitlements)
                 VALUES (?, ?, ?, ?, ?, ?, ?)
                 ON CONFLICT(id) DO UPDATE SET
                   status = excluded.status,
                   evaluation_summary = excluded.evaluation_summary,
                   evaluation_score = excluded.evaluation_score,
                   offer_entitlements = excluded.offer_entitlements
                 """)) {
            ps.setString(1, application.getId());
            ps.setString(2, application.getCandidate().getEmail());
            ps.setString(3, application.getJob().getId());
            ps.setString(4, application.getStatus().name());
            ps.setString(5, application.getLastEvaluationSummary());
            ps.setInt(6, application.getEvaluationScore());
            ps.setString(7, application.getOfferEntitlements());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save application", e);
        }
    }

    /**
     * True when this candidate has already applied to this job.
     *
     * FR6 in the proposal, and the guard shown in sequence diagram 5.1:
     * a candidate must not be able to apply to the same role twice.
     * Checked in SQL so the answer is authoritative rather than depending
     * on what happens to be loaded in memory.
     */
    public boolean applicationExists(String candidateEmail, String jobId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM applications WHERE LOWER(candidate_email) = LOWER(?) AND job_id = ?")) {
            ps.setString(1, candidateEmail);
            ps.setString(2, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not check for an existing application", e);
        }
    }

    /**
     * Removes an application. Used when a candidate withdraws (FR8).
     * Returns false when no row matched.
     */
    public synchronized boolean deleteApplication(String id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM applications WHERE id = ?")) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not withdraw application", e);
        }
    }

    public Application getApplication(String id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, candidate_email, job_id, status, evaluation_summary, evaluation_score, offer_entitlements FROM applications WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rowToApplication(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load application", e);
        }
    }

    public List<Application> getAllApplications() {
        List<Application> apps = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, candidate_email, job_id, status, evaluation_summary, evaluation_score, offer_entitlements FROM applications ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Application app = rowToApplication(rs);
                if (app != null) {          // skip orphaned rows
                    apps.add(app);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not list applications", e);
        }
        return apps;
    }

    /**
     * Rebuilds an Application from a database row.
     *
     * Returns null when the row references a candidate or job that no longer
     * exists, rather than throwing. An orphaned row is bad data, but it must
     * not take down the whole list - one unreadable application should not
     * stop a recruiter seeing every other one. The row is reported once so
     * the problem is visible rather than silent.
     */
    private Application rowToApplication(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        UserAccount account = AuthRepository.loadAccount(rs.getString("candidate_email"));
        if (!(account instanceof Candidate candidate)) {
            System.err.println("[DATA] Skipping application " + id
                    + " - candidate not found: " + rs.getString("candidate_email"));
            return null;
        }
        Job job = getJob(rs.getString("job_id"));
        if (job == null) {
            System.err.println("[DATA] Skipping application " + id
                    + " - job not found: " + rs.getString("job_id"));
            return null;
        }
        Application app = new Application(id, candidate, job);
        app.setStatus(ApplicationStatus.valueOf(rs.getString("status")));
        app.setLastEvaluationSummary(rs.getString("evaluation_summary"));
        app.setEvaluationScore(rs.getInt("evaluation_score"));
        app.setOfferEntitlements(rs.getString("offer_entitlements"));
        return app;
    }

    /**
     * Rebuilds a Job from a database row using the BUILDER pattern.
     *
     * Using the builder here means the row -> object mapping names every
     * field it sets, instead of relying on the order of an 8-argument
     * constructor call.
     */
    private Job rowToJob(ResultSet rs) throws SQLException {
        return new JobBuilder()
                .id(rs.getString("id"))
                .title(rs.getString("title"))
                .company(rs.getString("company_name"))
                .type(JobType.valueOf(rs.getString("type")))
                .description(rs.getString("description") == null ? "" : rs.getString("description"))
                .location(rs.getString("location") == null ? "" : rs.getString("location"))
                .salaryRange(rs.getString("salary_range") == null ? "" : rs.getString("salary_range"))
                .deadline(rs.getString("deadline") == null ? "" : rs.getString("deadline"))
                .featured(rs.getInt("featured") == 1)
                .baseSalary(rs.getDouble("base_salary"))
                .urgent(rs.getInt("urgent") == 1)
                .build();
    }

    private void seedDemoUsersIfEmpty() throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS c FROM accounts");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            if (rs.getInt("c") > 0) {
                return;
            }
        }
        PasswordUtil.HashResult recruiter = PasswordUtil.hashPassword("demo123");
        PasswordUtil.HashResult candidate = PasswordUtil.hashPassword("demo123");
        AuthRepository.saveAccount(
                new com.recruitcrm.domain.Recruiter("Demo Recruiter", "recruiter@demo.com", "TechNova Ltd"),
                recruiter.hash(), recruiter.salt());
        AuthRepository.saveAccount(
                new Candidate("Demo Candidate", "candidate@demo.com", "https://example.com/cv"),
                candidate.hash(), candidate.salt());
        System.out.println("Seeded demo users: recruiter@demo.com / demo123, candidate@demo.com / demo123");
    }
}
