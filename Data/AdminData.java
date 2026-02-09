package Data;

import Entities.Admin;
import Utils.SecurityUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AdminData {
    public static final String ADMIN_FILE = "database_admins.txt";
    public static final String ADMIN_SALT_FILE = "database_admin_salts.txt";
    public static final int MAX_ENTRIES = 1000; // Added missing constant
    
    /**
     * VALIDATE ADMIN - With backward compatibility
     */
    public static boolean validateAdminCredentials(String adminId, String password) {
        System.out.println("🔐 Validating admin: " + adminId);
        
        // Validate inputs
        if (adminId == null || adminId.trim().isEmpty() || password == null) {
            System.out.println("❌ Invalid input parameters");
            return false;
        }
        
        File adminFile = new File(ADMIN_FILE);
        if (!adminFile.exists()) {
            System.out.println("❌ Admin file does not exist: " + ADMIN_FILE);
            return false;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 4 && parts[0].equals(adminId)) {
                    String storedPassword = parts[2];
                    System.out.println("✓ Admin found in database");
                    
                    // Try plaintext first (for very old data)
                    if (storedPassword.equals(password)) {
                        System.out.println("✅ Login successful (plaintext match)");
                        migrateAdminToHashedPassword(adminId, password);
                        return true;
                    }
                    
                    // Try OLD hashing algorithm first (backward compatibility)
                    String salt = getAdminSalt(adminId);
                    if (salt != null && !salt.isEmpty()) {
                        String hashedInputOld = SecurityUtils.hashPasswordOld(password, salt);
                        if (hashedInputOld != null && storedPassword.equals(hashedInputOld)) {
                            System.out.println("✅ Login successful (old hashing match)");
                            return true;
                        }
                        
                        // Try NEW hashing algorithm
                        String hashedInputNew = SecurityUtils.hashPassword(password, salt);
                        if (hashedInputNew != null && storedPassword.equals(hashedInputNew)) {
                            System.out.println("✅ Login successful (new hashing match)");
                            return true;
                        }
                    }
                    
                    System.out.println("❌ Password mismatch");
                    return false;
                }
            }
            System.out.println("❌ Admin ID not found in database: " + adminId);
        } catch (IOException e) {
            System.out.println("❌ File error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Auto-migrate plaintext password to hashed
     */
    private static void migrateAdminToHashedPassword(String adminId, String plainPassword) {
        String salt = SecurityUtils.generateSalt();
        String hashedPassword = SecurityUtils.hashPassword(plainPassword, salt);
        
        if (hashedPassword == null) {
            System.out.println("❌ Failed to hash password for migration");
            return;
        }
        
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 4 && parts[0].equals(adminId)) {
                    lines.add(parts[0] + ":" + parts[1] + ":" + hashedPassword + ":" + parts[3]);
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading admin file for migration: " + e.getMessage());
            return;
        }
        
        // Write back
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ADMIN_FILE))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("❌ Error writing admin file during migration: " + e.getMessage());
            return;
        }
        
        // Save salt
        try (BufferedWriter saltWriter = new BufferedWriter(new FileWriter(ADMIN_SALT_FILE, true))) {
            saltWriter.write(adminId + ":" + salt);
            saltWriter.newLine();
            System.out.println("✅ Auto-migrated admin " + adminId + " to hashed password");
        } catch (IOException e) {
            System.out.println("⚠️ Admin migrated but failed to save salt: " + e.getMessage());
        }
    }
    
    /**
     * Gets the admin's salt from salt file.
     * Returns empty string if salt file doesn't exist (for backward compatibility).
     */
    private static String getAdminSalt(String adminId) {
        File saltFile = new File(ADMIN_SALT_FILE);
        if (!saltFile.exists()) {
            return "";
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_SALT_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 2 && parts[0].equals(adminId)) {
                    return parts[1];
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Salt file error: " + e.getMessage());
        }
        
        return "";
    }

    /**
     * Checks if admin exists in the database.
     */
    private static boolean adminExists(String adminId) {
        if (adminId == null || adminId.trim().isEmpty()) {
            return false;
        }
        
        File adminFile = new File(ADMIN_FILE);
        if (!adminFile.exists()) {
            return false;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 1 && parts[0].equals(adminId)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error checking admin existence: " + e.getMessage());
        }
        return false;
    }

    /**
     * Gets the admin's name by ID.
     */
    public static String getAdminNameById(String adminId) {
        if (adminId == null || adminId.trim().isEmpty()) {
            return null;
        }
        
        System.out.println("👤 Looking for name of: " + adminId);

        try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 4 && parts[0].equals(adminId)) {
                    System.out.println("✅ Found name: " + parts[1]);
                    return parts[1];
                }
            }
        } catch (IOException e) {
            System.out.println("❌ File error: " + e.getMessage());
        }
        System.out.println("❌ Name not found for: " + adminId);
        return null;
    }

    /**
     * Gets the admin's role by ID.
     */
    public static String getRoleById(String adminId) {
        if (adminId == null || adminId.trim().isEmpty()) {
            return null;
        }
        
        System.out.println("🎭 Looking for role of: " + adminId);

        try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 4 && parts[0].equals(adminId)) {
                    System.out.println("✅ Found role: " + parts[3]);
                    return parts[3];
                }
            }
        } catch (IOException e) {
            System.out.println("❌ File error: " + e.getMessage());
        }
        System.out.println("❌ Role not found for: " + adminId);
        return null;
    }

    /**
     * Saves a new admin to the file with password hashing.
     */
    public static boolean saveAdminToFile(Admin admin, String plainPassword) {
        // Validate inputs
        if (admin == null || plainPassword == null) {
            System.out.println("❌ Admin or password is null");
            return false;
        }
        
        String adminId = admin.getAdminId();
        String adminName = admin.getName();
        
        if (!SecurityUtils.isValidId(adminId)) {
            System.out.println("❌ Invalid admin ID format: " + adminId);
            return false;
        }
        
        if (!SecurityUtils.isValidName(adminName)) {
            System.out.println("❌ Invalid admin name: " + adminName);
            return false;
        }
        
        if (plainPassword.length() < 6) {
            System.out.println("❌ Password must be at least 6 characters");
            return false;
        }
        
        // Check if admin already exists
        if (adminExists(adminId)) {
            System.out.println("❌ Admin ID already exists: " + adminId);
            return false;
        }

        // Generate salt and hash password
        String salt = SecurityUtils.generateSalt();
        String hashedPassword = SecurityUtils.hashPassword(plainPassword, salt);
        
        if (hashedPassword == null) {
            System.out.println("❌ Failed to hash password");
            return false;
        }

        try {
            // Create salt file if it doesn't exist
            File saltFile = new File(ADMIN_SALT_FILE);
            if (!saltFile.exists()) {
                saltFile.createNewFile();
            }
            
            // Save salt to separate file
            try (BufferedWriter saltWriter = new BufferedWriter(new FileWriter(ADMIN_SALT_FILE, true))) {
                saltWriter.write(adminId + ":" + salt);
                saltWriter.newLine();
            }
            
            // Save admin with hashed password
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(ADMIN_FILE, true))) {
                writer.write(adminId + ":" + 
                           adminName + ":" + 
                           hashedPassword + ":" + 
                           admin.getRole());
                writer.newLine();
                System.out.println("✅ Admin saved: " + adminId);
                return true;
            }
        } catch (IOException e) {
            System.out.println("❌ Error saving admin: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes an admin by ID.
     */
    public static boolean deleteAdmin(String adminId) {
        if (adminId == null || adminId.trim().isEmpty()) {
            System.out.println("❌ Invalid admin ID");
            return false;
        }
        
        // First, read all admin records
        String[] lines = readFileLines(ADMIN_FILE);
        boolean found = false;
        
        for (int i = 0; i < lines.length; i++) {
            String[] parts = lines[i].split(":");
            if (parts.length >= 4 && parts[0].equals(adminId)) {
                found = true;
                lines[i] = ""; // Mark for deletion
            }
        }
        
        if (!found) {
            System.out.println("❌ Admin not found: " + adminId);
            return false;
        }

        // Write back non-deleted admins
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ADMIN_FILE))) {
            for (String line : lines) {
                if (!line.isEmpty()) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            
            // Also delete from salt file
            deleteAdminSalt(adminId);
            
            System.out.println("✅ Admin deleted: " + adminId);
            return true;
        } catch (IOException e) {
            System.out.println("❌ Error deleting admin: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes admin salt from salt file.
     */
    private static void deleteAdminSalt(String adminId) {
        try {
            String[] lines = readFileLines(ADMIN_SALT_FILE);
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(ADMIN_SALT_FILE))) {
                for (String line : lines) {
                    String[] parts = line.split(":");
                    if (parts.length >= 2 && !parts[0].equals(adminId)) {
                        writer.write(line);
                        writer.newLine();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error deleting admin salt: " + e.getMessage());
        }
    }

    /**
     * Updates an admin's password with hashing.
     * Public method that accepts plain password.
     */
    public static boolean updateAdminPassword(String adminId, String newPlainPassword) {
        if (adminId == null || adminId.trim().isEmpty()) {
            System.out.println("❌ Invalid admin ID");
            return false;
        }
        
        if (newPlainPassword == null || newPlainPassword.length() < 6) {
            System.out.println("❌ Password must be at least 6 characters");
            return false;
        }
        
        // Check if admin exists
        if (!adminExists(adminId)) {
            System.out.println("❌ Admin not found: " + adminId);
            return false;
        }
        
        // Generate new salt and hash
        String newSalt = SecurityUtils.generateSalt();
        String newHashedPassword = SecurityUtils.hashPassword(newPlainPassword, newSalt);
        
        if (newHashedPassword == null) {
            System.out.println("❌ Failed to hash new password");
            return false;
        }
        
        return updateAdminPasswordWithSalt(adminId, newHashedPassword, newSalt);
    }

    /**
     * Internal method to update password with already hashed password and salt.
     */
    private static boolean updateAdminPasswordWithSalt(String adminId, String newHashedPassword, String newSalt) {
        // Update admin record
        String[] lines = readFileLines(ADMIN_FILE);
        boolean found = false;
        
        for (int i = 0; i < lines.length; i++) {
            String[] parts = lines[i].split(":");
            if (parts.length >= 4 && parts[0].equals(adminId)) {
                found = true;
                lines[i] = parts[0] + ":" + parts[1] + ":" + newHashedPassword + ":" + parts[3];
            }
        }
        
        if (!found) {
            System.out.println("❌ Admin not found: " + adminId);
            return false;
        }

        // Write updated records
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ADMIN_FILE))) {
            for (String line : lines) {
                if (!line.isEmpty()) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            
            // Update salt in salt file
            updateAdminSalt(adminId, newSalt);
            
            System.out.println("✅ Password updated for admin: " + adminId);
            return true;
        } catch (IOException e) {
            System.out.println("❌ Error updating password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates admin salt in salt file.
     */
    private static void updateAdminSalt(String adminId, String newSalt) {
        try {
            // Create salt file if it doesn't exist
            File saltFile = new File(ADMIN_SALT_FILE);
            if (!saltFile.exists()) {
                saltFile.createNewFile();
            }
            
            String[] lines = readFileLines(ADMIN_SALT_FILE);
            boolean found = false;
            
            for (int i = 0; i < lines.length; i++) {
                String[] parts = lines[i].split(":");
                if (parts.length >= 2 && parts[0].equals(adminId)) {
                    found = true;
                    lines[i] = adminId + ":" + newSalt;
                }
            }
            
            // If salt not found, add new entry
            if (!found) {
                String[] newLines = new String[lines.length + 1];
                System.arraycopy(lines, 0, newLines, 0, lines.length);
                newLines[lines.length] = adminId + ":" + newSalt;
                lines = newLines;
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(ADMIN_SALT_FILE))) {
                for (String line : lines) {
                    if (line != null && !line.isEmpty()) {
                        writer.write(line);
                        writer.newLine();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error updating salt: " + e.getMessage());
        }
    }

    /**
     * Gets all admin details.
     */
    public static String[] getAllAdmins() {
        return readFileLines(ADMIN_FILE);
    }

    /**
     * Reads file lines into an array.
     */
    private static String[] readFileLines(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return new String[0];
        }
        
        List<String> linesList = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    linesList.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading file: " + filePath + " - " + e.getMessage());
        }
        
        return linesList.toArray(new String[0]);
    }

    /**
     * EMERGENCY FIX: Migrates existing plaintext passwords to hashed format.
     * This should be run ONCE after implementing password hashing.
     */
    public static void migrateToHashedPasswords() {
        System.out.println("🚀 Starting admin password migration...");
        
        try {
            // Create backup first
            createBackup();
            
            // Create salt file if it doesn't exist
            File saltFile = new File(ADMIN_SALT_FILE);
            if (!saltFile.exists()) {
                saltFile.createNewFile();
                System.out.println("✅ Created salt file: " + ADMIN_SALT_FILE);
            }
            
            String[] lines = readFileLines(ADMIN_FILE);
            List<String> updatedLines = new ArrayList<>();
            
            for (String line : lines) {
                if (line == null || line.isEmpty()) {
                    continue;
                }
                
                String[] parts = line.split(":");
                if (parts.length == 4) {
                    String adminId = parts[0];
                    String plainPassword = parts[2];
                    
                    // Skip if already looks hashed (long Base64 string)
                    if (plainPassword.length() <= 20 && !plainPassword.isEmpty()) {
                        // Generate salt and hash
                        String salt = SecurityUtils.generateSalt();
                        String hashedPassword = SecurityUtils.hashPassword(plainPassword, salt);
                        
                        if (hashedPassword != null) {
                            // Update admin record
                            updatedLines.add(parts[0] + ":" + parts[1] + ":" + hashedPassword + ":" + parts[3]);
                            
                            // Save salt
                            try (BufferedWriter saltWriter = new BufferedWriter(new FileWriter(ADMIN_SALT_FILE, true))) {
                                saltWriter.write(adminId + ":" + salt);
                                saltWriter.newLine();
                            }
                            
                            System.out.println("✅ Migrated admin: " + adminId);
                        } else {
                            System.out.println("❌ Failed to hash password for: " + adminId);
                            updatedLines.add(line); // Keep original
                        }
                    } else {
                        System.out.println("⚠️ Skipping (already hashed?): " + adminId);
                        updatedLines.add(line); // Keep original
                    }
                } else {
                    updatedLines.add(line); // Keep original line
                }
            }
            
            // Write back updated records
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(ADMIN_FILE))) {
                for (String line : updatedLines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            
            System.out.println("✅ Admin password migration completed!");
            
        } catch (IOException e) {
            System.out.println("❌ Migration error: " + e.getMessage());
        }
    }

    /**
     * Validates admin ID format.
     */
    public static boolean isValidAdminId(String adminId) {
        return SecurityUtils.isValidId(adminId);
    }

    /**
     * Validates admin name format.
     */
    public static boolean isValidAdminName(String adminName) {
        return SecurityUtils.isValidName(adminName);
    }

    /**
     * EMERGENCY BACKUP: Creates a backup of admin data before migration.
     */
    public static void createBackup() {
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String backupFile = ADMIN_FILE + ".backup." + timestamp;
            
            File sourceFile = new File(ADMIN_FILE);
            if (!sourceFile.exists()) {
                System.out.println("⚠️ No admin file to backup");
                return;
            }
            
            try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_FILE));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(backupFile))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            
            System.out.println("✅ Backup created: " + backupFile);
            
        } catch (IOException e) {
            System.out.println("❌ Error creating backup: " + e.getMessage());
        }
    }
    
    /**
     * Additional utility method: Get all admin IDs
     */
    public static String[] getAllAdminIds() {
        String[] admins = getAllAdmins();
        String[] ids = new String[admins.length];
        
        for (int i = 0; i < admins.length; i++) {
            String[] parts = admins[i].split(":");
            if (parts.length >= 1) {
                ids[i] = parts[0];
            }
        }
        
        return ids;
    }
}