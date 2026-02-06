package Data;

import Entities.Admin;
import java.io.*;

/**
 * Manages admin data, like login credentials and roles, stored in files.
 */
public class AdminData {
    public static final String ADMIN_FILE = "database_admins.txt";
    private static final int MAX_ENTRIES = 100;

    /**
     * Checks if admin ID and password match.
     */
    public static boolean validateAdminCredentials(String adminId, String password) {
        System.out.println("🔐 Validating: " + adminId);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 4 && parts[0].equals(adminId) && parts[2].equals(password)) {
                    System.out.println("✅ Login successful for: " + adminId);
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("❌ File error: " + e.getMessage());
        }
        System.out.println("❌ Login failed for: " + adminId);
        return false;
    }

    /**
     * Gets the admin's name by ID.
     */
    public static String getAdminNameById(String adminId) {
        System.out.println("👤 Looking for name of: " + adminId);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 4 && parts[0].equals(adminId)) {
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
        System.out.println("🎭 Looking for role of: " + adminId);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 4 && parts[0].equals(adminId)) {
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
     * Saves a new admin to the file.
     */
    public static boolean saveAdminToFile(Admin admin) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ADMIN_FILE, true))) {
            writer.write(admin.getAdminId() + ":" + admin.getName() + ":" + admin.getPassword() + ":" + admin.getRole());
            writer.newLine();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes an admin by ID.
     */
    public static boolean deleteAdmin(String adminId) {
        String[] lines = readFileLines(ADMIN_FILE);
        boolean found = false;
        for (int i = 0; i < lines.length; i++) {
            String[] parts = lines[i].split(":");
            if (parts.length == 4 && parts[0].equals(adminId)) {
                found = true;
                lines[i] = "";
            }
        }
        if (!found) return false;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ADMIN_FILE))) {
            for (String line : lines) {
                if (!line.isEmpty()) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates an admin's password.
     */
    public static boolean updateAdminPassword(String adminId, String newPassword) {
        String[] lines = readFileLines(ADMIN_FILE);
        boolean found = false;
        for (int i = 0; i < lines.length; i++) {
            String[] parts = lines[i].split(":");
            if (parts.length == 4 && parts[0].equals(adminId)) {
                found = true;
                lines[i] = parts[0] + ":" + parts[1] + ":" + newPassword + ":" + parts[3];
            }
        }
        if (!found) return false;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ADMIN_FILE))) {
            for (String line : lines) {
                if (!line.isEmpty()) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets all admin details.
     */
    public static String[] getAllAdmins() {
        String[] admins = new String[MAX_ENTRIES];
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(ADMIN_FILE))) {
            String line;
            while ((line = reader.readLine()) != null && count < MAX_ENTRIES) {
                admins[count++] = line;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return trimArray(admins, count);
    }

    /**
     * Reads file lines into an array.
     */
    private static String[] readFileLines(String filePath) {
        String[] lines = new String[MAX_ENTRIES];
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null && count < MAX_ENTRIES) {
                lines[count++] = line;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return trimArray(lines, count);
    }

    /**
     * Trims an array to remove null entries.
     */
    private static String[] trimArray(String[] array, int size) {
        String[] trimmed = new String[size];
        for (int i = 0; i < size; i++) {
            trimmed[i] = array[i];
        }
        return trimmed;
    }
}