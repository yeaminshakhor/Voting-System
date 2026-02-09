package Data;

import Entities.Admin;
import Utils.SecurityUtils;
import Utils.AdminRole;
import java.sql.*;
import Data.AdminData;
import java.util.*;

public class SqlAdminManager {
    
    // ==================== SYSTEM INITIALIZATION ====================
    
    /**
     * Initialize the admin system - called once at application startup
     */
    public static void initializeAdminSystem() {
        System.out.println("\n🔧 [SqlAdminManager] Initializing Admin System...");
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            System.out.println("❌ [SqlAdminManager] Database connection failed");
            return;
        }
        
        try {
            // First, ensure the admins table exists
            createAdminTableIfNotExists(conn);
            
            // Check if any admin exists
            if (isFirstSetup()) {
                System.out.println("⚠️ [SqlAdminManager] No admins found - Creating default SuperAdmin");
                createDefaultSuperAdmin();
            } else {
                int adminCount = countAdmins();
                System.out.println("✅ [SqlAdminManager] System ready with " + adminCount + " admin(s)");
            }
            
        } catch (SQLException e) {
            System.out.println("❌ [SqlAdminManager] System initialization failed: " + e.getMessage());
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
                     "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                     "last_login TIMESTAMP," +
                     "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("✅ [SqlAdminManager] Admins table verified/created");
        }
        
