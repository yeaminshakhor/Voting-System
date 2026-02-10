package Utils;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class SecurityUtils {
    private static final int ITERATIONS = 10000; // New: multiple iterations
    
    /**
     * Hash password using current algorithm (multiple iterations for key stretching).
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
                return hashPasswordLegacy(password, ""); // Fallback to legacy
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
     * LEGACY: Hash password using single-pass SHA-256 (for your existing hashes).
     * This matches the hashes in your database_admins.txt file.
     */
    public static String hashPasswordLegacy(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            if (salt != null && !salt.isEmpty()) {
                // If salt is provided, combine salt + password
                byte[] combined = new byte[salt.getBytes().length + password.getBytes().length];
                System.arraycopy(salt.getBytes(), 0, combined, 0, salt.getBytes().length);
                System.arraycopy(password.getBytes(), 0, combined, salt.getBytes().length, password.getBytes().length);
                byte[] hashedBytes = md.digest(combined);
                return Base64.getEncoder().encodeToString(hashedBytes);
            } else {
                // No salt (original system might not have used salt)
                byte[] hashedBytes = md.digest(password.getBytes());
                return Base64.getEncoder().encodeToString(hashedBytes);
            }
        } catch (Exception e) {
            System.err.println("❌ Error in hashPasswordLegacy: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * OLD method for backward compatibility (kept for existing code).
     */
    public static String hashPasswordOld(String password, String salt) {
        return hashPasswordLegacy(password, salt);
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
    
    /**
     * Generate a short salt (for legacy compatibility).
     */
    public static String generateLegacySalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16]; // 128-bit salt
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    /**
     * Validate password against stored hash (supports both legacy and new).
     * This is the key method for your migration.
     */
    public static boolean validatePassword(String password, String storedHash, String salt) {
        if (password == null || storedHash == null) {
            return false;
        }
        
        System.out.println("🔐 [SecurityUtils] Validating password");
        System.out.println("🔐 [SecurityUtils] Stored hash: " + 
            (storedHash.length() > 20 ? storedHash.substring(0, 20) + "..." : storedHash));
        System.out.println("🔐 [SecurityUtils] Salt provided: " + (salt != null && !salt.isEmpty()));
        
        // Try new algorithm first (with multiple iterations)
        String newHash = hashPassword(password, salt);
        if (newHash != null && newHash.equals(storedHash)) {
            System.out.println("✅ [SecurityUtils] Password matches (new algorithm)");
            return true;
        }
        
        // Try legacy algorithm (single-pass SHA-256)
        String legacyHash = hashPasswordLegacy(password, salt);
        if (legacyHash != null && legacyHash.equals(storedHash)) {
            System.out.println("✅ [SecurityUtils] Password matches (legacy algorithm)");
            return true;
        }
        
        // Try legacy without salt (in case original system didn't use salt)
        if (salt != null && !salt.isEmpty()) {
            String legacyNoSaltHash = hashPasswordLegacy(password, "");
            if (legacyNoSaltHash != null && legacyNoSaltHash.equals(storedHash)) {
                System.out.println("✅ [SecurityUtils] Password matches (legacy no-salt)");
                return true;
            }
        }
        
        // Direct comparison (for debugging)
        if (password.equals(storedHash)) {
            System.out.println("⚠️ [SecurityUtils] Direct match (plain text hash?)");
            return true;
        }
        
        System.out.println("❌ [SecurityUtils] Password validation failed");
        return false;
    }
    
    /**
     * Detect if a hash is from the legacy system.
     * Legacy hashes are 44 characters (single SHA-256).
     * New hashes are also 44 characters but use different algorithm.
     */
    public static boolean isLegacyHash(String hash) {
        if (hash == null || hash.length() != 44) {
            return false;
        }
        
        // Check if it's Base64 encoded SHA-256 (44 chars)
        try {
            // Try to decode as Base64
            byte[] decoded = Base64.getDecoder().decode(hash);
            return decoded.length == 32; // SHA-256 produces 32 bytes
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Generate a test hash to verify algorithm.
     * Useful for debugging hash mismatches.
     */
    public static void testHashAlgorithms(String password, String salt) {
        System.out.println("\n🔬 [SecurityUtils] Testing hash algorithms:");
        System.out.println("Password: " + password);
        System.out.println("Salt: " + salt);
        
        String legacyHash = hashPasswordLegacy(password, salt);
        System.out.println("Legacy hash: " + legacyHash);
        
        String newHash = hashPassword(password, salt);
        System.out.println("New hash: " + newHash);
        
        if (legacyHash != null && newHash != null) {
            System.out.println("Same? " + legacyHash.equals(newHash));
        }
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
    
    /**
     * Decode Base64 string for debugging.
     */
    public static String decodeBase64(String base64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            return bytesToHex(decoded);
        } catch (Exception e) {
            return "Invalid Base64";
        }
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}