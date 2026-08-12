package com.recruitcrm.persistence;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite connection and schema setup. DataStore (Singleton) delegates
 * all persistence here — this is the real database layer replacing
 * the old Java-serialization file store.
 */
public final class Database {
    private static final String DEFAULT_PATH = "data/crm.db";
    private static boolean schemaReady = false;

    private Database() {}

    /**
     * Returns a NEW connection each call, on purpose.
     *
     * An earlier version cached one shared static Connection and handed
     * the same object to everybody. Because every caller wraps it in
     * try-with-resources, the first block to finish closed the shared
     * connection for everyone else. That broke any nested query — e.g.
     * listing applications, which loops over a ResultSet and looks up
     * each candidate account inside the loop — with "stmt pointer is
     * closed" halfway through.
     *
     * A fresh connection per call keeps try-with-resources correct: each
     * caller owns and closes exactly its own connection. SQLite is
     * perfectly happy with several connections to the same file.
     */
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
    }
}
