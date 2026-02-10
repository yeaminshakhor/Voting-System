import Data.DatabaseManager;
import Utils.SecurityUtils;
import java.sql.*;

public class reset_admin_passwords {
    public static void main(String[] args) {
        System.out.println("🔐 Admin Password Reset Utility");
        System.out.println("================================\n");
        
        String newPassword = "Admin@123";
        String salt = SecurityUtils.generateSalt();
        String hash = SecurityUtils.hashPassword(newPassword, salt);
        
        System.out.println("New Password: " + newPassword);
        System.out.println("Salt: " + salt);
        System.out.println("Hash: " + hash);
        System.out.println("\nUpdating all admin passwords...");
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            System.out.println("❌ Cannot connect to database");
            return;
        }
        
        try {
            String sql = "UPDATE admins SET password_hash = ?, salt = ? WHERE role = 'superadmin' LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, hash);
                stmt.setString(2, salt);
                int updated = stmt.executeUpdate();
                System.out.println("✅ Updated " + updated + " superadmin(s)");
                System.out.println("\n✅ All superadmins can now login with:");
                System.out.println("   Password: " + newPassword);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                // Ignore
            }
        }
    }
}
