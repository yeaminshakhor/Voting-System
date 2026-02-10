package Data;

import Entities.Voter;
import Entities.Nominee;
import Utils.SecurityUtils;
import java.sql.*;
import java.util.*;
import java.io.*;

/**
 * SQL-based Election Data Manager - Replaces plain text file storage.
 * All voter, nominee, and vote operations are now in SQLite database.
 */
public class SqlElectionDataManager {
    
    // ==================== MIGRATION METHODS ====================
    
    /**
     * Migrate all data from text files to SQL database
     */
    public static boolean migrateAllData() {
        System.out.println("\n🔄 Starting data migration from plain text files to SQL...");
        
        int adminCount = SqlAdminManager.migrateAllAdminsFromTextFile();
        int voterCount = migrateVotersFromTextFile();
        int nomineeCount = migrateNomineesFromTextFile();
        int voteCount = migrateVotesFromTextFile();
        
        System.out.println("\n✅ Migration completed!");
        System.out.println("   ➜ Admins imported: " + adminCount);
        System.out.println("   ➜ Voters imported: " + voterCount);
        System.out.println("   ➜ Nominees imported: " + nomineeCount);
        System.out.println("   ➜ Votes imported: " + voteCount);
        
        return (adminCount + voterCount + nomineeCount + voteCount) > 0;
    }
    
