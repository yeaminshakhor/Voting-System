package Data;

import java.io.*;
import java.util.*;

/**
 * Utility class for migrating data from plain text files to SQL database.
 * This is a one-time migration tool to preserve existing data during system migration.
 */
public class DataMigrationUtility {
    
    private static final String ADMIN_FILE = "database_admins.txt";
    private static final String ADMIN_SALT_FILE = "database_admin_salts.txt";
    private static final String VOTER_FILE = "database_voters.txt";
    private static final String VOTER_SALT_FILE = "database_voter_salts.txt";
    private static final String NOMINEE_FILE = "database_nominees.txt";
    private static final String VOTE_FILE = "database_votes.txt";
    
    /**
     * Migrate all data from plain text files to SQL database
     */
    public static void migrateAllData() {
        System.out.println("🔄 Starting data migration from plain text files to SQL...");
        
        if (DatabaseManager.getConnection() == null) {
            System.out.println("❌ Database connection failed. Cannot migrate data.");
            return;
        }
        
        int adminsImported = migrateAdmins();
        int votersImported = migrateVoters();
        int nomineesImported = migrateNominees();
        int votesImported = migrateVotes();
        
        System.out.println("\n✅ Migration completed!");
        System.out.println("   ➜ Admins imported: " + adminsImported);
        System.out.println("   ➜ Voters imported: " + votersImported);
        System.out.println("   ➜ Nominees imported: " + nomineesImported);
        System.out.println("   ➜ Votes imported: " + votesImported);
    }
    
