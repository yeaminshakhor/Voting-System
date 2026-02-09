package Utils;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class SecurityUtils {
    private static final int ITERATIONS = 10000; // Increase iterations for better security
    
    /**
     * Hash password using SHA-256 with multiple iterations (key stretching).
     * More secure than single-pass hashing.
     */
    public static String hashPassword(String password, String salt) {
        try {
            // Validate inputs
            if (password == null || password.isEmpty()) {
                System.err.println("❌ Error: Password cannot be null or empty");
                return null;
            }
            
            if (salt == null || salt.isEmpty()) {
                System.err.println("⚠️ Warning: Salt is null or empty, using fallback");
                // Fallback: hash password with SHA-256 only
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = password.getBytes();
                for (int i = 0; i < ITERATIONS; i++) {
                    md.reset();
                    hash = md.digest(hash);
                }
                return Base64.getEncoder().encodeToString(hash);
            }
            
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            // Combine salt and password
            byte[] input = new byte[saltBytes.length + password.getBytes().length];
            System.arraycopy(saltBytes, 0, input, 0, saltBytes.length);
            System.arraycopy(password.getBytes(), 0, input, saltBytes.length, password.getBytes().length);
            
            // Apply hashing multiple times for key stretching
            byte[] hash = input;
            for (int i = 0; i < ITERATIONS; i++) {
                md.reset();
                hash = md.digest(hash);
            }
            
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            System.err.println("❌ Error in hashPassword: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Hash password using OLD algorithm (single-pass SHA-256).
     * Used for backward compatibility with existing hashes.
     */
    public static String hashPasswordOld(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedBytes = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generate a cryptographically secure random salt (256-bit).
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[32]; // 256-bit salt
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    // Input validation
    public static boolean isValidId(String id) {
        return id != null && !id.trim().isEmpty() && 
               id.matches("[a-zA-Z0-9\\-]+") && id.length() <= 20;
    }
    
    public static boolean isValidName(String name) {
        // Allow letters, spaces, hyphens, and apostrophes for better name support
        return name != null && !name.trim().isEmpty() && 
               name.matches("[a-zA-Z\\s\\-']+") && name.length() <= 100;
    }
    
    public static String sanitizeInput(String input) {
        if (input == null) return "";
        return input.trim()
                   .replace(":", "")  // Remove colon to prevent injection
                   .replace("\n", "")
                   .replace("\r", "");
    }

    public static boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) return false;
        // Password should have mix of uppercase, lowercase, and numbers
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        return hasUpper && hasLower && hasDigit;
    }

    /**
     * Generate hash for empty/unregistered password.
     * Used to mark voters that need to self-register.
     */
    public static String generateEmptyPasswordHash() {
        return "__UNREGISTERED__"; // Marker for voters imported from portal
    }

    /**
     * Check if a voter is unregistered (empty password).
     */
    public static boolean isUnregistered(String passwordHash) {
        return "__UNREGISTERED__".equals(passwordHash);
    }
}