    /**
     * Migrate voters from text file to SQL
     */
    public static int migrateVotersFromTextFile() {
        System.out.println("\n👥 Migrating voters...");
        File voterFile = new File("database_voters.txt");
        if (!voterFile.exists()) {
            System.out.println("❌ Voter file not found: database_voters.txt");
            return 0;
        }
        
        int migrated = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(voterFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Format: voterId:Name:Email:PasswordHash:HasVoted
                String[] parts = line.split(":");
                if (parts.length >= 5) {
                    String voterId = parts[0].trim();
                    String name = parts[1].trim();
                    String email = parts[2].trim();
                    String hasVotedStr = parts[4].trim();
                    boolean hasVoted = "1".equals(hasVotedStr) || "true".equalsIgnoreCase(hasVotedStr);
                    
                    System.out.println("  Migrating voter: " + voterId);
                    
                    // Check if voter already exists
                    if (!voterIdExists(voterId)) {
                        // Generate new salt for migrated voter
                        String salt = SecurityUtils.generateSalt();
                        String tempPassword = "Voter123!"; // Temporary password
                        String newHash = SecurityUtils.hashPassword(tempPassword, salt);
                        
                        if (newHash != null) {
                            // Use existing addVoter method
                            boolean success = addVoterDirect(voterId, name, email, newHash, salt, hasVoted);
                            if (success) {
                                migrated++;
                                System.out.println("    ✅ Migrated - Temp password: Voter123!");
                            } else {
                                System.out.println("    ❌ Failed to migrate");
                            }
                        }
                    } else {
                        System.out.println("    ⚠️ Already exists, skipping");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading voter file: " + e.getMessage());
        }
        
        System.out.println("✅ Migrated " + migrated + " voters");
        return migrated;
    }
    
    /**
     * Direct voter add for migration (bypasses some validations)
     */
    private static boolean addVoterDirect(String voterId, String name, String email, 
                                         String passwordHash, String salt, boolean hasVoted) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "INSERT INTO voters (voter_id, name, email, password_hash, salt, is_registered, has_voted) " +
                     "VALUES (?, ?, ?, ?, ?, 1, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, voterId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, passwordHash);
            stmt.setString(5, salt);
            stmt.setInt(6, hasVoted ? 1 : 0);
            
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Error adding voter in migration: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Migrate nominees from text file to SQL
     */
    public static int migrateNomineesFromTextFile() {
        System.out.println("\n🎭 Migrating nominees...");
        File nomineeFile = new File("database_nominees.txt");
        if (!nomineeFile.exists()) {
            System.out.println("❌ Nominee file not found: database_nominees.txt");
            return 0;
        }
        
        int migrated = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(nomineeFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Format: nomineeId:Name:Position:VoteCount
                String[] parts = line.split(":");
                if (parts.length >= 4) {
                    String nomineeId = parts[0].trim();
                    String name = parts[1].trim();
                    String position = parts[2].trim();
                    String voteCountStr = parts[3].trim();
                    int voteCount = 0;
                    try {
                        voteCount = Integer.parseInt(voteCountStr);
                    } catch (NumberFormatException e) {
                        voteCount = 0;
                    }
                    
                    System.out.println("  Migrating nominee: " + name + " for " + position);
                    
                    // Check if nominee already exists
                    if (!nomineeExists(nomineeId)) {
                        boolean success = addNomineeDirect(nomineeId, name, position, voteCount);
                        if (success) {
                            migrated++;
                            System.out.println("    ✅ Migrated");
                        } else {
                            System.out.println("    ❌ Failed to migrate");
                        }
                    } else {
                        System.out.println("    ⚠️ Already exists, skipping");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading nominee file: " + e.getMessage());
        }
        
        System.out.println("✅ Migrated " + migrated + " nominees");
        return migrated;
    }
    
    /**
     * Direct nominee add for migration
     */
    private static boolean addNomineeDirect(String nomineeId, String name, String position, int voteCount) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "INSERT INTO nominees (nominee_id, name, party, is_active) VALUES (?, ?, ?, 1)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nomineeId);
            stmt.setString(2, name);
            stmt.setString(3, position); // Using party field for position
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Error adding nominee in migration: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Migrate votes from text file to SQL
     */
    public static int migrateVotesFromTextFile() {
        System.out.println("\n🗳️ Migrating votes...");
        File voteFile = new File("database_votes.txt");
        if (!voteFile.exists()) {
            System.out.println("❌ Vote file not found: database_votes.txt");
            return 0;
        }
        
        int migrated = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(voteFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Format: voteId:VoterId:NomineeId:Timestamp
                String[] parts = line.split(":");
                if (parts.length >= 4) {
                    String voteId = parts[0].trim();
                    String voterId = parts[1].trim();
                    String nomineeId = parts[2].trim();
                    String timestamp = parts[3].trim();
                    
                    System.out.println("  Migrating vote #" + voteId);
                    
                    // Check if vote already exists
                    if (!voteExists(voteId)) {
                        boolean success = addVoteDirect(voteId, voterId, nomineeId, timestamp);
                        if (success) {
                            migrated++;
                            System.out.println("    ✅ Migrated");
                            // Also update voter's has_voted status
                            updateVoterVotedStatus(voterId);
                        } else {
                            System.out.println("    ❌ Failed to migrate");
                        }
                    } else {
                        System.out.println("    ⚠️ Already exists, skipping");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading vote file: " + e.getMessage());
        }
        
        System.out.println("✅ Migrated " + migrated + " votes");
        return migrated;
    }
    
    /**
     * Check if vote exists
     */
    private static boolean voteExists(String voteId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "SELECT 1 FROM votes WHERE vote_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, voteId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Direct vote add for migration
     */
    private static boolean addVoteDirect(String voteId, String voterId, String nomineeId, String timestamp) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "INSERT INTO votes (vote_id, voter_id, nominee_id, created_at) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, voteId);
            stmt.setString(2, voterId);
            stmt.setString(3, nomineeId);
            stmt.setString(4, timestamp);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Error adding vote in migration: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update voter's has_voted status
     */
    private static void updateVoterVotedStatus(String voterId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return;
        
        String sql = "UPDATE voters SET has_voted = 1 WHERE voter_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, voterId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("⚠️ Could not update voter status: " + e.getMessage());
        }
    }
    
    // ==================== EXISTING VOTER OPERATIONS ====================
    // (Keep all your existing voter methods as they are)
    
    /**
     * Add a new voter to the database
     */
    public static boolean addVoter(String voterId, String name) {
        return addVoterWithEmail(voterId, name, "");
    }
    
    /**
     * Add voter with email (for portal imports)
     */
    public static boolean addVoterWithEmail(String voterId, String name, String email) {
        if (!SecurityUtils.isValidId(voterId)) {
            System.out.println("❌ Invalid voter ID format");
            return false;
        }
        
        if (!SecurityUtils.isValidName(name)) {
            System.out.println("❌ Invalid voter name");
            return false;
        }
        
        if (voterIdExists(voterId)) {
            System.out.println("⚠️ Voter already exists: " + voterId);
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "INSERT INTO voters (voter_id, name, email, is_registered) VALUES (?, ?, ?, 0)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, voterId);
            stmt.setString(2, name);
            stmt.setString(3, email != null && !email.isEmpty() ? email : null);
            stmt.executeUpdate();
            System.out.println("✅ Voter added: " + voterId + (email != null && !email.isEmpty() ? " (" + email + ")" : ""));
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Error adding voter: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete a voter from the database
     */
    public static boolean deleteVoter(String voterId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "DELETE FROM voters WHERE voter_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, voterId);
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("✅ Voter deleted: " + voterId);
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Error deleting voter: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Check if voter ID exists
     */
    public static boolean voterIdExists(String voterId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "SELECT 1 FROM voters WHERE voter_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, voterId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            System.out.println("❌ Error checking voter existence: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Register a voter (set password)
     */
    public static boolean registerVoter(String voterId, String password) {
        if (password == null || password.length() < 6) {
            System.out.println("❌ Password must be at least 6 characters");
            return false;
        }
        
        if (!isVoterRegistered(voterId)) {
            System.out.println("❌ Voter not found or already registered");
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String salt = SecurityUtils.generateSalt();
        String passwordHash = SecurityUtils.hashPassword(password, salt);
        
        if (passwordHash == null) {
            System.out.println("❌ Failed to hash password");
            return false;
        }
        
        String sql = "UPDATE voters SET password_hash = ?, salt = ?, is_registered = 1, registered_at = CURRENT_TIMESTAMP WHERE voter_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, passwordHash);
            stmt.setString(2, salt);
            stmt.setString(3, voterId);
            
            stmt.executeUpdate();
            System.out.println("✅ Voter registered: " + voterId);
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Error registering voter: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if voter is registered (has password set)
     */
    public static boolean isVoterRegistered(String voterId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "SELECT is_registered FROM voters WHERE voter_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, voterId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("is_registered") == 1;
            }
        } catch (SQLException e) {
            System.out.println("❌ Error checking registration: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Validate voter credentials
     */
    public static boolean validateVoterCredentials(String voterId, String password) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "SELECT password_hash, salt FROM voters WHERE voter_id = ? AND is_registered = 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, voterId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                String salt = rs.getString("salt");
                
                String inputHash = SecurityUtils.hashPassword(password, salt);
                
                if (inputHash != null && inputHash.equals(storedHash)) {
                    updateVoterLastLogin(voterId);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error validating voter: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get voter name by ID
     */
    public static String getVoterName(String voterId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return "Unknown";
        
        String sql = "SELECT name FROM voters WHERE voter_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, voterId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            System.out.println("❌ Error getting voter name: " + e.getMessage());
        }
        
        return "Unknown";
    }
    
    /**
     * Get all voters
     */
    public static List<Voter> getAllVoters() {
        List<Voter> voters = new ArrayList<>();
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return voters;
        
        String sql = "SELECT voter_id, name, is_registered, has_voted FROM voters ORDER BY voter_id";
        
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Voter voter = new Voter();
                voter.setId(rs.getString("voter_id"));
                voter.setName(rs.getString("name"));
                voters.add(voter);
            }
        } catch (SQLException e) {
            System.out.println("❌ Error retrieving voters: " + e.getMessage());
        }
        
        return voters;
    }
    
    /**
     * Check if voter has already voted
     */
    public static boolean hasVoterVoted(String voterId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "SELECT has_voted FROM voters WHERE voter_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, voterId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("has_voted") == 1;
            }
        } catch (SQLException e) {
            System.out.println("❌ Error checking vote status: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Update voter last login
     */
    private static void updateVoterLastLogin(String voterId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return;
        
        String sql = "UPDATE voters SET last_login = CURRENT_TIMESTAMP WHERE voter_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, voterId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("⚠️ Could not update last login: " + e.getMessage());
        }
    }
    
    /**
     * Add voter with email and image path (for portal imports with images)
     */
    public static boolean addVoterWithEmailAndImage(String voterId, String name, String email, String imagePath) {
        if (!SecurityUtils.isValidId(voterId)) {
            System.out.println("❌ Invalid voter ID format");
            return false;
        }
        
        if (!SecurityUtils.isValidName(name)) {
            System.out.println("❌ Invalid voter name");
            return false;
        }
        
        if (voterIdExists(voterId)) {
            System.out.println("⚠️ Voter already exists: " + voterId);
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "INSERT INTO voters (voter_id, name, email, image_path, is_registered) VALUES (?, ?, ?, ?, 0)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, voterId);
            stmt.setString(2, name);
            stmt.setString(3, email != null && !email.isEmpty() ? email : null);
            stmt.setString(4, imagePath != null && !imagePath.isEmpty() ? imagePath : null);
            stmt.executeUpdate();
            System.out.println("✅ Voter added with image: " + voterId + " (" + name + ")");
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Error adding voter: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update voter profile information (name, email, image)
     */
    public static boolean updateVoterProfile(String voterId, String name, String email, String imagePath) {
        if (!SecurityUtils.isValidId(voterId)) {
            System.out.println("❌ Invalid voter ID");
            return false;
        }
        
        if (name != null && !SecurityUtils.isValidName(name)) {
            System.out.println("❌ Invalid voter name");
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "UPDATE voters SET name = COALESCE(?, name), " +
                     "email = COALESCE(?, email), " +
                     "image_path = COALESCE(?, image_path) " +
                     "WHERE voter_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, email != null && !email.isEmpty() ? email : null);
            stmt.setString(3, imagePath != null && !imagePath.isEmpty() ? imagePath : null);
            stmt.setString(4, voterId);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Voter profile updated: " + voterId);
                return true;
            }
            System.out.println("⚠️ Voter not found: " + voterId);
            return false;
        } catch (SQLException e) {
            System.out.println("❌ Error updating voter profile: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get voter information from SQL database
     * Returns format: voterId:name:email:imagePath:isRegistered:hasVoted
     */
    public static String getVoterInfo(String voterId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return null;
        
        String sql = "SELECT voter_id, name, email, image_path, is_registered, has_voted FROM voters WHERE voter_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, voterId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String id = rs.getString("voter_id");
                String name = rs.getString("name");
                String email = rs.getString("email") != null ? rs.getString("email") : "";
                String imagePath = rs.getString("image_path") != null ? rs.getString("image_path") : "";
                int isRegistered = rs.getInt("is_registered");
                int hasVoted = rs.getInt("has_voted");
                
                return id + ":" + name + ":" + email + ":" + imagePath + ":" + isRegistered + ":" + hasVoted;
            }
        } catch (SQLException e) {
            System.out.println("❌ Error getting voter info: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Get voter email from SQL database
     */
    public static String getVoterEmail(String voterId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return null;
        
        String sql = "SELECT email FROM voters WHERE voter_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, voterId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("email");
            }
        } catch (SQLException e) {
            System.out.println("❌ Error getting voter email: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Get voter image path from SQL database
     */
    public static String getVoterImagePath(String voterId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return null;
        
        String sql = "SELECT image_path FROM voters WHERE voter_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, voterId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("image_path");
            }
        } catch (SQLException e) {
            System.out.println("❌ Error getting voter image: " + e.getMessage());
        }
        return null;
    }
    
    // ==================== EXISTING NOMINEE OPERATIONS ====================
    // (Keep all your existing nominee methods as they are)
    
    /**
     * Add a new nominee
     */
    public static boolean addNominee(String nomineeId, String name, String party) {
        if (!SecurityUtils.isValidId(nomineeId)) {
            System.out.println("❌ Invalid nominee ID format");
            return false;
        }
        
        if (!SecurityUtils.isValidName(name)) {
            System.out.println("❌ Invalid nominee name");
            return false;
        }
        
        if (party == null || party.trim().isEmpty()) {
            System.out.println("❌ Party name cannot be empty");
            return false;
        }
        
        if (nomineeExists(nomineeId)) {
            System.out.println("❌ Nominee already exists: " + nomineeId);
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "INSERT INTO nominees (nominee_id, name, party, is_active) VALUES (?, ?, ?, 1)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nomineeId);
            stmt.setString(2, name);
            stmt.setString(3, party);
            stmt.executeUpdate();
            System.out.println("✅ Nominee added: " + nomineeId);
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Error adding nominee: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete a nominee
     */
    public static boolean deleteNominee(String nomineeId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "UPDATE nominees SET is_active = 0 WHERE nominee_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nomineeId);
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("✅ Nominee deleted: " + nomineeId);
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Error deleting nominee: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Check if nominee exists
     */
    public static boolean nomineeExists(String nomineeId) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "SELECT 1 FROM nominees WHERE nominee_id = ? AND is_active = 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nomineeId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            System.out.println("❌ Error checking nominee existence: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get all active nominees
     */
    public static List<Nominee> getAllNominees() {
        List<Nominee> nominees = new ArrayList<>();
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return nominees;
        
        String sql = "SELECT nominee_id, name, party FROM nominees WHERE is_active = 1 ORDER BY nominee_id";
        
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Nominee nominee = new Nominee();
                nominee.setId(rs.getString("nominee_id"));
                nominee.setName(rs.getString("name"));
                nominee.setParty(rs.getString("party"));
                nominees.add(nominee);
            }
        } catch (SQLException e) {
            System.out.println("❌ Error retrieving nominees: " + e.getMessage());
        }
        
        return nominees;
    }
    
    // ==================== EXISTING VOTE OPERATIONS ====================
    // (Keep all your existing vote methods as they are)
    
    /**
     * Record a vote
     */
    public static boolean recordVote(String voterId, String nomineeId) {
        if (!voterIdExists(voterId)) {
            System.out.println("❌ Voter not found: " + voterId);
            return false;
        }
        
        if (!nomineeExists(nomineeId)) {
            System.out.println("❌ Nominee not found: " + nomineeId);
            return false;
        }
        
        if (hasVoterVoted(voterId)) {
            System.out.println("❌ Voter has already voted: " + voterId);
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        try {
            conn.setAutoCommit(false);
            
            // Record the vote
            String insertVoteSql = "INSERT INTO votes (voter_id, nominee_id) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertVoteSql)) {
                stmt.setString(1, voterId);
                stmt.setString(2, nomineeId);
                stmt.executeUpdate();
            }
            
            // Update voter's has_voted flag
            String updateVoterSql = "UPDATE voters SET has_voted = 1 WHERE voter_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateVoterSql)) {
                stmt.setString(1, voterId);
                stmt.executeUpdate();
            }
            
            conn.commit();
            System.out.println("✅ Vote recorded: " + voterId + " -> " + nomineeId);
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Error recording vote: " + e.getMessage());
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                System.out.println("⚠️ Rollback failed: " + rollbackEx.getMessage());
            }
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println("⚠️ Could not set autocommit: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get vote counts for all nominees
     */
    public static Map<String, Integer> getVoteCounts() {
        Map<String, Integer> counts = new HashMap<>();
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return counts;
        
        String sql = "SELECT nominee_id, COUNT(*) as vote_count FROM votes GROUP BY nominee_id";
        
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                counts.put(rs.getString("nominee_id"), rs.getInt("vote_count"));
            }
        } catch (SQLException e) {
            System.out.println("❌ Error getting vote counts: " + e.getMessage());
        }
        
        return counts;
    }
    
    /**
     * Get total votes cast
     */
    public static int getTotalVotesCast() {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return 0;
        
        String sql = "SELECT COUNT(*) as total FROM votes";
        
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.out.println("❌ Error getting total votes: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Get total registered voters
     */
    public static int getTotalRegisteredVoters() {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return 0;
        
        String sql = "SELECT COUNT(*) as total FROM voters WHERE is_registered = 1";
        
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.out.println("❌ Error getting registered voters: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Clear all votes (admin only, use with caution)
     */
    public static boolean clearVotes() {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        try {
            conn.setAutoCommit(false);
            
            // Clear votes
            String clearVotes = "DELETE FROM votes";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(clearVotes);
            }
            
            // Reset voter has_voted flags
            String resetVoters = "UPDATE voters SET has_voted = 0";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(resetVoters);
            }
            
            conn.commit();
            System.out.println("✅ All votes cleared");
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Error clearing votes: " + e.getMessage());
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                System.out.println("⚠️ Rollback failed: " + rollbackEx.getMessage());
            }
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println("⚠️ Could not set autocommit: " + e.getMessage());
            }
        }
    }
}