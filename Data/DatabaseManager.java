package Data;

import java.sql.*;
import java.util.*;

/**
 * Enhanced Database Manager with complete SQL operations.
 * Provides centralized database connectivity and utility methods.
 * No more plain text database files - all data is stored in SQLite.
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:election_system.db";
    private static Connection connection = null;
    private static boolean dbInitialized = false;
    
    public static Connection getConnection() {
        if (connection == null && !dbInitialized) {
            try {
                // Load SQLite JDBC driver
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(DB_URL);
                connection.setAutoCommit(true);
                createTables();
                System.out.println("✅ Database connection established");
            } catch (ClassNotFoundException e) {
                System.out.println("❌ SQLite JDBC driver not found: " + e.getMessage());
                connection = null;
            } catch (SQLException e) {
                System.out.println("❌ Database connection failed: " + e.getMessage());
                connection = null;
            } finally {
                dbInitialized = true;
            }
        }
        return connection;
    }
    
    private static void createTables() {
        if (connection == null) return;
        
        String[] createTables = {
            // Admins table with role-based permissions
            "CREATE TABLE IF NOT EXISTS admins (" +
            "admin_id TEXT PRIMARY KEY, " +
            "name TEXT NOT NULL, " +
            "password_hash TEXT NOT NULL, " +
            "role TEXT NOT NULL, " +
            "salt TEXT NOT NULL, " +
            "is_active INTEGER DEFAULT 1, " +
            "permissions TEXT, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "last_login TIMESTAMP)",
            
            // Voters table
            "CREATE TABLE IF NOT EXISTS voters (" +
            "voter_id TEXT PRIMARY KEY, " +
            "name TEXT NOT NULL, " +
            "password_hash TEXT, " +
            "salt TEXT, " +
            "email TEXT, " +
            "is_registered INTEGER DEFAULT 0, " +
            "has_voted INTEGER DEFAULT 0, " +
            "registered_at TIMESTAMP, " +
            "last_login TIMESTAMP, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            
            // Nominees table
            "CREATE TABLE IF NOT EXISTS nominees (" +
            "nominee_id TEXT PRIMARY KEY, " +
            "name TEXT NOT NULL, " +
            "party TEXT NOT NULL, " +
            "position TEXT, " +
            "is_active INTEGER DEFAULT 1, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            
            // Votes table
            "CREATE TABLE IF NOT EXISTS votes (" +
            "vote_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "voter_id TEXT NOT NULL, " +
            "nominee_id TEXT NOT NULL, " +
            "cast_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (voter_id) REFERENCES voters(voter_id), " +
            "FOREIGN KEY (nominee_id) REFERENCES nominees(nominee_id))",
            
            // Audit logs table
            "CREATE TABLE IF NOT EXISTS audit_logs (" +
            "log_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "admin_id TEXT, " +
            "action TEXT NOT NULL, " +
            "details TEXT, " +
            "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (admin_id) REFERENCES admins(admin_id))",
            
            // Election configuration
            "CREATE TABLE IF NOT EXISTS election_config (" +
            "config_id TEXT PRIMARY KEY, " +
            "is_active INTEGER DEFAULT 0, " +
            "start_date TIMESTAMP, " +
            "end_date TIMESTAMP, " +
            "election_name TEXT, " +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
        };
        
        try (Statement stmt = connection.createStatement()) {
            for (String sql : createTables) {
                stmt.execute(sql);
            }
            System.out.println("✅ All tables created/verified");
        } catch (SQLException e) {
            System.out.println("⚠️ Error creating tables: " + e.getMessage());
        }
    }
    
    /**
     * Execute an INSERT, UPDATE, or DELETE statement
     */
    public static boolean executeUpdate(String sql, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Error executing update: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Execute a SELECT query and return results
     */
    public static ResultSet executeQuery(String sql, Object... params) {
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeQuery();
        } catch (SQLException e) {
            System.out.println("❌ Error executing query: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Close database connection
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✅ Database connection closed");
            }
        } catch (SQLException e) {
            System.out.println("❌ Error closing connection: " + e.getMessage());
        }
    }
    
    /**
     * Check if database is available
     */
    public static boolean isDatabaseAvailable() {
        return getConnection() != null;
    }
}