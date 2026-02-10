package Data;

import Entities.Admin;
import Utils.SecurityUtils;
import Utils.AdminRole;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SqlAdminManager {
    
    // Cache for frequently accessed admin data
    private static final Map<String, Admin> adminCache = new ConcurrentHashMap<>();
    private static final Map<String, Integer> loginAttempts = new ConcurrentHashMap<>();
    
    // ==================== SYSTEM INITIALIZATION ====================
    
    /**
     * Initialize the admin system - called once at application startup
     */
    public static void initializeAdminSystem() {
        System.out.println("\n🔧 [SqlAdminManager] Initializing Admin System...");
        
        Connection conn = getConnection();
        if (conn == null) {
            System.out.println("❌ [SqlAdminManager] Database connection failed");
            return;
        }
        
        try {
            // First, ensure the admins table exists
            createAdminTableIfNotExists(conn);
            
            // Create additional security tables
            createSecurityTables(conn);
            
            // Check if any admin exists
            if (isFirstSetup()) {
                System.out.println("⚠️ [SqlAdminManager] No admins found - Creating default SuperAdmin");
                createDefaultSuperAdmin();
            } else {
                int adminCount = countAdmins();
                System.out.println("✅ [SqlAdminManager] System ready with " + adminCount + " admin(s)");
            }
            
            // Clear cache on startup
            adminCache.clear();
            loginAttempts.clear();
            
        } catch (SQLException e) {
            System.out.println("❌ [SqlAdminManager] System initialization failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
    }
    
    /**
     * Create admins table if it doesn't exist
     */
    private static void createAdminTableIfNotExists(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS admins (" +
                     "admin_id VARCHAR(50) PRIMARY KEY," +
                     "name VARCHAR(100) NOT NULL," +
                     "password_hash TEXT NOT NULL," +
                     "role VARCHAR(30) NOT NULL," +
                     "salt TEXT NOT NULL," +
                     "permissions TEXT," +
                     "is_active INTEGER DEFAULT 1," +
                     "needs_password_reset INTEGER DEFAULT 0," +
                     "failed_login_attempts INTEGER DEFAULT 0," +
                     "account_locked_until TIMESTAMP," +
                     "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                     "last_login TIMESTAMP," +
                     "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("✅ [SqlAdminManager] Admins table verified/created");
        }
        
        // Create audit_logs table with correct column names
        String auditSql = "CREATE TABLE IF NOT EXISTS audit_logs (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "admin_id VARCHAR(50) NOT NULL," +
                         "action VARCHAR(100) NOT NULL," +
                         "details TEXT," +
                         "ip_address VARCHAR(45)," +
                         "user_agent TEXT," +
                         "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(auditSql);
        }
        
        // Create session table
        String sessionSql = "CREATE TABLE IF NOT EXISTS admin_sessions (" +
                          "session_id VARCHAR(100) PRIMARY KEY," +
                          "admin_id VARCHAR(50) NOT NULL," +
                          "login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                          "last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                          "ip_address VARCHAR(45)," +
                          "user_agent TEXT," +
                          "expires_at TIMESTAMP NOT NULL)";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sessionSql);
        }
    }
    
    /**
     * Create additional security tables
     */
    private static void createSecurityTables(Connection conn) throws SQLException {
        // Create login attempts table
        String attemptsSql = "CREATE TABLE IF NOT EXISTS login_attempts (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "admin_id VARCHAR(50)," +
                            "ip_address VARCHAR(45)," +
                            "attempt_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "success INTEGER DEFAULT 0)";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(attemptsSql);
        }
        
        // Create index for better performance
        String indexSql = "CREATE INDEX IF NOT EXISTS idx_admin_id ON admins(admin_id)";
        String indexSql2 = "CREATE INDEX IF NOT EXISTS idx_admin_role ON admins(role)";
        String indexSql3 = "CREATE INDEX IF NOT EXISTS idx_audit_admin ON audit_logs(admin_id)";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(indexSql);
            stmt.execute(indexSql2);
            stmt.execute(indexSql3);
        }
    }
    
    /**
     * Create default SuperAdmin (superadmin/super123) ONLY if no admins exist
     */
    private static void createDefaultSuperAdmin() {
        String adminId = "superadmin";
        String name = "System Administrator";
        String password = "super123";
        
        // Check if password is strong enough
        if (!isPasswordStrong(password)) {
            System.out.println("⚠️ [SqlAdminManager] Default password is not strong. Using enhanced password.");
            password = "SuperAdmin@123";
        }
        
        if (!adminExists(adminId)) {
            boolean created = addAdmin(adminId, name, password, AdminRole.SUPERADMIN);
            if (created) {
                System.out.println("✅ [SqlAdminManager] Default SuperAdmin created:");
                System.out.println("   ID: superadmin");
                System.out.println("   Password: " + password);
                System.out.println("⚠️  Please change password after first login!");
                
                // Log this system event
                logAdminAction(adminId, "SYSTEM_INIT", "Default SuperAdmin created on first setup", null, null);
            } else {
                System.out.println("❌ [SqlAdminManager] Failed to create default SuperAdmin");
            }
        } else {
            System.out.println("ℹ️ [SqlAdminManager] SuperAdmin already exists");
        }
    }
    
    // ==================== CONNECTION MANAGEMENT ====================
    
    private static Connection getConnection() {
        try {
            return DatabaseManager.getConnection();
        } catch (Exception e) {
            System.err.println("❌ [SqlAdminManager] Failed to get database connection: " + e.getMessage());
            return null;
        }
    }
    
    private static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.getAutoCommit()) {
                    conn.commit();
                }
                conn.close();
            } catch (SQLException e) {
                System.err.println("⚠️ [SqlAdminManager] Error closing connection: " + e.getMessage());
            }
        }
    }
    
    // ==================== AUTHENTICATION ====================
    
    public static boolean validateAdminCredentials(String adminId, String password) {
        return validateAdminCredentials(adminId, password, null, null);
    }
    
    public static boolean validateAdminCredentials(String adminId, String password, String ipAddress, String userAgent) {
        System.out.println("🔐 [SqlAdminManager] Validating admin: " + adminId);
        
        // For login, only do basic validation (allows legacy 6-char passwords)
        if (adminId == null || adminId.trim().isEmpty()) {
            System.out.println("❌ [SqlAdminManager] Admin ID is required");
            return false;
        }
        
        if (password == null || password.isEmpty()) {
            System.out.println("❌ [SqlAdminManager] Password is required");
            return false;
        }
        
        // Check if account is locked
        if (isAccountLocked(adminId)) {
            System.out.println("❌ [SqlAdminManager] Account is locked: " + adminId);
            logAdminAction(adminId, "LOGIN_BLOCKED", "Account locked due to too many failed attempts", ipAddress, userAgent);
            return false;
        }
        
        Connection conn = getConnection();
        if (conn == null) {
            System.out.println("❌ [SqlAdminManager] Database not available");
            logFailedLoginAttempt(adminId, ipAddress, "Database unavailable");
            return false;
        }
        
        String sql = "SELECT password_hash, salt, needs_password_reset, failed_login_attempts FROM admins WHERE admin_id = ? AND is_active = 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            try (ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String salt = rs.getString("salt");
                    int needsReset = rs.getInt("needs_password_reset");
                    int failedAttempts = rs.getInt("failed_login_attempts");
                    
                    System.out.println("✅ [SqlAdminManager] Admin found in database");
                    
                    String inputHash = SecurityUtils.hashPassword(password, salt);
                    
                    if (inputHash != null && inputHash.equals(storedHash)) {
                        System.out.println("✅ [SqlAdminManager] Login successful for: " + adminId);
                        
                        // Reset failed attempts on successful login
                        resetFailedLoginAttempts(adminId);
                        
                        // Update last login
                        updateLastLogin(adminId);
                        
                        if (needsReset == 1) {
                            clearPasswordResetFlag(adminId);
                            System.out.println("⚠️ [SqlAdminManager] Password reset flag cleared");
                        }
                        
                        // Log successful login
                        logAdminAction(adminId, "LOGIN_SUCCESS", "Admin logged in successfully", ipAddress, userAgent);
                        logSuccessfulLoginAttempt(adminId, ipAddress);
                        
                        // Update cache
                        adminCache.remove(adminId);
                        
                        return true;
                    } else {
                        // Increment failed attempts
                        incrementFailedLoginAttempts(adminId);
                        int newAttempts = failedAttempts + 1;
                        
                        if (newAttempts >= 5) {
                            lockAccount(adminId);
                            System.out.println("❌ [SqlAdminManager] Account locked due to 5 failed attempts: " + adminId);
                        } else {
                            System.out.println("❌ [SqlAdminManager] Password mismatch for: " + adminId + " (Attempt " + newAttempts + "/5)");
                        }
                        
                        logFailedLoginAttempt(adminId, ipAddress, "Incorrect password");
                        logAdminAction(adminId, "LOGIN_FAILED", "Incorrect password entered - Attempt " + newAttempts, ipAddress, userAgent);
                        return false;
                    }
                } else {
                    System.out.println("❌ [SqlAdminManager] Admin not found or inactive: " + adminId);
                    logFailedLoginAttempt(adminId, ipAddress, "Admin not found");
                    return false;
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ [SqlAdminManager] Database error: " + e.getMessage());
            logFailedLoginAttempt(adminId, ipAddress, "Database error: " + e.getMessage());
            return false;
        } finally {
            closeConnection(conn);
        }
    }
    
    /**
     * Check if account is locked
     */
    private static boolean isAccountLocked(String adminId) {
        Connection conn = getConnection();
        if (conn == null) return false;
        
        String sql = "SELECT account_locked_until FROM admins WHERE admin_id = ? AND is_active = 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp lockedUntil = rs.getTimestamp("account_locked_until");
                    if (lockedUntil != null) {
                        if (lockedUntil.after(new Timestamp(System.currentTimeMillis()))) {
                            return true;
                        } else {
                            // Lock has expired, unlock the account
                            unlockAccount(adminId);
                            return false;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error checking account lock: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
        
        return false;
    }
    
    /**
     * Lock account for 15 minutes
     */
    private static void lockAccount(String adminId) {
        Connection conn = getConnection();
        if (conn == null) return;
        
        // Lock for 15 minutes
        Timestamp lockUntil = new Timestamp(System.currentTimeMillis() + (15 * 60 * 1000));
        String sql = "UPDATE admins SET account_locked_until = ? WHERE admin_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, lockUntil);
            stmt.setString(2, adminId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error locking account: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
    }
    
    /**
     * Unlock account
     */
    private static void unlockAccount(String adminId) {
        Connection conn = getConnection();
        if (conn == null) return;
        
        String sql = "UPDATE admins SET account_locked_until = NULL, failed_login_attempts = 0 WHERE admin_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error unlocking account: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
    }
    
    /**
     * Increment failed login attempts
     */
    private static void incrementFailedLoginAttempts(String adminId) {
        Connection conn = getConnection();
        if (conn == null) return;
        
        String sql = "UPDATE admins SET failed_login_attempts = failed_login_attempts + 1 WHERE admin_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error incrementing failed attempts: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
    }
    
    /**
     * Reset failed login attempts
     */
    private static void resetFailedLoginAttempts(String adminId) {
        Connection conn = getConnection();
        if (conn == null) return;
        
        String sql = "UPDATE admins SET failed_login_attempts = 0, account_locked_until = NULL WHERE admin_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error resetting failed attempts: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
    }
    
    /**
     * Log failed login attempt
     */
    private static void logFailedLoginAttempt(String adminId, String ipAddress, String reason) {
        Connection conn = getConnection();
        if (conn == null) return;
        
        String sql = "INSERT INTO login_attempts (admin_id, ip_address, success, attempt_time) VALUES (?, ?, 0, CURRENT_TIMESTAMP)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            stmt.setString(2, ipAddress);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error logging failed attempt: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
    }
    
    /**
     * Log successful login attempt
     */
    private static void logSuccessfulLoginAttempt(String adminId, String ipAddress) {
        Connection conn = getConnection();
        if (conn == null) return;
        
        String sql = "INSERT INTO login_attempts (admin_id, ip_address, success, attempt_time) VALUES (?, ?, 1, CURRENT_TIMESTAMP)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            stmt.setString(2, ipAddress);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error logging successful attempt: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
    }
    
    /**
     * FORGOT PASSWORD - Reset password without knowing old password
     */
    public static boolean forgotPassword(String adminId, String newPassword) {
        System.out.println("🔐 [SqlAdminManager] Processing forgot password for: " + adminId);
        
        if (!validateAdminInput(adminId, null, newPassword, null)) {
            System.out.println("❌ [SqlAdminManager] Validation failed");
            return false;
        }
        
        // Verify admin exists
        if (!adminExists(adminId)) {
            System.out.println("❌ [SqlAdminManager] Admin ID not found: " + adminId);
            return false;
        }
        
        // Check password strength
        if (!isPasswordStrong(newPassword)) {
            System.out.println("❌ [SqlAdminManager] New password is not strong enough");
            return false;
        }
        
        return updatePasswordHash(adminId, newPassword, true, "PASSWORD_RESET");
    }
    
    private static void clearPasswordResetFlag(String adminId) {
        Connection conn = getConnection();
        if (conn == null) return;
        
        String sql = "UPDATE admins SET needs_password_reset = 0 WHERE admin_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("⚠️ [SqlAdminManager] Could not clear password reset flag: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
    }
    
    private static void updateLastLogin(String adminId) {
        Connection conn = getConnection();
        if (conn == null) return;
        
        String sql = "UPDATE admins SET last_login = CURRENT_TIMESTAMP WHERE admin_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            stmt.executeUpdate();
            // Clear from cache
            adminCache.remove(adminId);
        } catch (SQLException e) {
            System.err.println("⚠️ [SqlAdminManager] Could not update last login: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
    }
    
    // ==================== ADMIN CRUD OPERATIONS ====================
    
    public static Admin getAdminById(String adminId) {
        // Check cache first
        if (adminCache.containsKey(adminId)) {
            return adminCache.get(adminId);
        }
        
        Connection conn = getConnection();
        if (conn == null) return null;
        
        String sql = "SELECT admin_id, name, role, needs_password_reset FROM admins WHERE admin_id = ? AND is_active = 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            try (ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    Admin admin = new Admin();
                    admin.setAdminId(rs.getString("admin_id"));
                    admin.setName(rs.getString("name"));
                    admin.setRole(rs.getString("role"));
                    
                    // Cache the result
                    adminCache.put(adminId, admin);
                    
                    return admin;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error retrieving admin: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
        
        return null;
    }
    
    public static String getAdminNameById(String adminId) {
        Admin admin = getAdminById(adminId);
        return admin != null ? admin.getName() : null;
    }
    
    public static String getRoleById(String adminId) {
        Admin admin = getAdminById(adminId);
        return admin != null ? admin.getRole() : null;
    }
    
    public static boolean hasPermission(String adminId, String permission) {
        String role = getRoleById(adminId);
        if (role == null) return false;
        return AdminRole.hasPermission(role, permission);
    }
    
    public static boolean addAdmin(String adminId, String name, String password, String role) {
        return addAdminWithTransaction(adminId, name, password, role);
    }
    
    /**
     * Add admin with transaction support
     */
    private static boolean addAdminWithTransaction(String adminId, String name, String password, String role) {
        if (!validateAdminInput(adminId, name, password, role)) {
            System.out.println("❌ [SqlAdminManager] Validation failed");
            return false;
        }
        
        if (adminExists(adminId)) {
            System.out.println("❌ [SqlAdminManager] Admin ID already exists: " + adminId);
            return false;
        }
        
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                System.out.println("❌ [SqlAdminManager] Database not available");
                return false;
            }
            
            // Start transaction
            conn.setAutoCommit(false);
            
            // Hash password
            String salt = SecurityUtils.generateSalt();
            String passwordHash = SecurityUtils.hashPassword(password, salt);
            
            if (passwordHash == null) {
                System.out.println("❌ [SqlAdminManager] Failed to hash password");
                conn.rollback();
                return false;
            }
            
            Set<String> perms = AdminRole.getPermissions(role);
            String permissionsJson = String.join(",", perms);
            
            // Insert admin
            String sql = "INSERT INTO admins (admin_id, name, password_hash, role, salt, permissions, is_active, needs_password_reset) " +
                         "VALUES (?, ?, ?, ?, ?, ?, 1, 0)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, adminId);
                stmt.setString(2, name);
                stmt.setString(3, passwordHash);
                stmt.setString(4, role);
                stmt.setString(5, salt);
                stmt.setString(6, permissionsJson);
                
                stmt.executeUpdate();
            }
            
            // Log the action
            String creator = "system";
            Admin currentAdmin = getCurrentAdmin();
            if (currentAdmin != null) {
                creator = currentAdmin.getAdminId();
            }
            
            logAdminAction(creator, "ADMIN_CREATED", "Created admin: " + adminId + " with role: " + role, null, null);
            
            // Commit transaction
            conn.commit();
            System.out.println("✅ [SqlAdminManager] Admin added: " + adminId + " (" + role + ")");
            
            // Clear cache
            adminCache.clear();
            
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error adding admin: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("❌ [SqlAdminManager] Error rolling back transaction: " + ex.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    // Ignore
                }
                closeConnection(conn);
            }
        }
    }

    private static boolean addAdminWithHash(String adminId, String name, String passwordHash, String salt, String role) {
        if (!validateAdminInput(adminId, name, null, role)) {
            System.out.println("❌ [SqlAdminManager] Validation failed for addAdminWithHash");
            return false;
        }
        if (adminExists(adminId)) {
            System.out.println("❌ [SqlAdminManager] Admin ID already exists: " + adminId);
            return false;
        }
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                System.out.println("❌ [SqlAdminManager] Database not available");
                return false;
            }
            conn.setAutoCommit(false);
            Set<String> perms = AdminRole.getPermissions(role);
            String permissionsJson = String.join(",", perms);
            String sql = "INSERT INTO admins (admin_id, name, password_hash, role, salt, permissions, is_active, needs_password_reset) " +
                         "VALUES (?, ?, ?, ?, ?, ?, 1, 0)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, adminId);
                stmt.setString(2, name);
                stmt.setString(3, passwordHash);
                stmt.setString(4, role);
                stmt.setString(5, salt);
                stmt.setString(6, permissionsJson);
                stmt.executeUpdate();
            }
            String creator = "system";
            Admin currentAdmin = getCurrentAdmin();
            if (currentAdmin != null) creator = currentAdmin.getAdminId();
            logAdminAction(creator, "ADMIN_MIGRATED", "Migrated admin with preserved hash: " + adminId, null, null);
            conn.commit();
            System.out.println("✅ [SqlAdminManager] Admin added (preserved hash): " + adminId + " (" + role + ")");
            adminCache.clear();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error adding admin with hash: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { System.err.println("❌ [SqlAdminManager] Error rolling back transaction: " + ex.getMessage()); }
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ex) { }
                closeConnection(conn);
            }
        }
    }

    public static boolean deleteAdmin(String adminId) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) return false;
            
            conn.setAutoCommit(false);
            
            String sql = "UPDATE admins SET is_active = 0 WHERE admin_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, adminId);
                int rows = stmt.executeUpdate();
                
                if (rows > 0) {
                    // Log deletion
                    String deleter = "system";
                    Admin currentAdmin = getCurrentAdmin();
                    if (currentAdmin != null) {
                        deleter = currentAdmin.getAdminId();
                    }
                    
                    logAdminAction(deleter, "ADMIN_DEACTIVATED", "Deactivated admin: " + adminId, null, null);
                    
                    conn.commit();
                    System.out.println("✅ [SqlAdminManager] Admin deactivated: " + adminId);
                    
                    // Clear cache
                    adminCache.remove(adminId);
                    
                    return true;
                }
            }
            
            conn.rollback();
            return false;
            
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error deleting admin: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("❌ [SqlAdminManager] Error rolling back: " + ex.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    // Ignore
                }
                closeConnection(conn);
            }
        }
    }
    
    public static List<Admin> getAllAdmins() {
        List<Admin> admins = new ArrayList<>();
        Connection conn = getConnection();
        if (conn == null) return admins;
        
        String sql = "SELECT admin_id, name, role FROM admins WHERE is_active = 1 ORDER BY created_at DESC";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Admin admin = new Admin();
                admin.setAdminId(rs.getString("admin_id"));
                admin.setName(rs.getString("name"));
                admin.setRole(rs.getString("role"));
                admins.add(admin);
                
                // Update cache
                adminCache.put(admin.getAdminId(), admin);
            }
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error retrieving admins: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
        
        return admins;
    }
    
    public static boolean adminExists(String adminId) {
        Connection conn = getConnection();
        if (conn == null) return false;
        
        String sql = "SELECT 1 FROM admins WHERE admin_id = ? AND is_active = 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error checking admin existence: " + e.getMessage());
            return false;
        } finally {
            closeConnection(conn);
        }
    }
    
    public static boolean updateAdminPassword(String adminId, String oldPassword, String newPassword) {
        if (!validateAdminCredentials(adminId, oldPassword)) {
            System.out.println("❌ [SqlAdminManager] Current password is incorrect");
            return false;
        }
        
        if (!isPasswordStrong(newPassword)) {
            System.out.println("❌ [SqlAdminManager] New password is not strong enough");
            return false;
        }
        
        return updatePasswordHash(adminId, newPassword, false, "PASSWORD_CHANGED");
    }
    
    public static boolean resetAdminPassword(String adminId, String newPassword) {
        if (!isPasswordStrong(newPassword)) {
            System.out.println("❌ [SqlAdminManager] New password is not strong enough");
            return false;
        }
        
        return updatePasswordHash(adminId, newPassword, true, "PASSWORD_RESET_BY_ADMIN");
    }
    
    /**
     * Centralized password update method
     */
    private static boolean updatePasswordHash(String adminId, String newPassword, boolean needsReset, String logAction) {
        Connection conn = getConnection();
        if (conn == null) return false;
        
        String salt = SecurityUtils.generateSalt();
        String newHash = SecurityUtils.hashPassword(newPassword, salt);
        
        if (newHash == null) {
            System.out.println("❌ [SqlAdminManager] Failed to hash new password");
            return false;
        }
        
        String sql = "UPDATE admins SET password_hash = ?, salt = ?, needs_password_reset = ? WHERE admin_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newHash);
            stmt.setString(2, salt);
            stmt.setInt(3, needsReset ? 1 : 0);
            stmt.setString(4, adminId);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ [SqlAdminManager] Password updated for admin: " + adminId);
                if (needsReset) {
                    System.out.println("⚠️ [SqlAdminManager] Admin must reset password on next login");
                }
                
                // Log the action
                String actor = adminId;
                if (!logAction.equals("PASSWORD_CHANGED")) {
                    Admin currentAdmin = getCurrentAdmin();
                    if (currentAdmin != null) {
                        actor = currentAdmin.getAdminId();
                    }
                }
                
                logAdminAction(actor, logAction, "Password updated for admin: " + adminId, null, null);
                
                // Clear cache
                adminCache.remove(adminId);
                
                return true;
            } else {
                System.out.println("❌ [SqlAdminManager] Admin not found: " + adminId);
                return false;
            }
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error updating password: " + e.getMessage());
            return false;
        } finally {
            closeConnection(conn);
        }
    }
    
    // ==================== AUDIT LOGGING ====================
    
    public static void logAdminAction(String adminId, String action, String details) {
        logAdminAction(adminId, action, details, null, null);
    }
    
    public static void logAdminAction(String adminId, String action, String details, String ipAddress, String userAgent) {
        Connection conn = getConnection();
        if (conn == null) return;
        
        String sql = "INSERT INTO audit_logs (admin_id, action, details, ip_address, user_agent) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            stmt.setString(2, action);
            stmt.setString(3, details);
            stmt.setString(4, ipAddress);
            stmt.setString(5, userAgent);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("⚠️ [SqlAdminManager] Could not log action: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
    }
    
    // ==================== SUPERADMIN-SPECIFIC METHODS ====================
    
    public static boolean addAdminBySuper(String superAdminId, String newAdminId, String adminName, String password, String role) {
        Admin superAdmin = getAdminById(superAdminId);
        if (superAdmin == null || !superAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ [SqlAdminManager] Only SuperAdmin can add other admins");
            logAdminAction(superAdminId, "UNAUTHORIZED_ADMIN_ADD", "Attempted to add admin without SuperAdmin role", null, null);
            return false;
        }
        
        if (!AdminRole.isValidRole(role)) {
            System.out.println("❌ [SqlAdminManager] Invalid role: " + role);
            return false;
        }
        
        boolean success = addAdmin(newAdminId, adminName, password, role);
        if (success) {
            logAdminAction(superAdminId, "ADMIN_ADDED", "SuperAdmin added new admin: " + newAdminId + " with role: " + role, null, null);
        }
        return success;
    }
    
    public static boolean deleteAdminBySuper(String superAdminId, String targetAdminId) {
        Admin superAdmin = getAdminById(superAdminId);
        if (superAdmin == null || !superAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ [SqlAdminManager] Only SuperAdmin can delete admins");
            logAdminAction(superAdminId, "UNAUTHORIZED_ADMIN_DELETE", "Attempted to delete admin without SuperAdmin role", null, null);
            return false;
        }
        
        if (superAdminId.equals(targetAdminId)) {
            System.out.println("❌ [SqlAdminManager] SuperAdmin cannot delete itself");
            return false;
        }
        
        Admin targetAdmin = getAdminById(targetAdminId);
        if (targetAdmin != null && targetAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ [SqlAdminManager] Cannot delete another SuperAdmin");
            return false;
        }
        
        boolean success = deleteAdmin(targetAdminId);
        if (success) {
            logAdminAction(superAdminId, "ADMIN_DELETED", "SuperAdmin deleted admin: " + targetAdminId, null, null);
        }
        return success;
    }
    
    public static boolean reassignAdminRoleBySuper(String superAdminId, String targetAdminId, String newRole) {
        Admin superAdmin = getAdminById(superAdminId);
        if (superAdmin == null || !superAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ [SqlAdminManager] Only SuperAdmin can reassign roles");
            logAdminAction(superAdminId, "UNAUTHORIZED_ROLE_CHANGE", "Attempted to change role without SuperAdmin role", null, null);
            return false;
        }
        
        if (!AdminRole.isValidRole(newRole)) {
            System.out.println("❌ [SqlAdminManager] Invalid role: " + newRole);
            return false;
        }
        
        Admin targetAdmin = getAdminById(targetAdminId);
        if (targetAdmin != null && targetAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ [SqlAdminManager] Cannot change SuperAdmin role");
            return false;
        }
        
        Connection conn = getConnection();
        if (conn == null) {
            System.out.println("❌ [SqlAdminManager] Database connection failed");
            return false;
        }
        
        String sql = "UPDATE admins SET role = ? WHERE admin_id = ? AND is_active = 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newRole);
            stmt.setString(2, targetAdminId);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                logAdminAction(superAdminId, "ROLE_REASSIGNED", "SuperAdmin changed role for " + targetAdminId + " to: " + newRole, null, null);
                System.out.println("✅ [SqlAdminManager] Role updated for admin: " + targetAdminId);
                
                // Clear cache
                adminCache.remove(targetAdminId);
                
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Database error: " + e.getMessage());
            return false;
        } finally {
            closeConnection(conn);
        }
        
        return false;
    }
    
    public static boolean changeAdminPasswordBySuper(String superAdminId, String targetAdminId, String newPassword) {
        Admin superAdmin = getAdminById(superAdminId);
        if (superAdmin == null || !superAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ [SqlAdminManager] Only SuperAdmin can change admin passwords");
            logAdminAction(superAdminId, "UNAUTHORIZED_PASSWORD_CHANGE", "Attempted to change password without SuperAdmin role", null, null);
            return false;
        }
        
        if (!isPasswordStrong(newPassword)) {
            System.out.println("❌ [SqlAdminManager] New password is not strong enough");
            return false;
        }
        
        Connection conn = getConnection();
        if (conn == null) {
            System.out.println("❌ [SqlAdminManager] Database connection failed");
            return false;
        }
        
        String salt = SecurityUtils.generateSalt();
        String passwordHash = SecurityUtils.hashPassword(newPassword, salt);
        
        if (passwordHash == null) {
            System.out.println("❌ [SqlAdminManager] Failed to hash password");
            return false;
        }
        
        String sql = "UPDATE admins SET password_hash = ?, salt = ?, needs_password_reset = 1 WHERE admin_id = ? AND is_active = 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, passwordHash);
            stmt.setString(2, salt);
            stmt.setString(3, targetAdminId);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                logAdminAction(superAdminId, "PASSWORD_RESET_BY_SUPER", "SuperAdmin reset password for admin: " + targetAdminId, null, null);
                System.out.println("✅ [SqlAdminManager] Password updated for admin: " + targetAdminId);
                System.out.println("⚠️ [SqlAdminManager] Admin must reset password on next login");
                
                // Clear cache
                adminCache.remove(targetAdminId);
                
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Database error: " + e.getMessage());
            return false;
        } finally {
            closeConnection(conn);
        }
        
        return false;
    }
    
    public static List<Admin> getAllAdmins(String superAdminId) {
        Admin superAdmin = getAdminById(superAdminId);
        if (superAdmin == null || !superAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ [SqlAdminManager] Only SuperAdmin can view all admins");
            logAdminAction(superAdminId, "UNAUTHORIZED_VIEW_ADMINS", "Attempted to view all admins without SuperAdmin role", null, null);
            return new ArrayList<>();
        }
        
        Connection conn = getConnection();
        if (conn == null) return new ArrayList<>();
        
        List<Admin> admins = new ArrayList<>();
        String sql = "SELECT admin_id, name, role, is_active, created_at, last_login, needs_password_reset FROM admins ORDER BY created_at ASC";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Admin admin = new Admin();
                admin.setAdminId(rs.getString("admin_id"));
                admin.setName(rs.getString("name"));
                admin.setRole(rs.getString("role"));
                admins.add(admin);
                
                // Update cache
                adminCache.put(admin.getAdminId(), admin);
            }
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error retrieving all admins: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
        
        return admins;
    }
    
    // ==================== UTILITY METHODS ====================
    
    private static boolean isFirstSetup() {
        Connection conn = getConnection();
        if (conn == null) return true;
        
        String sql = "SELECT COUNT(*) as cnt FROM admins WHERE is_active = 1";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("cnt") == 0;
            }
        } catch (SQLException e) {
            System.err.println("⚠️ [SqlAdminManager] Error checking setup status: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
        
        return true;
    }
    
    private static int countAdmins() {
        Connection conn = getConnection();
        if (conn == null) return 0;
        
        String sql = "SELECT COUNT(*) as cnt FROM admins WHERE is_active = 1";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            System.err.println("⚠️ [SqlAdminManager] Error counting admins: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
        
        return 0;
    }
    
    /**
     * Get the currently logged in admin (for logging purposes)
     */
    private static Admin getCurrentAdmin() {
        // This should be implemented based on your session management
        // For now, returns null if no admin is logged in
        return null;
    }
    
    /**
     * Ensures at least one SuperAdmin exists. Called from Main at startup.
     * Creates default superadmin (superadmin / super123) if no admins exist.
     */
    public static void ensureDefaultSuperAdmin() {
        System.out.println("🔧 [SqlAdminManager] Ensuring default SuperAdmin exists...");
        
        if (isFirstSetup()) {
            System.out.println("⚠️ [SqlAdminManager] No admins found - creating default SuperAdmin");
            createDefaultSuperAdmin();
        } else {
            // Check if there's at least one active SuperAdmin
            Connection conn = getConnection();
            if (conn == null) {
                System.out.println("❌ [SqlAdminManager] Database connection failed");
                return;
            }
            
            String sql = "SELECT COUNT(*) as cnt FROM admins WHERE role = ? AND is_active = 1";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, AdminRole.SUPERADMIN);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int superAdminCount = rs.getInt("cnt");
                        if (superAdminCount == 0) {
                            System.out.println("⚠️ [SqlAdminManager] No active SuperAdmin found - creating default");
                            createDefaultSuperAdmin();
                        } else {
                            System.out.println("✅ [SqlAdminManager] " + superAdminCount + " SuperAdmin(s) exist");
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("⚠️ [SqlAdminManager] Error checking SuperAdmin count: " + e.getMessage());
            } finally {
                closeConnection(conn);
            }
        }
    }
    
    /**
     * Validate admin input comprehensively
     */
    private static boolean validateAdminInput(String adminId, String name, String password, String role) {
        List<String> errors = new ArrayList<>();
        
        // Validate admin ID
        if (adminId == null || adminId.trim().isEmpty()) {
            errors.add("Admin ID is required");
        } else if (adminId.length() < 3 || adminId.length() > 50) {
            errors.add("Admin ID must be 3-50 characters");
        } else if (!adminId.matches("^[a-zA-Z0-9_.-]+$")) {
            // Allow letters, numbers, underscores, dots and hyphens to support legacy IDs
            errors.add("Admin ID can only contain letters, numbers, underscores, dots or hyphens");
        }
        
        // Validate name (if provided)
        if (name != null) {
            if (name.trim().isEmpty()) {
                errors.add("Name is required");
            } else if (name.length() < 2 || name.length() > 100) {
                errors.add("Name must be 2-100 characters");
            }
        }
        
        // Validate password (if provided)
        if (password != null) {
            if (password.length() < 8) {
                errors.add("Password must be at least 8 characters");
            } else if (!isPasswordStrong(password)) {
                errors.add("Password must contain at least one uppercase letter, one lowercase letter, and one number");
            }
        }
        
        // Validate role (if provided)
        if (role != null && !AdminRole.isValidRole(role)) {
            errors.add("Invalid role: " + role);
        }
        
        if (!errors.isEmpty()) {
            System.out.println("❌ [SqlAdminManager] Validation errors:");
            for (String error : errors) {
                System.out.println("   - " + error);
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * Check password strength
     */
    private static boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        // Check for at least one uppercase, one lowercase, one digit
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            if (Character.isLowerCase(c)) hasLower = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        
        return hasUpper && hasLower && hasDigit;
    }
    
    /**
     * Migrate all admins from plain text database_admins.txt into SQL.
     * Each migrated admin gets temporary password "Reset123!" and should change on first login.
     * Returns number of admins migrated.
     */
    public static int migrateAllAdminsFromTextFile() {
        System.out.println("🔄 [SqlAdminManager] Migrating admins from text file to SQL...");
        // Backup original files first
        try {
            AdminData.createBackup();
        } catch (Exception e) {
            System.out.println("⚠️ [SqlAdminManager] Could not create admin backup: " + e.getMessage());
        }

        String[] lines = AdminData.getAllAdmins();
        if (lines == null || lines.length == 0) {
            System.out.println("⚠️ [SqlAdminManager] No admins in text file to migrate.");
            return 0;
        }

        String tempPassword = "Reset123!";
        int count = 0;

        for (String line : lines) {
            if (line == null) continue;
            line = line.trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(":");
            if (parts.length < 1) continue;

            String adminId = parts.length >= 1 ? parts[0].trim() : null;
            String name = parts.length >= 2 ? parts[1].trim() : "Imported Admin";
            String storedHash = parts.length >= 3 ? parts[2].trim() : null;
            String role = parts.length >= 4 ? parts[3].trim() : AdminRole.VIEW_ONLY;

            if (adminId == null || adminId.isEmpty()) continue;

            // Normalize role to our constants
            role = AdminRole.normalizeRole(role);

            // Skip if already present in SQL
            try {
                if (adminExists(adminId)) {
                    System.out.println("ℹ️ [SqlAdminManager] Skipping existing admin: " + adminId);
                    continue;
                }

                boolean created = false;
                String salt = AdminData.getAdminSalt(adminId);

                // If the admin record includes a pre-hashed password and we have the salt, preserve them
                if (storedHash != null && !storedHash.isEmpty() && salt != null && !salt.isEmpty()) {
                    created = addAdminWithHash(adminId, name, storedHash, salt, role);
                    if (created) {
                        System.out.println("✅ [SqlAdminManager] Migrated admin (preserved hash): " + adminId);
                    }
                } else {
                    created = addAdmin(adminId, name, tempPassword, role);
                    if (created) {
                        System.out.println("✅ [SqlAdminManager] Migrated admin: " + adminId + " (" + name + ") as " + role);
                    }
                }

                if (created) count++; else System.out.println("❌ [SqlAdminManager] Failed to migrate admin: " + adminId);
            } catch (Exception e) {
                System.out.println("❌ [SqlAdminManager] Error migrating admin " + adminId + ": " + e.getMessage());
            }
        }

        System.out.println("✅ [SqlAdminManager] Migration complete: " + count + " admins migrated");
        System.out.println("⚠️ [SqlAdminManager] All migrated admins have temporary password: " + tempPassword);
        return count;
    }
    
    // ==================== DEBUG & MAINTENANCE ====================
    
    public static void debugCheckAdminsTable() {
        System.out.println("\n🔍 [SqlAdminManager] DEBUG: Checking admins table");
        
        Connection conn = getConnection();
        if (conn == null) {
            System.out.println("❌ No database connection");
            return;
        }
        
        try {
            String sql = "SELECT admin_id, name, role, is_active, created_at FROM admins";
            try (Statement stmt = conn.createStatement(); 
                 ResultSet rs = stmt.executeQuery(sql)) {
                System.out.println("👥 SQL Admin records:");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.println("  • ID: " + rs.getString("admin_id") + 
                                     " | Name: " + rs.getString("name") + 
                                     " | Role: " + rs.getString("role") + 
                                     " | Active: " + rs.getInt("is_active") +
                                     " | Created: " + rs.getTimestamp("created_at"));
                }
                if (!found) {
                    System.out.println("  (No admin records in SQL)");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error checking admins table: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
    }
    
    public static void emergencyResetAllAdminPasswords() {
        System.out.println("🚨 [SqlAdminManager] EMERGENCY: Resetting all admin passwords");
        
        List<Admin> admins = getAllAdmins();
        for (Admin admin : admins) {
            resetAdminPassword(admin.getAdminId(), "Reset123!");
        }
        System.out.println("✅ [SqlAdminManager] All admin passwords reset to: Reset123!");
    }
    
    public static String[] getAllAdminIds() {
        List<Admin> admins = getAllAdmins();
        String[] ids = new String[admins.size()];
        for (int i = 0; i < admins.size(); i++) {
            ids[i] = admins.get(i).getAdminId();
        }
        return ids;
    }
    
    /**
     * Get admin's audit trail
     */
    public static List<String> getAdminAuditTrail(String adminId) {
        List<String> auditLogs = new ArrayList<>();
        Connection conn = getConnection();
        if (conn == null) return auditLogs;
        
        String sql = "SELECT action, details, timestamp FROM audit_logs WHERE admin_id = ? ORDER BY timestamp DESC LIMIT 50";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            try (ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    String log = String.format("[%s] %s - %s", 
                        rs.getTimestamp("timestamp"),
                        rs.getString("action"),
                        rs.getString("details"));
                    auditLogs.add(log);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error retrieving audit trail: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
        
        return auditLogs;
    }
    
    /**
     * Get all audit logs for system monitoring
     */
    public static List<String> getAllAuditLogs(int limit) {
        List<String> auditLogs = new ArrayList<>();
        Connection conn = getConnection();
        if (conn == null) return auditLogs;
        
        String sql = "SELECT admin_id, action, details, timestamp FROM audit_logs ORDER BY timestamp DESC LIMIT ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    String log = String.format("[%s] %s: %s - %s", 
                        rs.getTimestamp("timestamp"),
                        rs.getString("admin_id"),
                        rs.getString("action"),
                        rs.getString("details"));
                    auditLogs.add(log);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error retrieving audit logs: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
        
        return auditLogs;
    }
    
    /**
     * Clear all caches (for testing and maintenance)
     */
    public static void clearAllCaches() {
        adminCache.clear();
        loginAttempts.clear();
        System.out.println("✅ [SqlAdminManager] All caches cleared");
    }
    
    /**
     * Clean up old audit logs
     */
    public static int cleanupOldAuditLogs(int daysToKeep) {
        Connection conn = getConnection();
        if (conn == null) return 0;
        
        String sql = "DELETE FROM audit_logs WHERE timestamp < datetime('now', '-' || ? || ' days')";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, daysToKeep);
            int deleted = stmt.executeUpdate();
            System.out.println("✅ [SqlAdminManager] Cleaned up " + deleted + " old audit logs");
            return deleted;
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error cleaning up audit logs: " + e.getMessage());
            return 0;
        } finally {
            closeConnection(conn);
        }
    }
    
    /**
     * Session Management Methods
     */
    public static String createSession(String adminId, String ipAddress, String userAgent, int timeoutMinutes) {
        Connection conn = getConnection();
        if (conn == null) return null;
        
        String sessionId = UUID.randomUUID().toString();
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + (timeoutMinutes * 60 * 1000));
        
        String sql = "INSERT INTO admin_sessions (session_id, admin_id, ip_address, user_agent, expires_at) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessionId);
            stmt.setString(2, adminId);
            stmt.setString(3, ipAddress);
            stmt.setString(4, userAgent);
            stmt.setTimestamp(5, expiresAt);
            stmt.executeUpdate();
            return sessionId;
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error creating session: " + e.getMessage());
            return null;
        } finally {
            closeConnection(conn);
        }
    }
    
    public static boolean validateSession(String sessionId, String ipAddress) {
        Connection conn = getConnection();
        if (conn == null) return false;
        
        String sql = "SELECT admin_id, expires_at FROM admin_sessions WHERE session_id = ? AND ip_address = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessionId);
            stmt.setString(2, ipAddress);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp expiresAt = rs.getTimestamp("expires_at");
                    if (expiresAt.after(new Timestamp(System.currentTimeMillis()))) {
                        // Update last activity
                        updateSessionActivity(sessionId);
                        return true;
                    } else {
                        // Session expired, delete it
                        deleteSession(sessionId);
                        return false;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error validating session: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
        
        return false;
    }
    
    private static void updateSessionActivity(String sessionId) {
        Connection conn = getConnection();
        if (conn == null) return;
        
        String sql = "UPDATE admin_sessions SET last_activity = CURRENT_TIMESTAMP WHERE session_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error updating session activity: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
    }
    
    public static void deleteSession(String sessionId) {
        Connection conn = getConnection();
        if (conn == null) return;
        
        String sql = "DELETE FROM admin_sessions WHERE session_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error deleting session: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
    }
    
    public static void cleanupExpiredSessions() {
        Connection conn = getConnection();
        if (conn == null) return;
        
        String sql = "DELETE FROM admin_sessions WHERE expires_at < CURRENT_TIMESTAMP";
        
        try (Statement stmt = conn.createStatement()) {
            int deleted = stmt.executeUpdate(sql);
            if (deleted > 0) {
                System.out.println("✅ [SqlAdminManager] Cleaned up " + deleted + " expired sessions");
            }
        } catch (SQLException e) {
            System.err.println("❌ [SqlAdminManager] Error cleaning up sessions: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
    }
}