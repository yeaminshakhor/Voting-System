package Utils;

import Data.DatabaseManager;
import java.net.InetAddress;
import java.sql.*;

public class AuditLogger {
    private static final String LOG_TABLE = "audit_logs";
    
    public static void log(String userId, String action, String details) {
        // Database might not be available - use file-based logging instead
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            System.out.println("📝 [AUDIT LOG] User: " + userId + ", Action: " + action + ", Details: " + details);
            return;
        }
        
        String sql = "INSERT INTO " + LOG_TABLE + " " +
                    "(user_id, action, details, ip_address) " +
                    "VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            pstmt.setString(2, action);
            pstmt.setString(3, details);
            pstmt.setString(4, getClientIP());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("⚠️ Error logging to database: " + e.getMessage());
        }
    }
    
    public static void logAdminAction(String adminId, String action, String details) {
        log("ADMIN:" + adminId, action, details);
    }
    
    public static void logVoterAction(String voterId, String action, String details) {
        log("VOTER:" + voterId, action, details);
    }
    
    public static void logSystemAction(String action, String details) {
        log("SYSTEM", action, details);
    }
    
    private static String getClientIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "Unknown";
        }
    }
}