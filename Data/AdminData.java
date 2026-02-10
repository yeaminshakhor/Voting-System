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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class AdminData {
    public static final String ADMIN_FILE = "database_admins.txt";
    public static final String ADMIN_SALT_FILE = "database_admin_salts.txt";
    public static final int MAX_ENTRIES = 1000;
    
    /**
     * VALIDATE ADMIN - Updated to handle pre-hashed passwords in text file
     * Format: adminId:Name:PasswordHash:Role
     */
    public static boolean validateAdminCredentials(String adminId, String password) {
        System.out.println("🔐 [AdminData] Validating admin: " + adminId);
        
        // Validate inputs
        if (adminId == null || adminId.trim().isEmpty() || password == null) {
            System.out.println("❌ [AdminData] Invalid input parameters");
            return false;
        }
        
        File adminFile = new File(ADMIN_FILE);
        if (!adminFile.exists()) {
            System.out.println("❌ [AdminData] Admin file does not exist: " + ADMIN_FILE);
            return false;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Parse: adminId:Name:PasswordHash:Role
                String[] parts = line.split(":");
                if (parts.length >= 4 && parts[0].equals(adminId)) {
                    String storedHash = parts[2]; // This is already a Base64 hash from old system
                    String storedRole = parts[3];
                    
                    System.out.println("✅ [AdminData] Admin found: " + adminId + " | Role: " + storedRole);
                    System.out.println("🔐 [AdminData] Stored hash (Base64): " + storedHash.substring(0, Math.min(20, storedHash.length())) + "...");
                    
                    // Check if this is a legacy Base64 hash (like in your file)
                    if (isBase64(storedHash)) {
                        System.out.println("⚠️ [AdminData] Detected Base64 hash - using legacy validation");
                        
                        // Get the salt for this admin
                        String salt = getAdminSalt(adminId);
                        System.out.println("🔐 [AdminData] Retrieved salt: " + (salt != null && !salt.isEmpty() ? "Yes" : "No"));
                        
                        if (salt != null && !salt.isEmpty()) {
                            // Try to validate with current hashing algorithm
                            String inputHash = SecurityUtils.hashPassword(password, salt);
                            
                            if (inputHash != null) {
                                System.out.println("🔐 [AdminData] Input hash generated");
                                System.out.println("🔐 [AdminData] Comparing: " + (inputHash.equals(storedHash) ? "MATCH" : "NO MATCH"));
                                
                                if (inputHash.equals(storedHash)) {
                                    System.out.println("✅ [AdminData] Login successful (hash match)");
                                    return true;
                                }
                            }
                        }
                        
                        // If we reach here, hash didn't match
                        System.out.println("❌ [AdminData] Password mismatch or salt issue");
                        
                        // Last resort: check if password matches the hash directly (for debugging)
                        // This should normally not be true since storedHash is hashed
                        if (storedHash.equals(password)) {
                            System.out.println("⚠️ [AdminData] Direct password match (debug mode)");
                            return true;
                        }
                        
                        return false;
                    } else {
                        // This is plain text password (old system)
                        System.out.println("⚠️ [AdminData] Plain text password detected");
                        if (storedHash.equals(password)) {
                            System.out.println("✅ [AdminData] Login successful (plaintext match)");
                            // Migrate to hashed password
                            migrateAdminToHashedPassword(adminId, password);
                            return true;
                        }
                        return false;
                    }
                }
            }
            System.out.println("❌ [AdminData] Admin ID not found in database: " + adminId);
        } catch (IOException e) {
            System.out.println("❌ [AdminData] File error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Check if string is valid Base64
     */
    private static boolean isBase64(String str) {
        if (str == null || str.isEmpty()) return false;
        
        // Base64 strings are usually longer and contain specific characters
        String base64Pattern = "^[A-Za-z0-9+/]+[=]{0,2}$";
        return str.matches(base64Pattern) && str.length() >= 20;
    }
    
    /**
     * Get admin salt from salt file (public for migration purposes)
     */
    public static String getAdminSalt(String adminId) {
        File saltFile = new File(ADMIN_SALT_FILE);
        if (!saltFile.exists()) {
            System.out.println("⚠️ [AdminData] Salt file does not exist: " + ADMIN_SALT_FILE);
            return "";
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_SALT_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split(":");
                if (parts.length >= 2 && parts[0].equals(adminId)) {
                    return parts[1];
                }
            }
        } catch (IOException e) {
            System.out.println("❌ [AdminData] Error reading salt file: " + e.getMessage());
        }
        
        System.out.println("⚠️ [AdminData] No salt found for admin: " + adminId);
        return "";
    }
    
    /**
     * Auto-migrate plaintext password to hashed
     */
    private static void migrateAdminToHashedPassword(String adminId, String plainPassword) {
        System.out.println("🔄 [AdminData] Migrating admin to hashed password: " + adminId);
        
        String salt = SecurityUtils.generateSalt();
        String hashedPassword = SecurityUtils.hashPassword(plainPassword, salt);
        
        if (hashedPassword == null) {
            System.out.println("❌ [AdminData] Failed to hash password for migration");
            return;
        }
        
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 4 && parts[0].equals(adminId)) {
                    // Replace plain text password with hash
                    lines.add(parts[0] + ":" + parts[1] + ":" + hashedPassword + ":" + parts[3]);
                    System.out.println("✅ [AdminData] Updated password for: " + adminId);
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("❌ [AdminData] Error reading admin file for migration: " + e.getMessage());
            return;
        }
        
        // Write back updated file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ADMIN_FILE))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("❌ [AdminData] Error writing admin file during migration: " + e.getMessage());
            return;
        }
        
        // Save salt to salt file
        try (BufferedWriter saltWriter = new BufferedWriter(new FileWriter(ADMIN_SALT_FILE, true))) {
            saltWriter.write(adminId + ":" + salt);
            saltWriter.newLine();
            System.out.println("✅ [AdminData] Auto-migrated admin " + adminId + " to hashed password");
        } catch (IOException e) {
            System.out.println("⚠️ [AdminData] Admin migrated but failed to save salt: " + e.getMessage());
        }
    }
    
    /**
     * Checks if admin exists in the database
     */
    public static boolean adminExists(String adminId) {
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
                line = line.trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split(":");
                if (parts.length >= 1 && parts[0].equals(adminId)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("❌ [AdminData] Error checking admin existence: " + e.getMessage());
        }
        return false;
    }

    /**
     * Gets the admin's name by ID
     */
    public static String getAdminNameById(String adminId) {
        if (adminId == null || adminId.trim().isEmpty()) {
            return null;
        }
        
        System.out.println("👤 [AdminData] Looking for name of: " + adminId);

        try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split(":");
                if (parts.length >= 4 && parts[0].equals(adminId)) {
                    System.out.println("✅ [AdminData] Found name: " + parts[1]);
                    return parts[1];
                }
            }
        } catch (IOException e) {
            System.out.println("❌ [AdminData] File error: " + e.getMessage());
        }
        System.out.println("❌ [AdminData] Name not found for: " + adminId);
        return null;
    }

    /**
     * Gets the admin's role by ID
     */
    public static String getRoleById(String adminId) {
        if (adminId == null || adminId.trim().isEmpty()) {
            return null;
        }
        
        System.out.println("🎭 [AdminData] Looking for role of: " + adminId);

        try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split(":");
                if (parts.length >= 4 && parts[0].equals(adminId)) {
                    String role = parts[3];
                    System.out.println("✅ [AdminData] Found role: " + role);
                    return role;
                }
            }
        } catch (IOException e) {
            System.out.println("❌ [AdminData] File error: " + e.getMessage());
        }
        System.out.println("❌ [AdminData] Role not found for: " + adminId);
        return null;
    }

    /**
     * Saves a new admin to the file with password hashing
     */
    public static boolean saveAdminToFile(Admin admin, String plainPassword) {
        // Validate inputs
        if (admin == null || plainPassword == null) {
            System.out.println("❌ [AdminData] Admin or password is null");
            return false;
        }
        
        String adminId = admin.getAdminId();
        String adminName = admin.getName();
        
        if (!SecurityUtils.isValidId(adminId)) {
            System.out.println("❌ [AdminData] Invalid admin ID format: " + adminId);
            return false;
        }
        
        if (!SecurityUtils.isValidName(adminName)) {
            System.out.println("❌ [AdminData] Invalid admin name: " + adminName);
            return false;
        }
        
        if (plainPassword.length() < 6) {
            System.out.println("❌ [AdminData] Password must be at least 6 characters");
            return false;
        }
        
        // Check if admin already exists
        if (adminExists(adminId)) {
            System.out.println("❌ [AdminData] Admin ID already exists: " + adminId);
            return false;
        }

        // Generate salt and hash password
        String salt = SecurityUtils.generateSalt();
        String hashedPassword = SecurityUtils.hashPassword(plainPassword, salt);
        
        if (hashedPassword == null) {
            System.out.println("❌ [AdminData] Failed to hash password");
            return false;
        }

        try {
            // Create salt file if it doesn't exist
            File saltFile = new File(ADMIN_SALT_FILE);
            if (!saltFile.exists()) {
                saltFile.createNewFile();
                System.out.println("✅ [AdminData] Created salt file");
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
                System.out.println("✅ [AdminData] Admin saved: " + adminId + " | Role: " + admin.getRole());
                return true;
            }
        } catch (IOException e) {
            System.out.println("❌ [AdminData] Error saving admin: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes an admin by ID
     */
    public static boolean deleteAdmin(String adminId) {
        if (adminId == null || adminId.trim().isEmpty()) {
            System.out.println("❌ [AdminData] Invalid admin ID");
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
            System.out.println("❌ [AdminData] Admin not found: " + adminId);
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
            
            System.out.println("✅ [AdminData] Admin deleted: " + adminId);
            return true;
        } catch (IOException e) {
            System.out.println("❌ [AdminData] Error deleting admin: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes admin salt from salt file
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
            System.out.println("❌ [AdminData] Error deleting admin salt: " + e.getMessage());
        }
    }

    /**
     * Updates an admin's password with hashing
     */
    public static boolean updateAdminPassword(String adminId, String newPlainPassword) {
        if (adminId == null || adminId.trim().isEmpty()) {
            System.out.println("❌ [AdminData] Invalid admin ID");
            return false;
        }
        
        if (newPlainPassword == null || newPlainPassword.length() < 6) {
            System.out.println("❌ [AdminData] Password must be at least 6 characters");
            return false;
        }
        
        // Check if admin exists
        if (!adminExists(adminId)) {
            System.out.println("❌ [AdminData] Admin not found: " + adminId);
            return false;
        }
        
        // Generate new salt and hash
        String newSalt = SecurityUtils.generateSalt();
        String newHashedPassword = SecurityUtils.hashPassword(newPlainPassword, newSalt);
        
        if (newHashedPassword == null) {
            System.out.println("❌ [AdminData] Failed to hash new password");
            return false;
        }
        
        return updateAdminPasswordWithSalt(adminId, newHashedPassword, newSalt);
    }

    /**
     * Internal method to update password with already hashed password and salt
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
            System.out.println("❌ [AdminData] Admin not found: " + adminId);
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
            
            System.out.println("✅ [AdminData] Password updated for admin: " + adminId);
            return true;
        } catch (IOException e) {
            System.out.println("❌ [AdminData] Error updating password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates admin salt in salt file
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
            System.out.println("❌ [AdminData] Error updating salt: " + e.getMessage());
        }
    }

    /**
     * Gets all admin details
     */
    public static String[] getAllAdmins() {
        return readFileLines(ADMIN_FILE);
    }

    /**
     * Reads file lines into an array
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
                line = line.trim();
                if (!line.isEmpty()) {
                    linesList.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("❌ [AdminData] Error reading file: " + filePath + " - " + e.getMessage());
        }
        
        return linesList.toArray(new String[0]);
    }

    /**
     * Debug method to view admin file contents
     */
    public static void printAdminFileContents() {
        System.out.println("\n📋 [AdminData] Contents of " + ADMIN_FILE + ":");
        System.out.println("=" .repeat(80));
        
        try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_FILE))) {
            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                System.out.print(lineNum + ": ");
                String[] parts = line.split(":");
                if (parts.length >= 4) {
                    System.out.print("ID: " + parts[0] + " | ");
                    System.out.print("Name: " + parts[1] + " | ");
                    System.out.print("Password hash: " + 
                        (parts[2].length() > 20 ? parts[2].substring(0, 20) + "..." : parts[2]) + " | ");
                    System.out.print("Role: " + parts[3]);
                } else {
                    System.out.print("MALFORMED LINE: " + line);
                }
                System.out.println();
                lineNum++;
            }
        } catch (IOException e) {
            System.out.println("❌ [AdminData] Error reading admin file: " + e.getMessage());
        }
        System.out.println("=" .repeat(80));
    }

    /**
     * Emergency method to reset a specific admin's password
     */
    public static boolean emergencyPasswordReset(String adminId, String newPassword) {
        System.out.println("🚨 [AdminData] EMERGENCY: Resetting password for admin: " + adminId);
        
        if (!adminExists(adminId)) {
            System.out.println("❌ [AdminData] Admin not found");
            return false;
        }
        
        String salt = SecurityUtils.generateSalt();
        String hashedPassword = SecurityUtils.hashPassword(newPassword, salt);
        
        if (hashedPassword == null) {
            System.out.println("❌ [AdminData] Failed to hash password");
            return false;
        }
        
        // Read all lines
        List<String> lines = new ArrayList<>();
        boolean updated = false;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split(":");
                if (parts.length >= 4 && parts[0].equals(adminId)) {
                    // Update this line
                    lines.add(parts[0] + ":" + parts[1] + ":" + hashedPassword + ":" + parts[3]);
                    updated = true;
                    System.out.println("✅ [AdminData] Updated password in file");
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("❌ [AdminData] Error reading admin file: " + e.getMessage());
            return false;
        }
        
        if (!updated) {
            System.out.println("❌ [AdminData] Admin not found in file");
            return false;
        }
        
        // Write back
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ADMIN_FILE))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("❌ [AdminData] Error writing admin file: " + e.getMessage());
            return false;
        }
        
        // Update salt
        try (BufferedWriter saltWriter = new BufferedWriter(new FileWriter(ADMIN_SALT_FILE, true))) {
            saltWriter.write(adminId + ":" + salt);
            saltWriter.newLine();
            System.out.println("✅ [AdminData] Updated salt for admin: " + adminId);
        } catch (IOException e) {
            System.out.println("⚠️ [AdminData] Updated password but failed to save salt: " + e.getMessage());
        }
        
        System.out.println("✅ [AdminData] Emergency password reset complete for: " + adminId);
        System.out.println("🔐 New password: " + newPassword);
        return true;
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

    /**
     * Create backups of admin files (admins and salts) with timestamp suffix.
     */
    public static boolean createBackup() {
        try {
            String ts = String.valueOf(System.currentTimeMillis());
            File adminFile = new File(ADMIN_FILE);
            if (adminFile.exists()) {
                Files.copy(Paths.get(ADMIN_FILE), Paths.get(ADMIN_FILE + ".backup." + ts), StandardCopyOption.REPLACE_EXISTING);
            }

            File saltFile = new File(ADMIN_SALT_FILE);
            if (saltFile.exists()) {
                Files.copy(Paths.get(ADMIN_SALT_FILE), Paths.get(ADMIN_SALT_FILE + ".backup." + ts), StandardCopyOption.REPLACE_EXISTING);
            }

            System.out.println("✅ [AdminData] Backup created with timestamp: " + ts);
            return true;
        } catch (IOException e) {
            System.out.println("❌ [AdminData] Failed to create backup: " + e.getMessage());
            return false;
        }
    }

    /**
     * Migrate any plaintext admin passwords in the admin file to hashed form.
     * Returns the number of admins migrated.
     */
    public static int migrateToHashedPasswords() {
        String[] lines = readFileLines(ADMIN_FILE);
        if (lines == null || lines.length == 0) return 0;

        int migrated = 0;

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) continue;
            String[] parts = line.split(":");
            if (parts.length >= 4) {
                String adminId = parts[0];
                String storedPass = parts[2];

                // If not Base64 (legacy) and seems like plaintext, migrate
                if (!isBase64(storedPass)) {
                    System.out.println("🔄 [AdminData] Migrating admin password for: " + adminId);
                    migrateAdminToHashedPassword(adminId, storedPass);
                    migrated++;
                }
            }
        }

        System.out.println("✅ [AdminData] Migration complete. Total migrated: " + migrated);
        return migrated;
    }
}