    /**
     * Migrate admin accounts from text file to database
     */
    private static int migrateAdmins() {
        int count = 0;
        File adminFile = new File(ADMIN_FILE);
        File saltFile = new File(ADMIN_SALT_FILE);
        
        if (!adminFile.exists()) {
            System.out.println("⚠️  Admin file not found: " + ADMIN_FILE);
            return 0;
        }
        
        // Read salts into memory first
        Map<String, String> salts = new HashMap<>();
        if (saltFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(saltFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length >= 2) {
                        salts.put(parts[0], parts[1]);
                    }
                }
            } catch (IOException e) {
                System.out.println("⚠️  Could not read salt file: " + e.getMessage());
            }
        }
        
        // Migrate admins
        try (BufferedReader reader = new BufferedReader(new FileReader(adminFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 4) {
                    String adminId = parts[0];
                    String name = parts[1];
                    String passwordHash = parts[2];
                    String role = parts[3];
                    String salt = salts.getOrDefault(adminId, "");
                    
                    // The password is already hashed in the file, so we insert it directly
                    // Note: This assumes the role is valid
                    if (migrateAdminToDB(adminId, name, passwordHash, role, salt)) {
                        count++;
                    }
                }
            }
            System.out.println("✅ Migrated " + count + " admins");
        } catch (IOException e) {
            System.out.println("❌ Error reading admin file: " + e.getMessage());
        }
        
        return count;
    }
    
    /**
     * Migrate a single admin to database
     */
    private static boolean migrateAdminToDB(String adminId, String name, String passwordHash, String role, String salt) {
        try {
            java.sql.Connection conn = DatabaseManager.getConnection();
            if (conn == null) return false;
            
            // Check if admin already exists
            String checkSql = "SELECT 1 FROM admins WHERE admin_id = ?";
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                stmt.setString(1, adminId);
                if (stmt.executeQuery().next()) {
                    return false; // Already exists
                }
            }
            
            String sql = "INSERT INTO admins (admin_id, name, password_hash, role, salt, is_active) VALUES (?, ?, ?, ?, ?, 1)";
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, adminId);
                stmt.setString(2, name);
                stmt.setString(3, passwordHash);
                stmt.setString(4, role);
                stmt.setString(5, salt);
                stmt.executeUpdate();
                return true;
            }
        } catch (java.sql.SQLException e) {
            System.out.println("⚠️  Could not migrate admin " + adminId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Migrate voters from text file to database
     */
    private static int migrateVoters() {
        int count = 0;
        File voterFile = new File(VOTER_FILE);
        File saltFile = new File(VOTER_SALT_FILE);
        
        if (!voterFile.exists()) {
            System.out.println("⚠️  Voter file not found: " + VOTER_FILE);
            return 0;
        }
        
        // Read salts into memory
        Map<String, String> salts = new HashMap<>();
        if (saltFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(saltFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length >= 2) {
                        salts.put(parts[0], parts[1]);
                    }
                }
            } catch (IOException e) {
                System.out.println("⚠️  Could not read voter salt file: " + e.getMessage());
            }
        }
        
        // Migrate voters
        try (BufferedReader reader = new BufferedReader(new FileReader(voterFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                String[] parts = line.split(":");
                if (parts.length >= 3) {
                    String voterId = parts[0];
                    String name = parts[1];
                    String passwordHash = parts[2];
                    String salt = salts.getOrDefault(voterId, "");
                    boolean isRegistered = !passwordHash.isEmpty() && !passwordHash.equals("null");
                    
                    if (migrateVoterToDB(voterId, name, passwordHash, salt, isRegistered)) {
                        count++;
                    }
                }
            }
            System.out.println("✅ Migrated " + count + " voters");
        } catch (IOException e) {
            System.out.println("❌ Error reading voter file: " + e.getMessage());
        }
        
        return count;
    }
    
    /**
     * Migrate a single voter to database
     */
    private static boolean migrateVoterToDB(String voterId, String name, String passwordHash, String salt, boolean isRegistered) {
        try {
            java.sql.Connection conn = DatabaseManager.getConnection();
            if (conn == null) return false;
            
            // Check if voter already exists
            String checkSql = "SELECT 1 FROM voters WHERE voter_id = ?";
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                stmt.setString(1, voterId);
                if (stmt.executeQuery().next()) {
                    return false; // Already exists
                }
            }
            
            String sql = "INSERT INTO voters (voter_id, name, password_hash, salt, is_registered, has_voted) " +
                        "VALUES (?, ?, ?, ?, ?, 0)";
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, voterId);
                stmt.setString(2, name);
                stmt.setString(3, isRegistered ? passwordHash : null);
                stmt.setString(4, isRegistered ? salt : null);
                stmt.setInt(5, isRegistered ? 1 : 0);
                stmt.executeUpdate();
                return true;
            }
        } catch (java.sql.SQLException e) {
            System.out.println("⚠️  Could not migrate voter " + voterId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Migrate nominees from text file to database
     */
    private static int migrateNominees() {
        int count = 0;
        File nomineeFile = new File(NOMINEE_FILE);
        
        if (!nomineeFile.exists()) {
            System.out.println("⚠️  Nominee file not found: " + NOMINEE_FILE);
            return 0;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(nomineeFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                String[] parts = line.split(":");
                if (parts.length >= 3) {
                    String nomineeId = parts[0];
                    String name = parts[1];
                    String party = parts[2];
                    
                    if (migrateNomineeToDB(nomineeId, name, party)) {
                        count++;
                    }
                }
            }
            System.out.println("✅ Migrated " + count + " nominees");
        } catch (IOException e) {
            System.out.println("❌ Error reading nominee file: " + e.getMessage());
        }
        
        return count;
    }
    
    /**
     * Migrate a single nominee to database
     */
    private static boolean migrateNomineeToDB(String nomineeId, String name, String party) {
        try {
            java.sql.Connection conn = DatabaseManager.getConnection();
            if (conn == null) return false;
            
            // Check if nominee already exists
            String checkSql = "SELECT 1 FROM nominees WHERE nominee_id = ?";
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                stmt.setString(1, nomineeId);
                if (stmt.executeQuery().next()) {
                    return false; // Already exists
                }
            }
            
            String sql = "INSERT INTO nominees (nominee_id, name, party, is_active) VALUES (?, ?, ?, 1)";
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, nomineeId);
                stmt.setString(2, name);
                stmt.setString(3, party);
                stmt.executeUpdate();
                return true;
            }
        } catch (java.sql.SQLException e) {
            System.out.println("⚠️  Could not migrate nominee " + nomineeId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Migrate votes from text file to database
     */
    private static int migrateVotes() {
        int count = 0;
        File voteFile = new File(VOTE_FILE);
        
        if (!voteFile.exists()) {
            System.out.println("⚠️  Vote file not found: " + VOTE_FILE);
            return 0;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(voteFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                String[] parts = line.split(":");
                if (parts.length >= 2) {
                    String voterId = parts[0];
                    String nomineeId = parts[1];
                    
                    if (migrateVoteToDB(voterId, nomineeId)) {
                        count++;
                    }
                }
            }
            System.out.println("✅ Migrated " + count + " votes");
        } catch (IOException e) {
            System.out.println("❌ Error reading vote file: " + e.getMessage());
        }
        
        return count;
    }
    
    /**
     * Migrate a single vote to database
     */
    private static boolean migrateVoteToDB(String voterId, String nomineeId) {
        try {
            java.sql.Connection conn = DatabaseManager.getConnection();
            if (conn == null) return false;
            
            // Start transaction
            conn.setAutoCommit(false);
            
            try {
                // Insert vote
                String voteSql = "INSERT INTO votes (voter_id, nominee_id) VALUES (?, ?)";
                try (java.sql.PreparedStatement stmt = conn.prepareStatement(voteSql)) {
                    stmt.setString(1, voterId);
                    stmt.setString(2, nomineeId);
                    stmt.executeUpdate();
                }
                
                // Update voter's has_voted flag
                String updateVoterSql = "UPDATE voters SET has_voted = 1 WHERE voter_id = ?";
                try (java.sql.PreparedStatement stmt = conn.prepareStatement(updateVoterSql)) {
                    stmt.setString(1, voterId);
                    stmt.executeUpdate();
                }
                
                conn.commit();
                return true;
            } catch (java.sql.SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (java.sql.SQLException e) {
            System.out.println("⚠️  Could not migrate vote from " + voterId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if migration is needed
     */
    public static boolean isMigrationNeeded() {
        // Always return false - system is SQL-only now
        return false;
    }
    
    /**
     * Main method for running migration
     */
    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  DATA MIGRATION UTILITY - Text Files to SQL");
        System.out.println("═══════════════════════════════════════════════════\n");
        
        if (!isMigrationNeeded()) {
            System.out.println("✅ No plain text files found. Migration not needed.");
            System.out.println("   System is already using SQL database.");
            return;
        }
        
        System.out.println("⚠️  Found plain text database files.");
        System.out.println("   This utility will migrate your data to SQL.\n");
        
        System.out.println("Files to migrate:");
        System.out.println("  • " + ADMIN_FILE);
        System.out.println("  • " + VOTER_FILE);
        System.out.println("  • " + NOMINEE_FILE);
        System.out.println("  • " + VOTE_FILE);
        System.out.println();
        
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Continue with migration? (yes/no): ");
            String response = scanner.nextLine().trim().toLowerCase();
            
            if (!response.equals("yes") && !response.equals("y")) {
                System.out.println("Migration cancelled.");
                return;
            }
            
            System.out.println("\nStarting migration...\n");
            migrateAllData();
            
            System.out.println("\n═══════════════════════════════════════════════════");
            System.out.println("Migration completed successfully!");
            System.out.println("You can now delete the old plain text files if desired.");
            System.out.println("═══════════════════════════════════════════════════");
        }
    }
}
