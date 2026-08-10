package com.recruitcrm.patterns.singleton;

import com.recruitcrm.domain.Application;
import com.recruitcrm.domain.ApplicationStatus;
import com.recruitcrm.domain.Candidate;
import com.recruitcrm.domain.Job;
import com.recruitcrm.domain.JobType;
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
                 INSERT INTO jobs (id, title, company_name, type, description, featured, urgent, posted_by)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                 ON CONFLICT(id) DO UPDATE SET
                   title = excluded.title,
                   company_name = excluded.company_name,
                   type = excluded.type,
                   description = excluded.description,
                   featured = excluded.featured,
                   urgent = excluded.urgent,
                   posted_by = COALESCE(excluded.posted_by, jobs.posted_by)
                 """)) {
            ps.setString(1, job.getId());
            ps.setString(2, job.getTitle());
            ps.setString(3, job.getCompanyName());
            ps.setString(4, job.getType().name());
            ps.setString(5, job.getDescription());
            ps.setInt(6, job.isFeatured() ? 1 : 0);
            ps.setInt(7, job.isUrgent() ? 1 : 0);
            ps.setString(8, postedBy);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save job", e);
        }
    }

    public Job getJob(String id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, title, company_name, type, description, featured, urgent FROM jobs WHERE id = ?")) {
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
                     "SELECT id, title, company_name, type, description, featured, urgent FROM jobs ORDER BY title");
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
                 INSERT INTO applications (id, candidate_email, job_id, status, evaluation_summary)
                 VALUES (?, ?, ?, ?, ?)
                 ON CONFLICT(id) DO UPDATE SET
                   status = excluded.status,
                   evaluation_summary = excluded.evaluation_summary
                 """)) {
            ps.setString(1, application.getId());
            ps.setString(2, application.getCandidate().getEmail());
            ps.setString(3, application.getJob().getId());
            ps.setString(4, application.getStatus().name());
            ps.setString(5, application.getLastEvaluationSummary());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save application", e);
        }
    }

    public Application getApplication(String id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, candidate_email, job_id, status, evaluation_summary FROM applications WHERE id = ?")) {
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
                     "SELECT id, candidate_email, job_id, status, evaluation_summary FROM applications ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                apps.add(rowToApplication(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not list applications", e);
        }
        return apps;
    }

    private Application rowToApplication(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        UserAccount account = AuthRepository.loadAccount(rs.getString("candidate_email"));
        if (!(account instanceof Candidate candidate)) {
            throw new IllegalStateException("Application candidate not found: " + rs.getString("candidate_email"));
        }
        Job job = getJob(rs.getString("job_id"));
        if (job == null) {
            throw new IllegalStateException("Application job not found: " + rs.getString("job_id"));
        }
        Application app = new Application(id, candidate, job);
        app.setStatus(ApplicationStatus.valueOf(rs.getString("status")));
        app.setLastEvaluationSummary(rs.getString("evaluation_summary"));
        return app;
    }

    private Job rowToJob(ResultSet rs) throws SQLException {
        Job job = new Job(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("company_name"),
                JobType.valueOf(rs.getString("type")),
                rs.getString("description") == null ? "" : rs.getString("description")
        );
        job.setFeatured(rs.getInt("featured") == 1);
        job.setUrgent(rs.getInt("urgent") == 1);
        return job;
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
