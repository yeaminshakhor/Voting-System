import Data.DatabaseManager;
import Data.SqlAdminManager;

public class TestAdminAuth {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  Testing Admin Authentication System");
        System.out.println("========================================\n");
        
        // Initialize database
        DatabaseManager.initializeDatabase();
        
        // Test admins from database_admins.txt
        String[] testAdmins = {
            "24-59145-3:123456",  // Yeamin
            "24-59144-3:123456",  // Talha
            "1111:111111",         // Jalal Bhai
            "2222:222222",         // Kaka
        };
        
        System.out.println("Testing admin logins with legacy credentials...\n");
        
        for (String testCase : testAdmins) {
            String[] parts = testCase.split(":");
            String adminId = parts[0];
            String password = parts[1];
            
            System.out.println("Test: " + adminId + " with password: " + password);
            boolean success = SqlAdminManager.validateAdminCredentials(adminId, password);
            System.out.println("Result: " + (success ? "✅ SUCCESS" : "❌ FAILED"));
            System.out.println();
        }
        
        // Also test with strong password
        System.out.println("\nTesting password reset to strong password...");
        boolean resetSuccess = SqlAdminManager.forgotPassword("24-59145-3", "NewPass@123");
        System.out.println("Reset result: " + (resetSuccess ? "✅ SUCCESS" : "❌ FAILED"));
        
        // Test new login with new password
        if (resetSuccess) {
            System.out.println("\nTesting login with new password...");
            boolean newLoginSuccess = SqlAdminManager.validateAdminCredentials("24-59145-3", "NewPass@123");
            System.out.println("New login result: " + (newLoginSuccess ? "✅ SUCCESS" : "❌ FAILED"));
        }
    }
}
