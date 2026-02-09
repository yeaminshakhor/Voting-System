package Data;

import Entities.Nominee;
import Entities.Voter;
import Utils.SecurityUtils;
import Utils.AuditLogger;
import java.io.*;
import java.util.*;

public class ElectionData {

    public static final String VOTER_FILE = "database_voters.txt";
    public static final String VOTER_SALT_FILE = "database_voter_salts.txt";
    public static final String VOTER_INFO_FILE = "database_voter_info.txt";
    public static final String NOMINEE_FILE = "database_nominees.txt";
    public static final String VOTE_FILE = "database_votes.txt";
    public static final String VOTER_VOTED_LOG = "database_voter_voted_log.txt";
    public static final String ELECTION_CONFIG_FILE = "election_config.txt";
    
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final long LOCKOUT_TIME_MS = 15 * 60 * 1000; // 15 minutes

    // -------------------- STATISTICS FUNCTIONS --------------------

    /**
     * Get vote counts for all nominees.
     */
    public static Map<String, Integer> getVoteCounts() {
        Map<String, Integer> counts = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 1) {
                    String nominee = parts[0].trim();
                    if (!nominee.isEmpty()) {
                        counts.put(nominee, counts.getOrDefault(nominee, 0) + 1);
                    }
                }
            }
        } catch (IOException e) {
            // File might not exist yet
            System.out.println("DEBUG: Vote file doesn't exist or can't be read: " + e.getMessage());
        }
        return counts;
    }

    /**
     * Get total number of votes cast.
     */
    public static int getTotalVotesCast() {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTE_FILE))) {
            while (reader.readLine() != null) {
                count++;
            }
        } catch (IOException e) {
            System.out.println("Error reading vote file: " + e.getMessage());
        }
        return count;
    }

    /**
     * Get total number of registered voters.
     */
    public static int getTotalRegisteredVoters() {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 3 && !parts[2].isEmpty() && !parts[2].equals("null")) {
                    count++;
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading voter file: " + e.getMessage());
        }
        return count;
    }

    // -------------------- VOTER FUNCTIONS --------------------

    /**
     * Check if voter ID exists in the database.
     */
    public static boolean voterIdExists(String voterId) {
        if (voterId == null || voterId.trim().isEmpty()) {
            System.out.println("❌ Invalid voter ID format: " + voterId);
            return false;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 1 && parts[0].equals(voterId)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error checking voter existence: " + e.getMessage());
        }
        return false;
    }

    /**
     * Check if voter is already registered (has password).
     */
    public static boolean isVoterRegistered(String voterId) {
        if (voterId == null || voterId.trim().isEmpty()) {
            return false;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 3 && parts[0].equals(voterId)) {
                    String password = parts[2].trim();
                    // Check if password is NOT empty, NOT null, and NOT unregistered marker
                    return !password.isEmpty() && 
                           !password.equals("null") && 
                           !password.equals(SecurityUtils.generateEmptyPasswordHash());
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error checking registration: " + e.getMessage());
        }
        return false;
    }

    /**
     * Get voter name by ID.
     */
    public static String getVoterName(String voterId) {
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 2 && parts[0].equals(voterId)) {
                    return parts[1];
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading voter file: " + e.getMessage());
        }
        return "Unknown";
    }

    /**
     * Update voter information (name and email).
     */
    public static boolean updateVoterInfo(String voterId, String newName, String email) {
        if (voterId == null || voterId.trim().isEmpty()) {
            System.out.println("❌ Invalid voter ID");
            return false;
        }
        
        if (newName == null || newName.trim().isEmpty()) {
            System.out.println("❌ Voter name cannot be empty");
            return false;
        }
        
        List<String> lines = new ArrayList<>();
        boolean found = false;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 3 && parts[0].equals(voterId)) {
                    found = true;
                    // Update name and preserve password
                    String password = parts[2];
                    lines.add(parts[0] + ":" + newName.trim() + ":" + password);
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading voter file: " + e.getMessage());
            return false;
        }
        
        if (!found) {
            System.out.println("❌ Voter ID not found: " + voterId);
            return false;
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(VOTER_FILE))) {
            for (String line : lines) {
                writer.println(line);
            }
            writer.flush();  // Explicit flush to disk
            System.out.println("✅ Voter information updated: " + voterId);
            return true;
        } catch (IOException e) {
            System.out.println("❌ Error writing voter file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Register voter or set password for existing unregistered voter with password hashing.
     */
    public static boolean registerVoter(Voter voter) {
        // Validate inputs
        if (voter.getVoterId() == null || voter.getVoterId().trim().isEmpty()) {
            System.out.println("❌ Invalid voter ID: " + voter.getVoterId());
            return false;
        }
        
        if (voter.getPassword() == null || voter.getPassword().length() < 6) {
            System.out.println("❌ Password must be at least 6 characters");
            return false;
        }
        
        List<String> lines = new ArrayList<>();
        boolean voterExists = false;
        boolean alreadyRegistered = false;
        
        // Read all lines and check status
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 3 && parts[0].equals(voter.getVoterId())) {
                    voterExists = true;
                    if (!parts[2].isEmpty() && !parts[2].equals("null")) {
                        alreadyRegistered = true;
                    }
                }
                lines.add(line);
            }
        } catch (IOException e) {
            // File might not exist, that's OK
        }
        
        // If already registered, cannot register again
        if (alreadyRegistered) {
            System.out.println("❌ Voter " + voter.getVoterId() + " is already registered!");
            return false;
        }
        
        // Generate salt and hash password
        String salt = SecurityUtils.generateSalt();
        String hashedPassword = SecurityUtils.hashPassword(voter.getPassword(), salt);
        
        if (hashedPassword == null) {
            System.out.println("❌ Failed to hash password");
            return false;
        }
        
        // Save salt to separate file
        try (BufferedWriter saltWriter = new BufferedWriter(new FileWriter(VOTER_SALT_FILE, true))) {
            saltWriter.write(voter.getVoterId() + ":" + salt);
            saltWriter.newLine();
        } catch (IOException e) {
            System.out.println("❌ Error saving salt: " + e.getMessage());
            return false;
        }
        
        // If voter exists but unregistered, update password
        if (voterExists) {
            List<String> updatedLines = new ArrayList<>();
            for (String line : lines) {
                String[] parts = line.split(":");
                if (parts.length >= 3 && parts[0].equals(voter.getVoterId())) {
                    updatedLines.add(parts[0] + ":" + parts[1] + ":" + hashedPassword);
                } else {
                    updatedLines.add(line);
                }
            }
            lines = updatedLines;
        } else {
            // Add as completely new voter
            lines.add(voter.getVoterId() + ":" + voter.getName() + ":" + hashedPassword);
        }
        
        // Write back to file with explicit flush
        try (PrintWriter writer = new PrintWriter(new FileWriter(VOTER_FILE))) {
            for (String line : lines) {
                writer.println(line);
            }
            writer.flush();  // Explicit flush to disk
            System.out.println("✅ Voter " + voter.getVoterId() + " registered successfully!");
            return true;
        } catch (IOException e) {
            System.out.println("❌ Error registering voter: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Validate voter login with backward compatibility.
     */
    public static boolean validateVoter(String voterId, String password) {
        System.out.println("🔐 Validating voter: " + voterId);
        
        if (voterId == null || voterId.trim().isEmpty() || password == null) {
            System.out.println("❌ Invalid input: voterId or password is null/empty");
            return false;
        }
        
        // Check if account is locked
        if (isAccountLocked(voterId)) {
            System.out.println("❌ Account locked: " + voterId + " - Too many failed attempts. Try again later.");
            return false;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":", -1);  // Use -1 to include empty parts
                if (parts.length >= 3 && parts[0].equals(voterId)) {
                    String storedPassword = parts[2].trim();
                    
                    // Check for unregistered voter
                    if (storedPassword.isEmpty() || 
                        storedPassword.equals("null") || 
                        storedPassword.equals(SecurityUtils.generateEmptyPasswordHash())) {
                        System.out.println("❌ Voter not registered. Please register first.");
                        recordFailedAttempt(voterId);
                        return false;
                    }
                    
                    // Support both plaintext and hashed passwords for backward compatibility
                    if (storedPassword.length() < 40) {
                        // Plaintext or short password - direct comparison
                        System.out.println("ℹ️  Validating with plaintext password (legacy)");
                        if (storedPassword.equals(password)) {
                            System.out.println("✅ Login successful (plaintext password match)");
                            clearFailedAttempts(voterId);
                            return true;
                        } else {
                            System.out.println("❌ Password mismatch for voter: " + voterId);
                            recordFailedAttempt(voterId);
                            return false;
                        }
                    }
                    
                    // Hashed password - validate with salt
                    // Get voter salt
                    String salt = getVoterSalt(voterId);
                    if (salt == null || salt.isEmpty()) {
                        System.out.println("⚠️  No salt found for voter: " + voterId + ". Trying without salt.");
                        // Try hashing without salt
                        String inputHash = SecurityUtils.hashPassword(password, "");
                        if (inputHash != null && storedPassword.equals(inputHash)) {
                            System.out.println("✅ Login successful (no salt hash match)");
                            clearFailedAttempts(voterId);
                            return true;
                        }
                    } else {
                        // Try hashing with salt (new algorithm)
                        try {
                            String inputHashNew = SecurityUtils.hashPassword(password, salt);
                            if (inputHashNew != null && storedPassword.equals(inputHashNew)) {
                                System.out.println("✅ Login successful (hashed password match)");
                                clearFailedAttempts(voterId);
                                return true;
                            }
                        } catch (Exception e) {
                            System.out.println("⚠️  Error with new hashing algorithm: " + e.getMessage());
                        }
                    }
                    
                    // Try old hashing algorithm for backward compatibility
                    try {
                        String inputHashOld = SecurityUtils.hashPasswordOld(password, salt);
                        if (inputHashOld != null && storedPassword.equals(inputHashOld)) {
                            System.out.println("✅ Login successful (legacy hashed password match)");
                            clearFailedAttempts(voterId);
                            return true;
                        }
                    } catch (Exception e) {
                        System.out.println("⚠️  Error with legacy hashing algorithm: " + e.getMessage());
                    }
                    
                    System.out.println("❌ Password mismatch for voter: " + voterId);
                    recordFailedAttempt(voterId);
                    return false;
                }
            }
            System.out.println("❌ Voter ID not found in database: " + voterId);
        } catch (IOException e) {
            System.out.println("❌ Error validating voter: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Auto-migrate plaintext password to hashed password.
     */
    private static void migrateVoterToHashedPassword(String voterId, String plainPassword) {
        String salt = SecurityUtils.generateSalt();
        String hashedPassword = SecurityUtils.hashPassword(plainPassword, salt);
        
        if (hashedPassword == null) {
            System.out.println("❌ Failed to hash password for migration");
            return;
        }
        
        // Update voter file
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 3 && parts[0].equals(voterId)) {
                    lines.add(parts[0] + ":" + parts[1] + ":" + hashedPassword);
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading voter file for migration: " + e.getMessage());
            return;
        }
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(VOTER_FILE))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("❌ Error writing voter file during migration: " + e.getMessage());
            return;
        }
        
        // Save salt
        try (BufferedWriter saltWriter = new BufferedWriter(new FileWriter(VOTER_SALT_FILE, true))) {
            saltWriter.write(voterId + ":" + salt);
            saltWriter.newLine();
            System.out.println("✅ Auto-migrated voter " + voterId + " to hashed password");
        } catch (IOException e) {
            System.out.println("⚠️ Voter migrated but failed to save salt: " + e.getMessage());
        }
    }

    /**
     * Get voter's salt from salt file.
     */
    private static String getVoterSalt(String voterId) {
        File saltFile = new File(VOTER_SALT_FILE);
        if (!saltFile.exists()) {
            return "";
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_SALT_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 2 && parts[0].equals(voterId)) {
                    return parts[1];
                }
            }
        } catch (IOException e) {
            return "";
        }
        return "";
    }

    /**
     * Record failed login attempt for brute force protection.
     */
    private static void recordFailedAttempt(String voterId) {
        String lockFile = "database_login_attempts.txt";
        Map<String, Integer> attempts = new HashMap<>();
        Map<String, Long> lockTimes = new HashMap<>();
        
        // Read existing attempts
        try (BufferedReader reader = new BufferedReader(new FileReader(lockFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 3) {
                    attempts.put(parts[0], Integer.parseInt(parts[1]));
                    lockTimes.put(parts[0], Long.parseLong(parts[2]));
                }
            }
        } catch (IOException e) {
            // File might not exist
        }
        
        // Update attempts
        int currentAttempts = attempts.getOrDefault(voterId, 0) + 1;
        attempts.put(voterId, currentAttempts);
        
        // Lock account if max attempts reached
        if (currentAttempts >= MAX_LOGIN_ATTEMPTS) {
            lockTimes.put(voterId, System.currentTimeMillis());
            System.out.println("⚠️ Account locked: " + voterId + " - Too many failed attempts");
        }
        
        // Save attempts
        try (PrintWriter writer = new PrintWriter(new FileWriter(lockFile))) {
            for (Map.Entry<String, Integer> entry : attempts.entrySet()) {
                writer.println(entry.getKey() + ":" + entry.getValue() + ":" + 
                              lockTimes.getOrDefault(entry.getKey(), 0L));
            }
        } catch (IOException e) {
            System.out.println("❌ Error recording failed attempt: " + e.getMessage());
        }
    }

    /**
     * Clear failed attempts after successful login.
     */
    private static void clearFailedAttempts(String voterId) {
        String lockFile = "database_login_attempts.txt";
        Map<String, Integer> attempts = new HashMap<>();
        Map<String, Long> lockTimes = new HashMap<>();
        
        // Read existing attempts
        try (BufferedReader reader = new BufferedReader(new FileReader(lockFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 3 && !parts[0].equals(voterId)) {
                    attempts.put(parts[0], Integer.parseInt(parts[1]));
                    lockTimes.put(parts[0], Long.parseLong(parts[2]));
                }
            }
        } catch (IOException e) {
            return;
        }
        
        // Save without the cleared voter
        try (PrintWriter writer = new PrintWriter(new FileWriter(lockFile))) {
            for (Map.Entry<String, Integer> entry : attempts.entrySet()) {
                writer.println(entry.getKey() + ":" + entry.getValue() + ":" + 
                              lockTimes.getOrDefault(entry.getKey(), 0L));
            }
        } catch (IOException e) {
            System.out.println("❌ Error clearing failed attempts: " + e.getMessage());
        }
    }

    /**
     * Check if account is locked due to too many failed attempts.
     */
    private static boolean isAccountLocked(String voterId) {
        String lockFile = "database_login_attempts.txt";
        File file = new File(lockFile);
        if (!file.exists()) {
            return false;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(lockFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 3 && parts[0].equals(voterId)) {
                    int attempts = Integer.parseInt(parts[1]);
                    long lockTime = Long.parseLong(parts[2]);
                    
                    if (attempts >= MAX_LOGIN_ATTEMPTS) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lockTime < LOCKOUT_TIME_MS) {
                            return true;
                        }
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            return false;
        }
        return false;
    }

    /**
     * Check if voter already voted.
     */
    public static boolean hasVoted(String voterId) {
        if (voterId == null || voterId.trim().isEmpty()) {
            return false;
        }
        
        File votedFile = new File(VOTER_VOTED_LOG);
        if (!votedFile.exists()) {
            return false;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_VOTED_LOG))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 1 && parts[0].equals(voterId)) {
                    return true;
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    /**
     * Get all voters with security info hidden.
     */
    public static String[] getAllVoters() {
        List<String> voters = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] parts = line.split(":");
                    if (parts.length >= 3) {
                        voters.add(parts[0] + ":" + parts[1] + ":******");
                    } else {
                        voters.add(line);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading voters: " + e.getMessage());
            e.printStackTrace();
        }
        return voters.toArray(new String[0]);
    }

    /**
     * Delete voter by ID.
     */
    public static boolean deleteVoter(String voterId) {
        if (voterId == null || voterId.trim().isEmpty()) {
            System.out.println("❌ Invalid voter ID: " + voterId);
            return false;
        }
        
        List<String> lines = new ArrayList<>();
        boolean deleted = false;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 1 && parts[0].equals(voterId)) {
                    deleted = true;
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error deleting voter: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        
        if (!deleted) {
            System.out.println("❌ Voter not found: " + voterId);
            return false;
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(VOTER_FILE))) {
            for (String line : lines) {
                writer.println(line);
            }
            
            deleteVoterSalt(voterId);
            deleteVoterVote(voterId);
            clearFailedAttempts(voterId);
            
            System.out.println("✅ Voter deleted: " + voterId);
            return true;
        } catch (IOException e) {
            System.out.println("❌ Error saving voter file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete voter salt from salt file.
     */
    private static void deleteVoterSalt(String voterId) {
        try {
            List<String> lines = new ArrayList<>();
            File saltFile = new File(VOTER_SALT_FILE);
            if (!saltFile.exists()) return;
            
            try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_SALT_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split(":");
                    if (parts.length >= 2 && !parts[0].equals(voterId)) {
                        lines.add(line);
                    }
                }
            }
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(VOTER_SALT_FILE))) {
                for (String line : lines) {
                    writer.println(line);
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error deleting voter salt: " + e.getMessage());
        }
    }

    /**
     * Delete voter's record of voting.
     */
    private static void deleteVoterVote(String voterId) {
        try {
            List<String> lines = new ArrayList<>();
            File votedFile = new File(VOTER_VOTED_LOG);
            if (!votedFile.exists()) return;
            
            try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_VOTED_LOG))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split(":");
                    if (parts.length >= 1 && !parts[0].equals(voterId)) {
                        lines.add(line);
                    }
                }
            }
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(VOTER_VOTED_LOG))) {
                for (String line : lines) {
                    writer.println(line);
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error deleting voter vote record: " + e.getMessage());
        }
    }

    /**
     * Update voter password with hashing.
     */
    public static boolean updateVoterPassword(String voterId, String newPassword) {
        if (voterId == null || voterId.trim().isEmpty()) {
            System.out.println("❌ Invalid voter ID: " + voterId);
            return false;
        }
        
        if (newPassword == null || newPassword.length() < 6) {
            System.out.println("❌ Password must be at least 6 characters");
            return false;
        }
        
        // Generate new salt and hash
        String newSalt = SecurityUtils.generateSalt();
        String newHashedPassword = SecurityUtils.hashPassword(newPassword, newSalt);
        
        if (newHashedPassword == null) {
            System.out.println("❌ Failed to hash new password");
            return false;
        }
        
        List<String> lines = new ArrayList<>();
        boolean updated = false;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 3 && parts[0].equals(voterId)) {
                    lines.add(parts[0] + ":" + parts[1] + ":" + newHashedPassword);
                    updated = true;
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error updating password: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        
        if (!updated) {
            System.out.println("❌ Voter not found: " + voterId);
            return false;
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(VOTER_FILE))) {
            for (String line : lines) {
                writer.println(line);
            }
            
            updateVoterSalt(voterId, newSalt);
            
            System.out.println("✅ Password updated for voter: " + voterId);
            return true;
        } catch (IOException e) {
            System.out.println("❌ Error saving voter file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update voter password with explicit salt (for password reset utility).
     * Takes pre-hashed password and salt directly.
     */
    public static String updateVoterPassword(String voterId, String hashedPassword, String salt) {
        if (voterId == null || voterId.trim().isEmpty()) {
            return "Error: Invalid voter ID";
        }
        
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            return "Error: Hashed password cannot be empty";
        }
        
        if (salt == null || salt.isEmpty()) {
            return "Error: Salt cannot be empty";
        }
        
        List<String> lines = new ArrayList<>();
        boolean updated = false;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 3 && parts[0].equals(voterId)) {
                    lines.add(parts[0] + ":" + parts[1] + ":" + hashedPassword);
                    updated = true;
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            return "Error reading voter file: " + e.getMessage();
        }
        
        if (!updated) {
            return "Error: Voter not found";
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(VOTER_FILE))) {
            for (String line : lines) {
                writer.println(line);
            }
            
            // Update salt
            updateVoterSaltDirect(voterId, salt);
            
            System.out.println("✅ Password updated for voter: " + voterId);
            return "Password updated successfully";
        } catch (IOException e) {
            return "Error saving voter file: " + e.getMessage();
        }
    }

    /**
     * Update voter salt in salt file.
     */
    private static void updateVoterSalt(String voterId, String newSalt) {
        try {
            List<String> lines = new ArrayList<>();
            boolean found = false;
            
            File saltFile = new File(VOTER_SALT_FILE);
            if (saltFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_SALT_FILE))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        String[] parts = line.split(":");
                        if (parts.length >= 2 && parts[0].equals(voterId)) {
                            found = true;
                            lines.add(voterId + ":" + newSalt);
                        } else {
                            lines.add(line);
                        }
                    }
                }
            }
            
            // If salt not found, add new entry
            if (!found) {
                lines.add(voterId + ":" + newSalt);
            }
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(VOTER_SALT_FILE))) {
                for (String line : lines) {
                    writer.println(line);
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error updating voter salt: " + e.getMessage());
        }
    }

    /**
     * Update voter salt directly (for password reset utility).
     */
    private static void updateVoterSaltDirect(String voterId, String newSalt) {
        updateVoterSalt(voterId, newSalt);
    }

    /**
     * Get votes cast by a specific voter.
     */
    public static String[] getVoterVotes(String voterId) {
        if (hasVoted(voterId)) {
            return new String[]{"Vote recorded (anonymized)"};
        }
        return new String[]{"No vote recorded"};
    }

    /**
     * Get voting history for a specific voter as a string.
     */
    public static String getVoterHistory(String voterId) {
        StringBuilder sb = new StringBuilder();
        sb.append("Voter ID: ").append(voterId).append("\n");
        sb.append("-------------------------------------------\n");
        
        if (hasVoted(voterId)) {
            sb.append("Status: ✅ Vote recorded\n");
            sb.append("The vote has been recorded and is kept anonymous.\n");
        } else {
            sb.append("Status: ❌ No vote recorded\n");
            sb.append("This voter has not voted yet.\n");
        }
        
        sb.append("\nVoting Status: ").append(hasVoted(voterId) ? "Completed" : "Pending").append("\n");
        
        return sb.toString();
    }

    // -------------------- NOMINEE FUNCTIONS --------------------

    /**
     * Check if nominee ID exists in a specific election.
     */
    public static boolean nomineeIdExistsInElection(String nomineeId, String electionId) {
        if (nomineeId == null || nomineeId.trim().isEmpty()) {
            return false;
        }
        
        String election = (electionId == null || electionId.isEmpty()) ? "DEFAULT" : electionId;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(NOMINEE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 4) {
                    // New format with election_id
                    if (parts[0].equals(nomineeId) && parts[3].equals(election)) {
                        return true;
                    }
                } else if (parts.length >= 1 && parts[0].equals(nomineeId)) {
                    // Old format without election_id - treat as DEFAULT
                    if ("DEFAULT".equals(election)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error checking nominee: " + e.getMessage());
        }
        return false;
    }

    /**
     * Check if nominee ID exists.
     */
    public static boolean nomineeIdExists(String nomineeId) {
        if (nomineeId == null || nomineeId.trim().isEmpty()) {
            return false;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(NOMINEE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 1 && parts[0].equals(nomineeId)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error checking nominee: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get nominee name by ID.
     */
    public static String getNomineeName(String nomineeId) {
        try (BufferedReader reader = new BufferedReader(new FileReader(NOMINEE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 2 && parts[0].equals(nomineeId)) {
                    return parts[1];
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading nominee file: " + e.getMessage());
        }
        return "Unknown";
    }

    /**
     * Add nominee with input validation.
     */
    public static boolean addNominee(Nominee nominee) {
        // Validate inputs
        if (nominee.getNomineeId() == null || nominee.getNomineeId().trim().isEmpty()) {
            System.out.println("❌ Invalid nominee ID: " + nominee.getNomineeId());
            return false;
        }
        
        if (nominee.getNomineeName() == null || nominee.getNomineeName().trim().isEmpty()) {
            System.out.println("❌ Invalid nominee name: " + nominee.getNomineeName());
            return false;
        }
        
        if (nominee.getPartyName() == null || nominee.getPartyName().trim().isEmpty()) {
            System.out.println("❌ Party name is required");
            return false;
        }
        
        // Check for duplicate within same election
        if (nomineeIdExistsInElection(nominee.getNomineeId(), nominee.getElectionId())) {
            System.out.println("❌ Nominee ID already exists in this election: " + nominee.getNomineeId());
            return false;
        }
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(NOMINEE_FILE, true))) {
            String electionId = (nominee.getElectionId() == null || nominee.getElectionId().isEmpty()) ? "DEFAULT" : nominee.getElectionId();
            writer.write(nominee.getNomineeId() + ":" + nominee.getNomineeName() + ":" + nominee.getPartyName() + ":" + electionId);
            writer.newLine();
            System.out.println("✅ Nominee added to election " + electionId + ": " + nominee.getNomineeName());
            return true;
        } catch (IOException e) {
            System.out.println("❌ Error adding nominee: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete nominee by ID.
     */
    public static boolean deleteNominee(String nomineeId) {
        if (nomineeId == null || nomineeId.trim().isEmpty()) {
            System.out.println("❌ Invalid nominee ID: " + nomineeId);
            return false;
        }
        
        List<String> lines = new ArrayList<>();
        boolean deleted = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(NOMINEE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length >= 1 && parts[0].equals(nomineeId)) {
                    deleted = true;
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error deleting nominee: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        if (!deleted) {
            System.out.println("❌ Nominee not found: " + nomineeId);
            return false;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(NOMINEE_FILE))) {
            for (String l : lines) {
                writer.println(l);
            }
            
            deleteNomineeVotes(nomineeId);
            
            System.out.println("✅ Nominee deleted: " + nomineeId);
            return true;
        } catch (IOException e) {
            System.out.println("❌ Error saving nominee file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete all votes for a nominee.
     */
    private static void deleteNomineeVotes(String nomineeId) {
        try {
            List<String> lines = new ArrayList<>();
            File voteFile = new File(VOTE_FILE);
            if (!voteFile.exists()) return;
            
            try (BufferedReader reader = new BufferedReader(new FileReader(VOTE_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split(":");
                    if (parts.length >= 1 && !parts[0].equals(nomineeId)) {
                        lines.add(line);
                    }
                }
            }
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(VOTE_FILE))) {
                for (String line : lines) {
                    writer.println(line);
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error deleting nominee votes: " + e.getMessage());
        }
    }

    /**
     * Get all nominees.
     */
    public static String[] getAllNominees() {
        List<String> nominees = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(NOMINEE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    // Return only core info (ID:Name:Party) for backward compatibility
                    String[] parts = line.split(":");
                    if (parts.length >= 3) {
                        // Check if this nominee has election_id (new format)
                        if (parts.length >= 4) {
                            // Has election_id - return with it
                            nominees.add(parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3]);
                        } else {
                            // Old format - add as DEFAULT election
                            nominees.add(parts[0] + ":" + parts[1] + ":" + parts[2] + ":DEFAULT");
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading nominees: " + e.getMessage());
            e.printStackTrace();
        }
        return nominees.toArray(new String[0]);
    }

    /**
     * Get nominees for a specific election.
     */
    public static String[] getNomineesByElection(String electionId) {
        List<String> nominees = new ArrayList<>();
        String election = (electionId == null || electionId.isEmpty()) ? "DEFAULT" : electionId;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(NOMINEE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] parts = line.split(":");
                    // Check for new format with election_id
                    if (parts.length >= 4) {
                        if (parts[3].equals(election)) {
                            nominees.add(parts[0] + ":" + parts[1] + ":" + parts[2]);
                        }
                    } else if (parts.length >= 3 && "DEFAULT".equals(election)) {
                        // Old format - treat as DEFAULT election
                        nominees.add(parts[0] + ":" + parts[1] + ":" + parts[2]);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading nominees for election " + election + ": " + e.getMessage());
        }
        return nominees.toArray(new String[0]);
    }

    // -------------------- VOTE FUNCTIONS (FIXED) --------------------

    /**
     * Cast a vote with validation and security checks.
     * IMPORTANT: Votes are stored ANONYMOUSLY (no voter ID) to preserve ballot secrecy.
     * Only the nominee ID and timestamp are recorded.
     */
    public static boolean castVote(String voterId, String nomineeId) {
        System.out.println("DEBUG: Attempting to cast vote for " + voterId + " -> " + nomineeId);
        
        // Validate inputs
        if (voterId == null || voterId.trim().isEmpty()) {
            System.out.println("❌ Invalid voter ID: " + voterId);
            return false;
        }
        
        if (nomineeId == null || nomineeId.trim().isEmpty()) {
            System.out.println("❌ Invalid nominee ID: " + nomineeId);
            return false;
        }
        
        // Check if voter exists and is registered
        if (!voterIdExists(voterId)) {
            System.out.println("❌ Voter not found: " + voterId);
            return false;
        }
        
        if (!isVoterRegistered(voterId)) {
            System.out.println("❌ Voter not registered: " + voterId);
            return false;
        }
        
        // Check if nominee exists
        if (!nomineeIdExists(nomineeId)) {
            System.out.println("❌ Nominee not found: " + nomineeId);
            return false;
        }
        
        // Check if already voted
        if (hasVoted(voterId)) {
            System.out.println("❌ Voter already voted: " + voterId);
            return false;
        }
        
        // Check if election is active
        if (!isElectionActive()) {
            System.out.println("❌ Election is not currently active");
            return false;
        }
        
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        // ANONYMIZED VOTE: Store ONLY nominee ID and timestamp
        try {
            // Create vote file if it doesn't exist
            File voteFile = new File(VOTE_FILE);
            if (!voteFile.exists()) {
                voteFile.createNewFile();
                System.out.println("DEBUG: Created vote file: " + voteFile.getAbsolutePath());
            }
            
            // Write the vote
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(VOTE_FILE, true))) {
                writer.write(nomineeId + ":" + timestamp);
                writer.newLine();
                writer.flush();  // Explicit flush to disk
                System.out.println("✅ Vote cast successfully: " + nomineeId + ":" + timestamp);
            }
        } catch (IOException e) {
            System.out.println("❌ Error casting vote: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        
        // Record that this voter has voted (separate from vote choice)
        try {
            // Create voted log file if it doesn't exist
            File votedFile = new File(VOTER_VOTED_LOG);
            if (!votedFile.exists()) {
                votedFile.createNewFile();
                System.out.println("DEBUG: Created voted log file");
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(VOTER_VOTED_LOG, true))) {
                writer.write(voterId + ":" + timestamp);
                writer.newLine();
                writer.flush();  // Explicit flush to disk
                System.out.println("✅ Voter recorded as voted: " + voterId);
            }
        } catch (IOException e) {
            System.out.println("❌ Error recording voter: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        
        return true;
    }

    /**
     * Check if election is active.
     */
    private static boolean isElectionActive() {
        // Read election configuration
        try {
            File configFile = new File(ELECTION_CONFIG_FILE);
            if (!configFile.exists()) {
                System.out.println("DEBUG: No election config file, assuming active");
                return true; // Default to active if no config
            }
            
            try (BufferedReader reader = new BufferedReader(new FileReader(ELECTION_CONFIG_FILE))) {
                String line = reader.readLine();
                boolean active = (line != null && line.trim().equalsIgnoreCase("active"));
                System.out.println("DEBUG: Election active status: " + active);
                return active;
            }
        } catch (IOException e) {
            System.out.println("DEBUG: Error reading config, defaulting to active: " + e.getMessage());
            return true; // Default to active on error
        }
    }

    /**
     * Get all votes (for admin use only).
     */
    public static String[] getAllVotes() {
        List<String> votes = new ArrayList<>();
        File voteFile = new File(VOTE_FILE);
        if (!voteFile.exists()) {
            return votes.toArray(new String[0]);
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    votes.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading votes: " + e.getMessage());
            e.printStackTrace();
        }
        return votes.toArray(new String[0]);
    }

    /**
     * Initialize election configuration if it doesn't exist.
     */
    public static void initializeElectionConfig() {
        File configFile = new File(ELECTION_CONFIG_FILE);
        if (!configFile.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(ELECTION_CONFIG_FILE))) {
                writer.write("active");
                writer.newLine();
                System.out.println("✅ Election configuration initialized (active)");
            } catch (IOException e) {
                System.out.println("❌ Error initializing election config: " + e.getMessage());
            }
        }
    }

    /**
     * Set election status.
     */
    public static boolean setElectionStatus(boolean active) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ELECTION_CONFIG_FILE))) {
            writer.write(active ? "active" : "inactive");
            writer.newLine();
            System.out.println("✅ Election status set to: " + (active ? "active" : "inactive"));
            return true;
        } catch (IOException e) {
            System.out.println("❌ Error setting election status: " + e.getMessage());
            return false;
        }
    }

    // -------------------- TEST AND DIAGNOSTICS --------------------

    /**
     * Run diagnostics on the voting system.
     */
    public static void runDiagnostics() {
        System.out.println("\n=== VOTING SYSTEM DIAGNOSTICS ===");
        
        // Check files
        String[] files = {VOTER_FILE, NOMINEE_FILE, VOTE_FILE, VOTER_VOTED_LOG, ELECTION_CONFIG_FILE};
        for (String file : files) {
            File f = new File(file);
            System.out.println(file + " exists: " + f.exists() + " writable: " + f.canWrite() + 
                             " path: " + f.getAbsolutePath());
        }
        
        // Check election status
        System.out.println("Election active: " + isElectionActive());
        
        // Show counts
        System.out.println("Total voters: " + getAllVoters().length);
        System.out.println("Total nominees: " + getAllNominees().length);
        System.out.println("Total votes cast: " + getTotalVotesCast());
        
        System.out.println("=== END DIAGNOSTICS ===\n");
    }

    // -------------------- PORTAL INTEGRATION FUNCTIONS --------------------

    /**
     * Register a new voter from portal data.
     * Creates voter without password (requires self-registration).
     * Also stores extended profile information (DOB, blood group, department).
     */
    public static boolean addVoterFromPortal(String voterData, String dob, String bloodGroup, String department) {
        // Format: id:name:email
        String[] parts = voterData.split(":");
        if (parts.length < 2) {
            System.out.println("❌ Invalid voter data format");
            return false;
        }
        
        String voterId = parts[0].trim();
        String voterName = parts[1].trim();
        String voterEmail = parts.length > 2 ? parts[2].trim() : "";
        
        // Check if voter already exists
        if (voterIdExists(voterId)) {
            System.out.println("⚠️ Voter already exists: " + voterId);
            return false;
        }
        
        // Add voter with empty password (requires self-registration)
        String emptyPasswordHash = SecurityUtils.generateEmptyPasswordHash();
        String salt = SecurityUtils.generateSalt();
        
        try {
            // Add to voter file
            try (PrintWriter writer = new PrintWriter(new FileWriter(VOTER_FILE, true))) {
                writer.println(voterId + ":" + voterName + ":" + emptyPasswordHash);
                writer.flush();  // Explicit flush to disk
            }
            
            // Save salt
            try (PrintWriter writer = new PrintWriter(new FileWriter(VOTER_SALT_FILE, true))) {
                writer.println(voterId + ":" + salt);
                writer.flush();  // Explicit flush to disk
            }
            
            // Save extended voter info (DOB, blood group, department, email)
            saveExtendedVoterInfo(voterId, dob, bloodGroup, department, voterEmail);
            
            // Log in audit
            AuditLogger.logSystemAction("VOTER_IMPORTED", "Voter imported from portal: " + voterId + " (" + voterName + ")");
            
            System.out.println("✅ Voter added from portal: " + voterId + " (" + voterName + ")");
            return true;
        } catch (IOException e) {
            System.out.println("❌ Error adding voter from portal: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Backward compatibility: Register a new voter from portal data (without extended fields).
     */
    public static boolean addVoterFromPortal(String voterData) {
        return addVoterFromPortal(voterData, "", "", "");
    }
    
    /**
     * Save extended voter information (DOB, blood group, department, email)
     * Format: id:dob:blood_group:department:email
     */
    private static void saveExtendedVoterInfo(String voterId, String dob, String bloodGroup, String department, String email) {
        try {
            // Create info file if it doesn't exist, then append
            try (PrintWriter writer = new PrintWriter(new FileWriter(VOTER_INFO_FILE, true))) {
                writer.println(voterId + ":" + (dob == null ? "" : dob) + ":" + 
                              (bloodGroup == null ? "" : bloodGroup) + ":" + 
                              (department == null ? "" : department) + ":" + 
                              (email == null ? "" : email));
                writer.flush();  // Explicit flush to disk
            }
            System.out.println("✓ Extended voter info saved: " + voterId);
        } catch (IOException e) {
            System.out.println("⚠️  Warning: Could not save extended voter info: " + e.getMessage());
            // Don't fail the whole import if extended info fails
        }
    }
    
    /**
     * Get extended voter information by ID
     * Returns: id:dob:blood_group:department:email
     */
    public static String getExtendedVoterInfo(String voterId) {
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_INFO_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":");
                if (parts.length > 0 && parts[0].equals(voterId)) {
                    return line;
                }
            }
        } catch (IOException e) {
            // File might not exist or voter info not available
        }
        return null;
    }
    
    /**
     * Get DOB for a voter
     */
    public static String getVoterDOB(String voterId) {
        String info = getExtendedVoterInfo(voterId);
        if (info != null) {
            String[] parts = info.split(":");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return "";
    }
    
    /**
     * Get blood group for a voter
     */
    public static String getVoterBloodGroup(String voterId) {
        String info = getExtendedVoterInfo(voterId);
        if (info != null) {
            String[] parts = info.split(":");
            if (parts.length > 2) {
                return parts[2];
            }
        }
        return "";
    }

    /**
     * Register a new voter with self-registration (no admin required).
     * Password is automatically hashed.
     */
    public static String registerVoterSelf(String voterId, String voterName, String plainPassword) {
        if (voterId == null || voterId.trim().isEmpty()) {
            return "Error: Voter ID cannot be empty";
        }
        
        if (voterName == null || voterName.trim().isEmpty()) {
            return "Error: Voter name cannot be empty";
        }
        
        if (plainPassword == null || plainPassword.length() < 6) {
            return "Error: Password must be at least 6 characters";
        }
        
        // Check if voter already exists or is registered
        String[] voters = getAllVoters();
        for (String voter : voters) {
            String[] parts = voter.split(":");
            if (parts.length > 0 && parts[0].equals(voterId)) {
                String storedPassword = parts.length > 2 ? parts[2] : "";
                if (!storedPassword.equals(SecurityUtils.generateEmptyPasswordHash())) {
                    return "Error: Voter already registered";
                }
                // Voter exists but has empty password - allow registration
                break;
            }
        }
        
        // Hash the password
        String salt = SecurityUtils.generateSalt();
        String hashedPassword = SecurityUtils.hashPassword(plainPassword, salt);
        
        if (hashedPassword == null) {
            return "Error: Failed to hash password";
        }
        
        try {
            List<String> lines = new ArrayList<>();
            boolean updated = false;
            
            // Read and update voter file
            try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split(":");
                    if (parts.length > 0 && parts[0].equals(voterId)) {
                        // Update existing voter with password
                        lines.add(voterId + ":" + voterName + ":" + hashedPassword);
                        updated = true;
                    } else {
                        lines.add(line);
                    }
                }
            }
            
            // If voter not found, create new one
            if (!updated) {
                lines.add(voterId + ":" + voterName + ":" + hashedPassword);
            }
            
            // Write back to file
            try (PrintWriter writer = new PrintWriter(new FileWriter(VOTER_FILE))) {
                for (String line : lines) {
                    writer.println(line);
                }
                writer.flush();  // Explicit flush to disk
            }
            
            // Update salt
            updateVoterSalt(voterId, salt);
            
            // Log registration
            AuditLogger.logSystemAction("VOTER_REGISTERED", "Voter self-registered: " + voterId);
            
            System.out.println("✅ Voter registered successfully: " + voterId);
            return "Success: Voter registered. You can now login.";
        } catch (IOException e) {
            System.out.println("❌ Error registering voter: " + e.getMessage());
            e.printStackTrace();
            return "Error: Failed to register voter";
        }
    }

    /**
     * Check if voter needs password (for portal-imported voters).
     */
    public static boolean voterNeedsRegistration(String voterId) {
        String[] voters = getAllVoters();
        for (String voter : voters) {
            String[] parts = voter.split(":");
            if (parts.length > 0 && parts[0].equals(voterId)) {
                String password = parts.length > 2 ? parts[2] : "";
                return password.equals(SecurityUtils.generateEmptyPasswordHash());
            }
        }
        return false;
    }

    /**
     * Get voter email from voter data.
     */
    public static String getVoterEmail(String voterId) {
        String[] voters = getAllVoters();
        for (String voter : voters) {
            String[] parts = voter.split(":");
            if (parts.length > 0 && parts[0].equals(voterId)) {
                return parts.length > 3 ? parts[3] : "";
            }
        }
        return "";
    }

    /**
     * Import voters from CSV file (portal format).
     * Format: id,name,email (one per line)
     */
    public static int importVotersFromCSV(String csvFilePath) {
        int imported = 0;
        int failed = 0;
        
        System.out.println("📥 Importing voters from CSV: " + csvFilePath);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String email = parts.length > 2 ? parts[2].trim() : "";
                    
                    String voterData = id + ":" + name + ":" + email;
                    if (addVoterFromPortal(voterData)) {
                        imported++;
                    } else {
                        failed++;
                    }
                }
            }
            
            System.out.println("✓ CSV import complete: " + imported + " imported, " + failed + " failed/skipped");
            return imported;
        } catch (IOException e) {
            System.out.println("❌ Error reading CSV file: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
}