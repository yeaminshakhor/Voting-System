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
    
    public static Connection getConnection() {
        try {
            // If connection is null or closed, (re)open it
            if (connection == null || connection.isClosed()) {
                try {
                    Class.forName("org.sqlite.JDBC");
                } catch (ClassNotFoundException e) {
                    System.out.println("❌ SQLite JDBC driver not found: " + e.getMessage());
                    connection = null;
                    return null;
                }

                try {
                    connection = DriverManager.getConnection(DB_URL);
                    connection.setAutoCommit(true);
                    System.out.println("✅ Database connection established");
                    // Do NOT call initializeDatabase() here to avoid repeated initialization
                    // Initialization should be performed once at startup via initializeDatabase().
                } catch (SQLException e) {
                    System.out.println("❌ Database connection failed: " + e.getMessage());
                    connection = null;
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error checking connection state: " + e.getMessage());
            connection = null;
        }

        return connection;
    }
    
    /**
     * Initialize database - creates tables and fixes schema
     */
    public static void initializeDatabase() {
        System.out.println("🔧 Initializing database...");
        
        if (connection == null) {
            System.out.println("❌ Cannot initialize - no database connection");
            return;
        }
        
        try {
            createTables();
            fixExistingSchema();  // Fix any existing databases
            createMissingColumns(); // Ensure all columns exist
            
            // Verify everything is okay
            if (!verifyTables()) {
                System.err.println("⚠️ Database verification failed - some tables may be missing");
            }
            
            System.out.println("✅ Database initialization completed successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Database initialization failed: " + e.getMessage());
            connection = null;  // Ensure connection is null on failure
        }
    }

    /**
     * Public wrapper to attempt fixing existing schema and creating missing columns.
     * Keeps compatibility with older callers like Main.fixDatabaseSchema().
     */
    public static void fixDatabaseSchema() {
        if (connection == null) getConnection();
        fixExistingSchema();
        createMissingColumns();
    }
    
    private static void createTables() {
        if (connection == null) return;
        
        System.out.println("🔧 Creating/verifying database tables...");
        
        String[] createTables = {
            // Admins table with role-based permissions (must match SqlAdminManager usage)
            "CREATE TABLE IF NOT EXISTS admins (" +
            "admin_id TEXT PRIMARY KEY, " +
            "name TEXT NOT NULL, " +
            "password_hash TEXT NOT NULL, " +
            "role TEXT NOT NULL, " +
            "salt TEXT NOT NULL, " +
            "permissions TEXT, " +
            "is_active INTEGER DEFAULT 1, " +
            "needs_password_reset INTEGER DEFAULT 0, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "last_login TIMESTAMP, " +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            
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
            
            // Audit logs table (column names must match SqlAdminManager expectations)
            "CREATE TABLE IF NOT EXISTS audit_logs (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +  // Changed from log_id to id
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
                try {
                    stmt.execute(sql);
                } catch (SQLException e) {
                    System.out.println("⚠️ Error executing SQL: " + e.getMessage());
                }
            }
            System.out.println("✅ All tables created/verified");
        } catch (SQLException e) {
            System.out.println("❌ Error creating tables: " + e.getMessage());
        }
    }
    
    /**
     * Fix existing schema by adding missing columns
     */
    private static void fixExistingSchema() {
        System.out.println("🔧 Fixing existing database schema...");
        
        if (connection == null) return;
        
        try (Statement stmt = connection.createStatement()) {
            // Check and add missing columns to admins table
            String[] adminColumns = {
                "ALTER TABLE admins ADD COLUMN needs_password_reset INTEGER DEFAULT 0",
                "ALTER TABLE admins ADD COLUMN permissions TEXT",
                "ALTER TABLE admins ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
            };
            
            for (String sql : adminColumns) {
                try {
                    stmt.execute(sql);
                    System.out.println("✅ Added missing column to admins table");
                } catch (SQLException e) {
                    // Column already exists, ignore
                }
            }
            
            // Check and add missing columns to audit_logs table
            String[] auditColumns = {
                "ALTER TABLE audit_logs ADD COLUMN admin_id TEXT",
                "ALTER TABLE audit_logs ADD COLUMN action TEXT",
                "ALTER TABLE audit_logs ADD COLUMN details TEXT",
                "ALTER TABLE audit_logs ADD COLUMN timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
            };
            
            for (String sql : auditColumns) {
                try {
                    stmt.execute(sql);
                    System.out.println("✅ Added missing column to audit_logs table");
                } catch (SQLException e) {
                    // Column already exists, ignore
                }
            }
            
        } catch (SQLException e) {
            System.out.println("⚠️ Error fixing schema: " + e.getMessage());
        }
    }
    
    /**
     * Ensure all required columns exist in tables
     */
    private static void createMissingColumns() {
        System.out.println("🔧 Creating any missing columns...");
        
        if (connection == null) return;
        
        // Define required columns for each table
        Map<String, String[]> requiredColumns = new HashMap<>();
        
        // Admins table columns (must match SqlAdminManager expectations)
        requiredColumns.put("admins", new String[]{
            "password_hash", "role", "salt", "permissions", "is_active",
            "needs_password_reset", "created_at", "last_login", "updated_at"
        });
        
        // Audit logs columns (must match SqlAdminManager expectations)
        requiredColumns.put("audit_logs", new String[]{
            "admin_id", "action", "details", "timestamp"
        });
        
        // Voters table columns
        requiredColumns.put("voters", new String[]{
            "password_hash", "salt", "email", "is_registered",
            "has_voted", "registered_at", "last_login", "created_at"
        });
        
        try (Statement stmt = connection.createStatement()) {
            for (String tableName : requiredColumns.keySet()) {
                // Check if table exists
                String checkTableSql = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'";
                ResultSet rs = stmt.executeQuery(checkTableSql);
                
                if (rs.next()) {
                    // Table exists, check columns
                    for (String columnName : requiredColumns.get(tableName)) {
                        try {
                            // Try to select the column - if it fails, column doesn't exist
                            String testSql = "SELECT " + columnName + " FROM " + tableName + " LIMIT 0";
                            stmt.execute(testSql);
                        } catch (SQLException e) {
                            // Column doesn't exist, try to add it
                            String addColumnSql = "ALTER TABLE " + tableName + 
                                                 " ADD COLUMN " + getColumnDefinition(tableName, columnName);
                            try {
                                stmt.execute(addColumnSql);
                                System.out.println("✅ Added column " + columnName + " to " + tableName);
                            } catch (SQLException ex) {
                                System.out.println("⚠️ Could not add column " + columnName + " to " + tableName + ": " + ex.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("⚠️ Error creating missing columns: " + e.getMessage());
        }
    }
    
    /**
     * Get column definition based on table and column name
     */
    private static String getColumnDefinition(String tableName, String columnName) {
        switch (tableName) {
            case "admins":
                switch (columnName) {
                    case "password_hash":
                    case "role":
                    case "salt":
                    case "permissions":
                        return columnName + " TEXT NOT NULL DEFAULT ''";
                    case "is_active":
                    case "needs_password_reset":
                        return columnName + " INTEGER DEFAULT 0";
                    case "created_at":
                    case "last_login":
                    case "updated_at":
                        return columnName + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP";
                }
                break;
                
            case "audit_logs":
                switch (columnName) {
                    case "admin_id":
                        return columnName + " TEXT";
                    case "action":
                        return columnName + " TEXT NOT NULL";
                    case "details":
                        return columnName + " TEXT";
                    case "timestamp":
                        return columnName + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP";
                }
                break;
                
            case "voters":
                switch (columnName) {
                    case "password_hash":
                    case "salt":
                    case "email":
                        return columnName + " TEXT";
                    case "is_registered":
                    case "has_voted":
                        return columnName + " INTEGER DEFAULT 0";
                    case "registered_at":
                    case "last_login":
                    case "created_at":
                        return columnName + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP";
                }
                break;
        }
        
        // Default fallback
        return columnName + " TEXT";
    }
    
    /**
     * Verify that all required tables exist and have correct structure
     */
    public static boolean verifyTables() {
        System.out.println("🔍 Verifying database tables...");
        
        if (connection == null) {
            System.out.println("❌ Cannot verify - no database connection");
            return false;
        }
        
        String[] requiredTables = {
            "admins", "voters", "nominees", "votes", "audit_logs", "election_config"
        };
        
        try (Statement stmt = connection.createStatement()) {
            boolean allTablesExist = true;
            
            for (String tableName : requiredTables) {
                String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'";
                ResultSet rs = stmt.executeQuery(sql);
                
                if (rs.next()) {
                    System.out.println("✅ Table exists: " + tableName);
                } else {
                    System.out.println("❌ Table missing: " + tableName);
                    allTablesExist = false;
                }
            }
            
            // Check for empty admins table
            if (allTablesExist) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM admins");
                if (rs.next() && rs.getInt("count") == 0) {
                    System.out.println("⚠️ Admins table is empty - first-time setup needed");
                }
            }
            
            return allTablesExist;
            
        } catch (SQLException e) {
            System.out.println("❌ Error verifying tables: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Execute an INSERT, UPDATE, or DELETE statement
     */
    public static boolean executeUpdate(String sql, Object... params) {
        Connection conn = getConnection();
        if (conn == null) return false;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        Connection conn = getConnection();
        if (conn == null) return null;
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
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
     * Safely close a ResultSet
     */
    public static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                // Ignore
            }
        }
    }
    
    /**
     * Safely close a PreparedStatement
     */
    public static void closeStatement(PreparedStatement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                // Ignore
            }
        }
    }
    
    /**
     * Close database connection
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
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
    
    /**
     * Reset database for testing
     */
    public static void resetDatabase() {
        closeConnection();
        connection = null;
        System.out.println("🔄 Database connection reset");
    }
    
    /**
     * Backup database to file
     */
    public static boolean backupDatabase(String backupPath) {
        Connection conn = getConnection();
        if (conn == null) return false;
        
        try {
            String backupSql = "BACKUP TO '" + backupPath + "'";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(backupSql);
                System.out.println("✅ Database backed up to: " + backupPath);
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Database backup failed: " + e.getMessage());
            return false;
        }
    }
}