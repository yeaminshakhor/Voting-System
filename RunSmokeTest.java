import Data.DatabaseManager;
import Data.SqlAdminManager;
import Data.SqlElectionDataManager;

public class RunSmokeTest {
    public static void main(String[] args) {
        System.out.println("=== Smoke test: DB init and migrations ===");

        // Initialize DB
        DatabaseManager.initializeDatabase();
        if (DatabaseManager.getConnection() == null) {
            System.out.println("❌ Database connection failed. Check JDBC driver and permissions.");
            return;
        }
        System.out.println("✅ Database connection OK");

        // Migrate admins from text file
        int adminsMigrated = SqlAdminManager.migrateAllAdminsFromTextFile();
        System.out.println("Admins migrated: " + adminsMigrated);

        // Migrate other data
        int otherMigrated = SqlElectionDataManager.migrateAllData() ? 1 : 0;
        System.out.println("Other data migration attempted (success flag): " + otherMigrated);

        // Show current admin count
        try {
            int adminCount = SqlAdminManager.getAllAdmins().size();
            System.out.println("SQL Admins in DB: " + adminCount);
        } catch (Exception e) {
            System.out.println("⚠️ Could not list admins: " + e.getMessage());
        }

        System.out.println("=== Smoke test complete ===");
    }
}
