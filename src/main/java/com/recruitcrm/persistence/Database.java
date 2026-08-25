package com.recruitcrm.persistence;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public final class Database {
    private static final String DEFAULT_PATH = "data/crm.db";
    private static boolean schemaReady = false;

    private Database() {}

    
    public static synchronized Connection getConnection() throws SQLException {
        String path = System.getenv().getOrDefault("DATABASE_PATH", DEFAULT_PATH);
        File file = new File(path);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path);
        conn.setAutoCommit(true);
        if (!schemaReady) {
            initSchema(conn);
            schemaReady = true;
        }
        return conn;
    }

    private static void addColumnIfMissing(Connection conn, String table, String column, String definition) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (SQLException e) {
            // Column already exists - expected on every run after the first.
        }
    }

    private static void initSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS accounts (
                  email TEXT PRIMARY KEY,
                  role TEXT NOT NULL,
                  name TEXT NOT NULL,
                  password_hash TEXT NOT NULL,
                  password_salt TEXT NOT NULL,
                  extra TEXT
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS jobs (
                  id TEXT PRIMARY KEY,
                  title TEXT NOT NULL,
                  company_name TEXT NOT NULL,
                  type TEXT NOT NULL,
                  description TEXT,
                  featured INTEGER NOT NULL DEFAULT 0,
                  urgent INTEGER NOT NULL DEFAULT 0,
                  posted_by TEXT
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS applications (
                  id TEXT PRIMARY KEY,
                  candidate_email TEXT NOT NULL,
                  job_id TEXT NOT NULL,
                  status TEXT NOT NULL,
                  evaluation_summary TEXT,
                  FOREIGN KEY (candidate_email) REFERENCES accounts(email),
                  FOREIGN KEY (job_id) REFERENCES jobs(id)
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sessions (
                  token TEXT PRIMARY KEY,
                  email TEXT NOT NULL,
                  expires_at INTEGER NOT NULL,
                  FOREIGN KEY (email) REFERENCES accounts(email)
                )
                """);
        }

                
        addColumnIfMissing(conn, "jobs", "location", "TEXT DEFAULT ''");
        addColumnIfMissing(conn, "jobs", "salary_range", "TEXT DEFAULT ''");
        addColumnIfMissing(conn, "jobs", "deadline", "TEXT DEFAULT ''");
        addColumnIfMissing(conn, "jobs", "base_salary", "REAL DEFAULT 0");
        addColumnIfMissing(conn, "applications", "evaluation_score", "INTEGER DEFAULT 0");
        addColumnIfMissing(conn, "applications", "offer_entitlements", "TEXT DEFAULT ''");
    }
}
