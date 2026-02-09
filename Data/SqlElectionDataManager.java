package Data;

import Entities.Voter;
import Entities.Nominee;
import Utils.SecurityUtils;
import java.sql.*;
import java.util.*;

/**
 * SQL-based Election Data Manager - Replaces plain text file storage.
 * All voter, nominee, and vote operations are now in SQLite database.
 */
public class SqlElectionDataManager {
    
    // ==================== VOTER OPERATIONS ====================
    
    /**
     * Add a new voter to the database
     */
    public static boolean addVoter(String voterId, String name) {
        if (!SecurityUtils.isValidId(voterId)) {
            System.out.println("❌ Invalid voter ID format");
            return false;
        }
        
        if (!SecurityUtils.isValidName(name)) {
            System.out.println("❌ Invalid voter name");
            return false;
        }
        
        if (voterIdExists(voterId)) {
            System.out.println("❌ Voter already exists: " + voterId);
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) return false;
        
        String sql = "INSERT INTO voters (voter_id, name, is_registered) VALUES (?, ?, 0)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, voterId);
            stmt.setString(2, name);
            stmt.executeUpdate();
            System.out.println("✅ Voter added: " + voterId);
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
    
    // ==================== NOMINEE OPERATIONS ====================
    
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
    
    // ==================== VOTE OPERATIONS ====================
    
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
