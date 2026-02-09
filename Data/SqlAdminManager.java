package Data;

import Entities.Admin;
import Utils.SecurityUtils;
import Utils.AdminRole;
import java.sql.*;
import java.util.*;

/**
 * SQL-based Admin Manager - Replaces plain text file storage.
 * All admin operations are now in SQLite database.
 * SuperAdmin can be created on first setup or migrated from legacy data.
 */
public class SqlAdminManager {
    
    /**
     * Validate admin credentials from database
     */
    public static boolean validateAdminCredentials(String adminId, String password) {
        System.out.println("🔐 Validating admin: " + adminId);
        
        if (adminId == null || adminId.trim().isEmpty() || password == null) {
            System.out.println("❌ Invalid input parameters");
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            System.out.println("❌ Database not available");
            return false;
        }
        
        String sql = "SELECT password_hash, salt FROM admins WHERE admin_id = ? AND is_active = 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                String salt = rs.getString("salt");
                
                // Hash the input password with the stored salt
                String inputHash = SecurityUtils.hashPassword(password, salt);
                
                if (inputHash != null && inputHash.equals(storedHash)) {
                    System.out.println("✅ Login successful for admin: " + adminId);
                    // Update last login
                    updateLastLogin(adminId);
                    return true;
                } else {
                    System.out.println("❌ Password mismatch for admin: " + adminId);
                }
            } else {
                System.out.println("❌ Admin not found or inactive in SQL: " + adminId);
            }
        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
        }
        
        // Fallback: try to authenticate against legacy plain text admin file
        try {
            if (AdminData.validateAdminCredentials(adminId, password)) {
                System.out.println("✅ Legacy admin authenticated via text file, migrating to SQL...");
                if (migrateLegacyAdminToSql(adminId, password)) {
                    updateLastLogin(adminId);
                }
                return true;
            }
        } catch (Exception ex) {
            System.out.println("⚠️ Legacy admin fallback failed: " + ex.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get admin by ID
     */
    public static Admin getAdminById(String adminId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return null;
        
        String sql = "SELECT admin_id, name, role FROM admins WHERE admin_id = ? AND is_active = 1";
        
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
            System.out.println("❌ Error retrieving admin: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get admin name by ID
     */
    public static String getAdminNameById(String adminId) {
        Admin admin = getAdminById(adminId);
        return admin != null ? admin.getName() : null;
    }
    
    /**
     * Get admin role by ID
     */
    public static String getRoleById(String adminId) {
        Admin admin = getAdminById(adminId);
        return admin != null ? admin.getRole() : null;
    }
    
    /**
     * Check if admin has a specific permission
     */
    public static boolean hasPermission(String adminId, String permission) {
        String role = getRoleById(adminId);
        if (role == null) return false;
        return AdminRole.hasPermission(role, permission);
    }
    
    /**
     * Add a new admin to the database (ADMIN CANNOT ADD ADMIN - only during setup)
     */
    public static boolean addAdmin(String adminId, String name, String password, String role) {
        // Validate role
        if (!AdminRole.isValidRole(role)) {
            System.out.println("❌ Invalid role: " + role);
            return false;
        }
        
        // Validate inputs
        if (!SecurityUtils.isValidId(adminId)) {
            System.out.println("❌ Invalid admin ID format");
            return false;
        }
        
        if (!SecurityUtils.isValidName(name)) {
            System.out.println("❌ Invalid admin name");
            return false;
        }
        
        if (password == null || password.length() < 6) {
            System.out.println("❌ Password must be at least 6 characters");
            return false;
        }
        
        // Check if admin already exists
        if (adminExists(adminId)) {
            System.out.println("❌ Admin ID already exists: " + adminId);
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            System.out.println("❌ Database not available");
            return false;
        }
        
        // Generate salt and hash password
        String salt = SecurityUtils.generateSalt();
        String passwordHash = SecurityUtils.hashPassword(password, salt);
        
        if (passwordHash == null) {
            System.out.println("❌ Failed to hash password");
            return false;
        }
        
        // Get permissions for the role
        Set<String> perms = AdminRole.getPermissions(role);
        String permissionsJson = String.join(",", perms);
        
        String sql = "INSERT INTO admins (admin_id, name, password_hash, role, salt, permissions, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 1)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            stmt.setString(2, name);
            stmt.setString(3, passwordHash);
            stmt.setString(4, role);
            stmt.setString(5, salt);
            stmt.setString(6, permissionsJson);
            
            stmt.executeUpdate();
            System.out.println("✅ Admin added successfully: " + adminId + " (" + role + ")");
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Error adding admin: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete an admin from the database
     */
    public static boolean deleteAdmin(String adminId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "UPDATE admins SET is_active = 0 WHERE admin_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("✅ Admin deactivated: " + adminId);
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Error deleting admin: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get all active admins
     */
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
            System.out.println("❌ Error retrieving admins: " + e.getMessage());
        }
        
        return admins;
    }
    
    /**
     * Check if admin exists
     */
    public static boolean adminExists(String adminId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "SELECT 1 FROM admins WHERE admin_id = ? AND is_active = 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            System.out.println("❌ Error checking admin existence: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update admin password
     */
    public static boolean updateAdminPassword(String adminId, String oldPassword, String newPassword) {
        if (!validateAdminCredentials(adminId, oldPassword)) {
            System.out.println("❌ Current password is incorrect");
            return false;
        }
        
        if (newPassword == null || newPassword.length() < 6) {
            System.out.println("❌ New password must be at least 6 characters");
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String salt = SecurityUtils.generateSalt();
        String newHash = SecurityUtils.hashPassword(newPassword, salt);
        
        if (newHash == null) {
            System.out.println("❌ Failed to hash new password");
            return false;
        }
        
        String sql = "UPDATE admins SET password_hash = ?, salt = ? WHERE admin_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newHash);
            stmt.setString(2, salt);
            stmt.setString(3, adminId);
            
            stmt.executeUpdate();
            System.out.println("✅ Password updated for admin: " + adminId);
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Error updating password: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update last login timestamp
     */
    private static void updateLastLogin(String adminId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return;
        
        String sql = "UPDATE admins SET last_login = CURRENT_TIMESTAMP WHERE admin_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("⚠️ Could not update last login: " + e.getMessage());
        }
    }
    
    /**
     * Log admin action to audit trail
     */
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
            System.out.println("⚠️ Could not log action: " + e.getMessage());
        }
    }
    
    /**
     * Check if this is the first admin setup
     */
    public static boolean isFirstSetup() {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return true;
        
        String sql = "SELECT COUNT(*) as cnt FROM admins WHERE is_active = 1";
        
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                // First setup only if there are NO admins at all
                return rs.getInt("cnt") == 0;
            }
        } catch (SQLException e) {
            System.out.println("⚠️ Error checking setup status: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * Check if at least one SuperAdmin exists in the system.
     */
    public static boolean superAdminExists() {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "SELECT COUNT(*) AS cnt FROM admins WHERE role = ? AND is_active = 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, AdminRole.SUPERADMIN);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("cnt") > 0;
            }
        } catch (SQLException e) {
            System.out.println("⚠️ Error checking superadmin existence: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Ensure there is at least one default SuperAdmin in the system.
     * If none exists, creates: ID = "superadmin", Password = "super123".
     * The SuperAdmin can later change this ID/password from the admin UI.
     */
    public static void ensureDefaultSuperAdmin() {
        try {
            if (superAdminExists()) {
                System.out.println("ℹ️ SuperAdmin already exists. No default account created.");
                return;
            }
            String defaultId = "superadmin";
            String defaultName = "System Administrator";
            String defaultPassword = "super123";
            boolean created = addAdmin(defaultId, defaultName, defaultPassword, AdminRole.SUPERADMIN);
            if (created) {
                System.out.println("✅ Default SuperAdmin created: ID='superadmin', Password='super123'");
            } else {
                System.out.println("⚠️ Failed to create default SuperAdmin. Check database connection and constraints.");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error ensuring default SuperAdmin: " + e.getMessage());
        }
    }
    
    /**
     * Migrate a single legacy admin (from text file) into the SQL database.
     * Uses the supplied plain-text password and current role/name from AdminData.
     */
    private static boolean migrateLegacyAdminToSql(String adminId, String plainPassword) {
        String name = AdminData.getAdminNameById(adminId);
        String role = AdminData.getRoleById(adminId);
        
        if (name == null || role == null) {
            System.out.println("⚠️ Legacy admin details not found for: " + adminId);
            return false;
        }
        
        if (!AdminRole.isValidRole(role)) {
            System.out.println("⚠️ Legacy admin has invalid role '" + role + "', defaulting to " + AdminRole.VOTER_MANAGER);
            role = AdminRole.VOTER_MANAGER;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            System.out.println("❌ Database connection not available for legacy migration");
            return false;
        }
        
        String salt = SecurityUtils.generateSalt();
        String passwordHash = SecurityUtils.hashPassword(plainPassword, salt);
        if (passwordHash == null) {
            System.out.println("❌ Failed to hash password for legacy admin migration");
            return false;
        }
        
        try {
            // If admin already exists in SQL, just update its password/role/salt
            if (adminExists(adminId)) {
                String updateSql = "UPDATE admins SET name = ?, password_hash = ?, role = ?, salt = ?, is_active = 1 WHERE admin_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setString(1, name);
                    stmt.setString(2, passwordHash);
                    stmt.setString(3, role);
                    stmt.setString(4, salt);
                    stmt.setString(5, adminId);
                    stmt.executeUpdate();
                }
            } else {
                // Insert new admin row
                String insertSql = "INSERT INTO admins (admin_id, name, password_hash, role, salt, is_active) VALUES (?, ?, ?, ?, ?, 1)";
                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    stmt.setString(1, adminId);
                    stmt.setString(2, name);
                    stmt.setString(3, passwordHash);
                    stmt.setString(4, role);
                    stmt.setString(5, salt);
                    stmt.executeUpdate();
                }
            }
            
            System.out.println("✅ Legacy admin migrated to SQL: " + adminId + " (" + role + ")");
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Error migrating legacy admin to SQL: " + e.getMessage());
            return false;
        }
    }
    
    // ========== SUPERADMIN-SPECIFIC METHODS ==========
    
    /**
     * SuperAdmin adds a new admin with specified role
     */
    public static boolean addAdminBySuper(String superAdminId, String newAdminId, String adminName, String password, String role) {
        // Verify SuperAdmin has permission
        Admin superAdmin = getAdminById(superAdminId);
        if (superAdmin == null || !superAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ Only SuperAdmin can add other admins");
            logAdminAction(superAdminId, "UNAUTHORIZED_ADMIN_ADD", "Attempted to add admin without SuperAdmin role");
            return false;
        }
        
        // Verify role is valid
        if (!AdminRole.isValidRole(role)) {
            System.out.println("❌ Invalid role: " + role);
            return false;
        }
        
        // Use existing addAdmin method, but log it
        boolean success = addAdmin(newAdminId, adminName, password, role);
        if (success) {
            logAdminAction(superAdminId, "ADMIN_ADDED", "SuperAdmin added new admin: " + newAdminId + " with role: " + role);
        }
        return success;
    }
    
    /**
     * SuperAdmin deletes an admin
     */
    public static boolean deleteAdminBySuper(String superAdminId, String targetAdminId) {
        // Verify SuperAdmin
        Admin superAdmin = getAdminById(superAdminId);
        if (superAdmin == null || !superAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ Only SuperAdmin can delete admins");
            logAdminAction(superAdminId, "UNAUTHORIZED_ADMIN_DELETE", "Attempted to delete admin without SuperAdmin role");
            return false;
        }
        
        // Cannot delete self
        if (superAdminId.equals(targetAdminId)) {
            System.out.println("❌ SuperAdmin cannot delete itself");
            return false;
        }
        
        // Cannot delete another SuperAdmin
        Admin targetAdmin = getAdminById(targetAdminId);
        if (targetAdmin != null && targetAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ Cannot delete another SuperAdmin");
            return false;
        }
        
        // Use existing deleteAdmin method
        boolean success = deleteAdmin(targetAdminId);
        if (success) {
            logAdminAction(superAdminId, "ADMIN_DELETED", "SuperAdmin deleted admin: " + targetAdminId);
        }
        return success;
    }
    
    /**
     * SuperAdmin reassigns a role to an admin
     */
    public static boolean reassignAdminRoleBySuper(String superAdminId, String targetAdminId, String newRole) {
        // Verify SuperAdmin
        Admin superAdmin = getAdminById(superAdminId);
        if (superAdmin == null || !superAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ Only SuperAdmin can reassign roles");
            logAdminAction(superAdminId, "UNAUTHORIZED_ROLE_CHANGE", "Attempted to change role without SuperAdmin role");
            return false;
        }
        
        // Verify role is valid
        if (!AdminRole.isValidRole(newRole)) {
            System.out.println("❌ Invalid role: " + newRole);
            return false;
        }
        
        // Cannot change SuperAdmin role
        Admin targetAdmin = getAdminById(targetAdminId);
        if (targetAdmin != null && targetAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ Cannot change SuperAdmin role");
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            System.out.println("❌ Database connection failed");
            return false;
        }
        
        String sql = "UPDATE admins SET role = ? WHERE admin_id = ? AND is_active = 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newRole);
            stmt.setString(2, targetAdminId);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                logAdminAction(superAdminId, "ROLE_REASSIGNED", "SuperAdmin changed role for " + targetAdminId + " to: " + newRole);
                System.out.println("✅ Role updated for admin: " + targetAdminId);
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * SuperAdmin changes another admin's password
     */
    public static boolean changeAdminPasswordBySuper(String superAdminId, String targetAdminId, String newPassword) {
        // Verify SuperAdmin
        Admin superAdmin = getAdminById(superAdminId);
        if (superAdmin == null || !superAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ Only SuperAdmin can change admin passwords");
            logAdminAction(superAdminId, "UNAUTHORIZED_PASSWORD_CHANGE", "Attempted to change password without SuperAdmin role");
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            System.out.println("❌ Database connection failed");
            return false;
        }
        
        String salt = SecurityUtils.generateSalt();
        String passwordHash = SecurityUtils.hashPassword(newPassword, salt);
        
        if (passwordHash == null) {
            System.out.println("❌ Failed to hash password");
            return false;
        }
        
        String sql = "UPDATE admins SET password_hash = ?, salt = ? WHERE admin_id = ? AND is_active = 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, passwordHash);
            stmt.setString(2, salt);
            stmt.setString(3, targetAdminId);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                logAdminAction(superAdminId, "PASSWORD_RESET_BY_SUPER", "SuperAdmin reset password for admin: " + targetAdminId);
                System.out.println("✅ Password updated for admin: " + targetAdminId);
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get all admins (for SuperAdmin to view)
     */
    public static List<Admin> getAllAdmins(String superAdminId) {
        // Verify SuperAdmin
        Admin superAdmin = getAdminById(superAdminId);
        if (superAdmin == null || !superAdmin.getRole().equals(AdminRole.SUPERADMIN)) {
            System.out.println("❌ Only SuperAdmin can view all admins");
            logAdminAction(superAdminId, "UNAUTHORIZED_VIEW_ADMINS", "Attempted to view all admins without SuperAdmin role");
            return new ArrayList<>();
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return new ArrayList<>();
        
        List<Admin> admins = new ArrayList<>();
        String sql = "SELECT admin_id, name, role, is_active, created_date, last_login FROM admins ORDER BY created_date ASC";
        
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
            System.out.println("❌ Error retrieving all admins: " + e.getMessage());
        }
        
        return admins;
    }}