        // Create audit_logs table
        String auditSql = "CREATE TABLE IF NOT EXISTS audit_logs (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "admin_id VARCHAR(50) NOT NULL," +
                         "action VARCHAR(100) NOT NULL," +
                         "details TEXT," +
                         "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(auditSql);
        }
    }
    
    /**
     * Create default SuperAdmin (superadmin/super123) ONLY if no admins exist
     */
    private static void createDefaultSuperAdmin() {
        String adminId = "superadmin";
        String name = "System Administrator";
        String password = "super123";
        
        if (!adminExists(adminId)) {
            boolean created = addAdmin(adminId, name, password, AdminRole.SUPERADMIN);
            if (created) {
                System.out.println("✅ [SqlAdminManager] Default SuperAdmin created:");
                System.out.println("   ID: superadmin");
                System.out.println("   Password: super123");
                System.out.println("⚠️  Please change password after first login!");
                
                // Log this system event
                logAdminAction(adminId, "SYSTEM_INIT", "Default SuperAdmin created on first setup");
            } else {
                System.out.println("❌ [SqlAdminManager] Failed to create default SuperAdmin");
            }
        } else {
            System.out.println("ℹ️ [SqlAdminManager] SuperAdmin already exists");
        }
    }
    
    // ==================== AUTHENTICATION ====================
    
    public static boolean validateAdminCredentials(String adminId, String password) {
        System.out.println("🔐 [SqlAdminManager] Validating admin: " + adminId);
        
        if (adminId == null || adminId.trim().isEmpty() || password == null) {
            System.out.println("❌ [SqlAdminManager] Invalid input parameters");
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            System.out.println("❌ [SqlAdminManager] Database not available");
            return false;
        }
        
        String sql = "SELECT password_hash, salt, needs_password_reset FROM admins WHERE admin_id = ? AND is_active = 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                String salt = rs.getString("salt");
                int needsReset = rs.getInt("needs_password_reset");
                
                System.out.println("✅ [SqlAdminManager] Admin found in database");
                
                String inputHash = SecurityUtils.hashPassword(password, salt);
                
                if (inputHash != null && inputHash.equals(storedHash)) {
                    System.out.println("✅ [SqlAdminManager] Login successful for: " + adminId);
                    updateLastLogin(adminId);
                    
                    if (needsReset == 1) {
                        clearPasswordResetFlag(adminId);
                        System.out.println("⚠️ [SqlAdminManager] Password reset flag cleared");
                    }
                    
                    // Log successful login
                    logAdminAction(adminId, "LOGIN_SUCCESS", "Admin logged in successfully");
                    return true;
                } else {
                    System.out.println("❌ [SqlAdminManager] Password mismatch for: " + adminId);
                    logAdminAction(adminId, "LOGIN_FAILED", "Incorrect password entered");
                    return false;
                }
            } else {
                System.out.println("❌ [SqlAdminManager] Admin not found or inactive: " + adminId);
                return false;
            }
        } catch (SQLException e) {
            System.out.println("❌ [SqlAdminManager] Database error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * FORGOT PASSWORD - Reset password without knowing old password
     */
    public static boolean forgotPassword(String adminId, String newPassword) {
        System.out.println("🔐 [SqlAdminManager] Processing forgot password for: " + adminId);
        
        if (adminId == null || adminId.trim().isEmpty()) {
            System.out.println("❌ [SqlAdminManager] Admin ID is required");
            return false;
        }
        
        if (newPassword == null || newPassword.length() < 6) {
            System.out.println("❌ [SqlAdminManager] New password must be at least 6 characters");
            return false;
        }
        
        // Verify admin exists
        if (!adminExists(adminId)) {
            System.out.println("❌ [SqlAdminManager] Admin ID not found: " + adminId);
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            System.out.println("❌ [SqlAdminManager] Database not available");
            return false;
        }
        
        String salt = SecurityUtils.generateSalt();
        String newHash = SecurityUtils.hashPassword(newPassword, salt);
        
        if (newHash == null) {
            System.out.println("❌ [SqlAdminManager] Failed to hash new password");
            return false;
        }
        
        String sql = "UPDATE admins SET password_hash = ?, salt = ?, needs_password_reset = 1 WHERE admin_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newHash);
            stmt.setString(2, salt);
            stmt.setString(3, adminId);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ [SqlAdminManager] Password reset successful for: " + adminId);
                System.out.println("⚠️ [SqlAdminManager] Admin must use new password on next login");
                
                // Log password reset
                logAdminAction(adminId, "PASSWORD_RESET", "Password reset via forgot password");
                return true;
            } else {
                System.out.println("❌ [SqlAdminManager] Failed to reset password for: " + adminId);
                return false;
            }
        } catch (SQLException e) {
            System.out.println("❌ [SqlAdminManager] Database error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Validate admin exists (for forgot password verification)
     */
    public static boolean validateAdminExists(String adminId) {
        return adminExists(adminId);
    }
    
    private static void clearPasswordResetFlag(String adminId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return;
        
        String sql = "UPDATE admins SET needs_password_reset = 0 WHERE admin_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("⚠️ [SqlAdminManager] Could not clear password reset flag: " + e.getMessage());
        }
    }
    
    private static void updateLastLogin(String adminId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return;
        
        String sql = "UPDATE admins SET last_login = CURRENT_TIMESTAMP WHERE admin_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("⚠️ [SqlAdminManager] Could not update last login: " + e.getMessage());
        }
    }
    
    // ==================== ADMIN CRUD OPERATIONS ====================
    
    public static Admin getAdminById(String adminId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return null;
        
        String sql = "SELECT admin_id, name, role, needs_password_reset FROM admins WHERE admin_id = ? AND is_active = 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Admin admin = new Admin();
                admin.setAdminId(rs.getString("admin_id"));
                admin.setName(rs.getString("name"));
                admin.setRole(rs.getString("role"));
                return admin;
            }
        } catch (SQLException e) {
            System.out.println("❌ [SqlAdminManager] Error retrieving admin: " + e.getMessage());
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
        if (!AdminRole.isValidRole(role)) {
            System.out.println("❌ [SqlAdminManager] Invalid role: " + role);
            return false;
        }
        
        if (!SecurityUtils.isValidId(adminId)) {
            System.out.println("❌ [SqlAdminManager] Invalid admin ID format");
            return false;
        }
        
        if (!SecurityUtils.isValidName(name)) {
            System.out.println("❌ [SqlAdminManager] Invalid admin name");
            return false;
        }
        
        if (password == null || password.length() < 6) {
            System.out.println("❌ [SqlAdminManager] Password must be at least 6 characters");
            return false;
        }
        
        if (adminExists(adminId)) {
            System.out.println("❌ [SqlAdminManager] Admin ID already exists: " + adminId);
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            System.out.println("❌ [SqlAdminManager] Database not available");
            return false;
        }
        
        String salt = SecurityUtils.generateSalt();
        String passwordHash = SecurityUtils.hashPassword(password, salt);
        
        if (passwordHash == null) {
            System.out.println("❌ [SqlAdminManager] Failed to hash password");
            return false;
        }
        
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
            System.out.println("✅ [SqlAdminManager] Admin added: " + adminId + " (" + role + ")");
            
            // Log admin creation
            String creator = "system";
            Admin currentAdmin = getCurrentAdmin();
            if (currentAdmin != null) {
                creator = currentAdmin.getAdminId();
            }
            logAdminAction(creator, "ADMIN_CREATED", "Created admin: " + adminId + " with role: " + role);
            
            return true;
        } catch (SQLException e) {
            System.out.println("❌ [SqlAdminManager] Error adding admin: " + e.getMessage());
            return false;
        }
    }
    
    public static boolean deleteAdmin(String adminId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "UPDATE admins SET is_active = 0 WHERE admin_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("✅ [SqlAdminManager] Admin deactivated: " + adminId);
                
                // Log deletion
                String deleter = "system";
                Admin currentAdmin = getCurrentAdmin();
                if (currentAdmin != null) {
                    deleter = currentAdmin.getAdminId();
                }
                logAdminAction(deleter, "ADMIN_DEACTIVATED", "Deactivated admin: " + adminId);
                
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ [SqlAdminManager] Error deleting admin: " + e.getMessage());
        }
        
        return false;
    }
    
    public static List<Admin> getAllAdmins() {
        List<Admin> admins = new ArrayList<>();
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return admins;
        
        String sql = "SELECT admin_id, name, role FROM admins WHERE is_active = 1 ORDER BY created_at DESC";
        
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Admin admin = new Admin();
                admin.setAdminId(rs.getString("admin_id"));
                admin.setName(rs.getString("name"));
                admin.setRole(rs.getString("role"));
                admins.add(admin);
            }
        } catch (SQLException e) {
            System.out.println("❌ [SqlAdminManager] Error retrieving admins: " + e.getMessage());
        }
        
        return admins;
    }
    
    public static boolean adminExists(String adminId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "SELECT 1 FROM admins WHERE admin_id = ? AND is_active = 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            System.out.println("❌ [SqlAdminManager] Error checking admin existence: " + e.getMessage());
            return false;
        }
    }
    
    public static boolean updateAdminPassword(String adminId, String oldPassword, String newPassword) {
        if (!validateAdminCredentials(adminId, oldPassword)) {
            System.out.println("❌ [SqlAdminManager] Current password is incorrect");
            return false;
        }
        
        if (newPassword == null || newPassword.length() < 6) {
            System.out.println("❌ [SqlAdminManager] New password must be at least 6 characters");
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String salt = SecurityUtils.generateSalt();
        String newHash = SecurityUtils.hashPassword(newPassword, salt);
        
        if (newHash == null) {
            System.out.println("❌ [SqlAdminManager] Failed to hash new password");
            return false;
        }
        
        String sql = "UPDATE admins SET password_hash = ?, salt = ?, needs_password_reset = 0 WHERE admin_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newHash);
            stmt.setString(2, salt);
            stmt.setString(3, adminId);
            
            stmt.executeUpdate();
            System.out.println("✅ [SqlAdminManager] Password updated for admin: " + adminId);
            
            // Log password change
            logAdminAction(adminId, "PASSWORD_CHANGED", "Admin changed their password");
            
            return true;
        } catch (SQLException e) {
            System.out.println("❌ [SqlAdminManager] Error updating password: " + e.getMessage());
            return false;
        }
    }
    
    public static boolean resetAdminPassword(String adminId, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            System.out.println("❌ [SqlAdminManager] New password must be at least 6 characters");
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String salt = SecurityUtils.generateSalt();
        String newHash = SecurityUtils.hashPassword(newPassword, salt);
        
        if (newHash == null) {
            System.out.println("❌ [SqlAdminManager] Failed to hash new password");
            return false;
        }
        
        String sql = "UPDATE admins SET password_hash = ?, salt = ?, needs_password_reset = 1 WHERE admin_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newHash);
            stmt.setString(2, salt);
            stmt.setString(3, adminId);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ [SqlAdminManager] Password reset for admin: " + adminId);
                System.out.println("⚠️ [SqlAdminManager] Admin must reset password on next login");
                
                // Log password reset
                String resetter = "system";
                Admin currentAdmin = getCurrentAdmin();
                if (currentAdmin != null) {
                    resetter = currentAdmin.getAdminId();
                }
                logAdminAction(resetter, "PASSWORD_RESET_BY_ADMIN", "Password reset for admin: " + adminId);
                
                return true;
            } else {
                System.out.println("❌ [SqlAdminManager] Admin not found: " + adminId);
                return false;
            }
        } catch (SQLException e) {
            System.out.println("❌ [SqlAdminManager] Error resetting password: " + e.getMessage());
            return false;
        }
    }
    
    // ==================== AUDIT LOGGING ====================
    
    public static void logAdminAction(String adminId, String action, String details) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return;
        
        String sql = "INSERT INTO audit_logs (admin_id, action, details) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            stmt.setString(2, action);
            stmt.setString(3, details);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("⚠️ [SqlAdminManager] Could not log action: " + e.getMessage());
        }
    }
    
    // ==================== SUPERADMIN-SPECIFIC METHODS ====================
    
    public static boolean addAdminBySuper(String superAdminId, String newAdminId, String adminName, String password, String role) {
        Admin superAdmin = getAdminById(superAdminId);
        if (superAdmin == null || !superAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ [SqlAdminManager] Only SuperAdmin can add other admins");
            logAdminAction(superAdminId, "UNAUTHORIZED_ADMIN_ADD", "Attempted to add admin without SuperAdmin role");
            return false;
        }
        
        if (!AdminRole.isValidRole(role)) {
            System.out.println("❌ [SqlAdminManager] Invalid role: " + role);
            return false;
        }
        
        boolean success = addAdmin(newAdminId, adminName, password, role);
        if (success) {
            logAdminAction(superAdminId, "ADMIN_ADDED", "SuperAdmin added new admin: " + newAdminId + " with role: " + role);
        }
        return success;
    }
    
    public static boolean deleteAdminBySuper(String superAdminId, String targetAdminId) {
        Admin superAdmin = getAdminById(superAdminId);
        if (superAdmin == null || !superAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ [SqlAdminManager] Only SuperAdmin can delete admins");
            logAdminAction(superAdminId, "UNAUTHORIZED_ADMIN_DELETE", "Attempted to delete admin without SuperAdmin role");
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
            logAdminAction(superAdminId, "ADMIN_DELETED", "SuperAdmin deleted admin: " + targetAdminId);
        }
        return success;
    }
    
    public static boolean reassignAdminRoleBySuper(String superAdminId, String targetAdminId, String newRole) {
        Admin superAdmin = getAdminById(superAdminId);
        if (superAdmin == null || !superAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ [SqlAdminManager] Only SuperAdmin can reassign roles");
            logAdminAction(superAdminId, "UNAUTHORIZED_ROLE_CHANGE", "Attempted to change role without SuperAdmin role");
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
        
        Connection conn = DatabaseManager.getConnection();
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
                logAdminAction(superAdminId, "ROLE_REASSIGNED", "SuperAdmin changed role for " + targetAdminId + " to: " + newRole);
                System.out.println("✅ [SqlAdminManager] Role updated for admin: " + targetAdminId);
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ [SqlAdminManager] Database error: " + e.getMessage());
        }
        
        return false;
    }
    
    public static boolean changeAdminPasswordBySuper(String superAdminId, String targetAdminId, String newPassword) {
        Admin superAdmin = getAdminById(superAdminId);
        if (superAdmin == null || !superAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ [SqlAdminManager] Only SuperAdmin can change admin passwords");
            logAdminAction(superAdminId, "UNAUTHORIZED_PASSWORD_CHANGE", "Attempted to change password without SuperAdmin role");
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
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
                logAdminAction(superAdminId, "PASSWORD_RESET_BY_SUPER", "SuperAdmin reset password for admin: " + targetAdminId);
                System.out.println("✅ [SqlAdminManager] Password updated for admin: " + targetAdminId);
                System.out.println("⚠️ [SqlAdminManager] Admin must reset password on next login");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ [SqlAdminManager] Database error: " + e.getMessage());
        }
        
        return false;
    }
    
    public static List<Admin> getAllAdmins(String superAdminId) {
        Admin superAdmin = getAdminById(superAdminId);
        if (superAdmin == null || !superAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ [SqlAdminManager] Only SuperAdmin can view all admins");
            logAdminAction(superAdminId, "UNAUTHORIZED_VIEW_ADMINS", "Attempted to view all admins without SuperAdmin role");
            return new ArrayList<>();
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return new ArrayList<>();
        
        List<Admin> admins = new ArrayList<>();
        String sql = "SELECT admin_id, name, role, is_active, created_at, last_login, needs_password_reset FROM admins ORDER BY created_at ASC";
        
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Admin admin = new Admin();
                admin.setAdminId(rs.getString("admin_id"));
                admin.setName(rs.getString("name"));
                admin.setRole(rs.getString("role"));
                admins.add(admin);
            }
        } catch (SQLException e) {
            System.out.println("❌ [SqlAdminManager] Error retrieving all admins: " + e.getMessage());
        }
        
        return admins;
    }
    
    // ==================== UTILITY METHODS ====================
    
    private static boolean isFirstSetup() {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return true;
        
        String sql = "SELECT COUNT(*) as cnt FROM admins WHERE is_active = 1";
        
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt("cnt") == 0;
            }
        } catch (SQLException e) {
            System.out.println("⚠️ [SqlAdminManager] Error checking setup status: " + e.getMessage());
        }
        
        return true;
    }
    
    private static int countAdmins() {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return 0;
        
        String sql = "SELECT COUNT(*) as cnt FROM admins WHERE is_active = 1";
        
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            System.out.println("⚠️ [SqlAdminManager] Error counting admins: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Get the currently logged in admin (for logging purposes)
     */
    private static Admin getCurrentAdmin() {
        // This is a simplified version - you might want to track current session differently
        // For now, returns null if no admin is logged in
        return null;
    }
    
    /**
     * Ensures at least one SuperAdmin exists. Called from Main at startup.
     * Creates default superadmin (superadmin / super123) if no admins exist.
     */
    public static void ensureDefaultSuperAdmin() {
        if (isFirstSetup()) {
            createDefaultSuperAdmin();
        } else {
            System.out.println("ℹ️ [SqlAdminManager] Admins already exist; default SuperAdmin not created.");
        }
    }
    
    /**
     * Migrate all admins from plain text database_admins.txt into SQL.
     * Each migrated admin gets temporary password "Reset123!" and should change on first login.
     * Returns number of admins migrated.
     */
    public static int migrateAllAdminsFromTextFile() {
        String[] lines = AdminData.getAllAdmins();
        if (lines == null || lines.length == 0) {
            System.out.println("⚠️ [SqlAdminManager] No admins in text file to migrate.");
            return 0;
        }
        String tempPassword = "Reset123!";
        int count = 0;
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) continue;
            String[] parts = line.split(":");
            if (parts.length >= 4) {
                String adminId = parts[0].trim();
                String name = parts[1].trim();
                String role = parts[3].trim();
                if (!AdminRole.isValidRole(role)) {
                    role = AdminRole.VOTER_MANAGER;
                }
                if (!adminExists(adminId) && addAdmin(adminId, name, tempPassword, role)) {
                    count++;
                }
            }
        }
        System.out.println("✅ [SqlAdminManager] Migrated " + count + " admins from text file (password: " + tempPassword + ")");
        return count;
    }
    
    // ==================== DEBUG & MAINTENANCE ====================
    
    public static void debugCheckAdminsTable() {
        System.out.println("\n🔍 [SqlAdminManager] DEBUG: Checking admins table");
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            System.out.println("❌ No database connection");
            return;
        }
        
        try {
            String sql = "SELECT admin_id, name, role, is_active, created_at FROM admins";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
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
            System.out.println("❌ Error checking admins table: " + e.getMessage());
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
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return auditLogs;
        
        String sql = "SELECT action, details, timestamp FROM audit_logs WHERE admin_id = ? ORDER BY timestamp DESC LIMIT 50";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String log = String.format("[%s] %s - %s", 
                    rs.getTimestamp("timestamp"),
                    rs.getString("action"),
                    rs.getString("details"));
                auditLogs.add(log);
            }
        } catch (SQLException e) {
            System.out.println("❌ [SqlAdminManager] Error retrieving audit trail: " + e.getMessage());
        }
        
        return auditLogs;
    }
}