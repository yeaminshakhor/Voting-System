import Data.AdminData;

public class RunAdminFix {
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("   ADMIN LOGIN EMERGENCY FIX TOOL");
        System.out.println("==========================================");
        
        // Step 1: Create backup
        System.out.println("\n📁 Step 1: Creating backup...");
        AdminData.createBackup();
        
        // Step 2: Migrate passwords
        System.out.println("\n🔐 Step 2: Migrating passwords to hashed format...");
        AdminData.migrateToHashedPasswords();
        
        System.out.println("\n==========================================");
        System.out.println("✅ FIX COMPLETE!");
        System.out.println("Admins can now login with their original passwords.");
        System.out.println("Example admin IDs from your file:");
        System.out.println("  - 24-59145-3 (password: 123456)");
        System.out.println("  - superadmin (password: super123)");
        System.out.println("  - 1111 (password: 111111)");
        System.out.println("==========================================");
    }
}