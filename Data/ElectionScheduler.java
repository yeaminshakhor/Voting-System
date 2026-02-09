package Data;

import java.sql.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;

/**
 * Manages election scheduling - start/end times, active/inactive status
 * Database version with support for multiple elections
 */
public class ElectionScheduler {
    private static final String TABLE_NAME = "election_schedule";
    
    /**
     * Initialize the election schedule table
     */
    public static boolean initializeTable() {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            System.out.println("⚠️ Database not available. Using file-based configuration.");
            return true;
        }
        
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "election_name VARCHAR(100) NOT NULL, " +
                    "start_time TIMESTAMP NOT NULL, " +
                    "end_time TIMESTAMP NOT NULL, " +
                    "is_active BOOLEAN DEFAULT true, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "UNIQUE(election_name)" +
                    ")";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("✅ Election schedule table initialized");
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Error initializing election schedule table: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Set election schedule for a specific election
     */
    public static boolean setElectionSchedule(String electionName, Date startTime, Date endTime, boolean isActive) {
        // Validate input
        if (electionName == null || electionName.trim().isEmpty()) {
            System.out.println("❌ Election name cannot be empty");
            return false;
        }
        
        if (startTime == null || endTime == null) {
            System.out.println("❌ Start and end times cannot be null");
            return false;
        }
        
        if (startTime.after(endTime)) {
            System.out.println("❌ Start time must be before end time");
            return false;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            // Fallback to file-based configuration
            return setElectionScheduleFile(electionName, startTime, endTime, isActive);
        }
        
        // Check if election already exists
        String checkSql = "SELECT id FROM " + TABLE_NAME + " WHERE election_name = ?";
        
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setString(1, electionName);
            int rowsAffected = 0;
            try (ResultSet rs = checkStmt.executeQuery()) {

                if (rs.next()) {
                    // Update existing election
                    String sql = "UPDATE " + TABLE_NAME + " SET " +
                          "start_time = ?, end_time = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP " +
                          "WHERE election_name = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setTimestamp(1, new Timestamp(startTime.getTime()));
                        pstmt.setTimestamp(2, new Timestamp(endTime.getTime()));
                        pstmt.setBoolean(3, isActive);
                        pstmt.setString(4, electionName);
                        rowsAffected = pstmt.executeUpdate();
                    }
                } else {
                    // Insert new election
                    String sql = "INSERT INTO " + TABLE_NAME + " " +
                          "(election_name, start_time, end_time, is_active) " +
                          "VALUES (?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setString(1, electionName);
                        pstmt.setTimestamp(2, new Timestamp(startTime.getTime()));
                        pstmt.setTimestamp(3, new Timestamp(endTime.getTime()));
                        pstmt.setBoolean(4, isActive);
                        rowsAffected = pstmt.executeUpdate();
                    }
                }
            }

            if (rowsAffected > 0) {
                System.out.println("✅ Election schedule set for: " + electionName);
                return true;
            } else {
                System.out.println("❌ Failed to set election schedule");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("❌ Error setting election schedule: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Set default election schedule (for backward compatibility)
     */
    public static boolean setElectionSchedule(Date startTime, Date endTime, boolean isActive) {
        return setElectionSchedule("Default Election", startTime, endTime, isActive);
    }
    
    /**
     * Check if any election is currently active
     */
    public static boolean isElectionActive() {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            return isElectionActiveFile();
        }
        
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + 
                    " WHERE is_active = true AND " +
                    "start_time <= CURRENT_TIMESTAMP AND " +
                    "end_time >= CURRENT_TIMESTAMP";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                int count = rs.getInt(1);
                return count > 0;
            }
            return false;
        } catch (SQLException e) {
            System.out.println("❌ Error checking election status: " + e.getMessage());
            return isElectionActiveFile(); // Fallback to file
        }
    }
    
    /**
     * File-based check if any election is currently active
     */
    private static boolean isElectionActiveFile() {
        try {
            File dir = new File(".");
            File[] files = dir.listFiles((d, name) -> name.startsWith("election_schedule_") && name.endsWith(".txt"));
            if (files == null) return false;
            
            long currentTime = System.currentTimeMillis();
            for (File file : files) {
                java.util.Map<String, String> data = readElectionFile(file);
                if (data.containsKey("is_active") && Boolean.parseBoolean(data.get("is_active"))) {
                    long startTime = Long.parseLong(data.getOrDefault("start_time", "0"));
                    long endTime = Long.parseLong(data.getOrDefault("end_time", "0"));
                    if (currentTime >= startTime && currentTime <= endTime) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error checking election status from files: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Check if a specific election is active
     */
    public static boolean isElectionActive(String electionName) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            return isElectionActiveFile(electionName);
        }
        
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + 
                    " WHERE election_name = ? AND is_active = true AND " +
                    "start_time <= CURRENT_TIMESTAMP AND " +
                    "end_time >= CURRENT_TIMESTAMP";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, electionName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                }
                return false;
            }
        } catch (SQLException e) {
            System.out.println("❌ Error checking specific election status: " + e.getMessage());
            return isElectionActiveFile(electionName); // Fallback to file
        }
    }
    
    /**
     * File-based check if a specific election is active
     */
    private static boolean isElectionActiveFile(String electionName) {
        try {
            String fileName = "election_schedule_" + electionName.replace(" ", "_") + ".txt";
            File file = new File(fileName);
            if (!file.exists()) return false;
            
            java.util.Map<String, String> data = readElectionFile(file);
            if (!Boolean.parseBoolean(data.getOrDefault("is_active", "false"))) {
                return false;
            }
            
            long currentTime = System.currentTimeMillis();
            long startTime = Long.parseLong(data.getOrDefault("start_time", "0"));
            long endTime = Long.parseLong(data.getOrDefault("end_time", "0"));
            return currentTime >= startTime && currentTime <= endTime;
        } catch (Exception e) {
            System.out.println("⚠️ Error checking election status from file: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get election status message
     */
    public static String getElectionStatus() {
        return getElectionStatus("Default Election");
    }
    
    /**
     * Get specific election status message
     */
    public static String getElectionStatus(String electionName) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            return getElectionStatusFile(electionName);
        }
        
        String sql = "SELECT election_name, start_time, end_time, is_active " +
                    "FROM " + TABLE_NAME + " WHERE election_name = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, electionName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("election_name");
                    Timestamp startTime = rs.getTimestamp("start_time");
                    Timestamp endTime = rs.getTimestamp("end_time");
                    boolean isActive = rs.getBoolean("is_active");

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                    if (!isActive) {
                        return name + " is INACTIVE (manually disabled)";
                    }

                    long currentTime = System.currentTimeMillis();

                    if (currentTime < startTime.getTime()) {
                        return name + " will start on: " + sdf.format(startTime);
                    } else if (currentTime > endTime.getTime()) {
                        return name + " ended on: " + sdf.format(endTime);
                    } else {
                        return name + " is ACTIVE (ends: " + sdf.format(endTime) + ")";
                    }
                }
            }
            // No election found
            return "Election '" + electionName + "' not found";
        } catch (SQLException e) {
            System.out.println("❌ Error getting election status: " + e.getMessage());
            return getElectionStatusFile(electionName); // Fallback to file
        }
    }
    
    /**
     * File-based get election status
     */
    private static String getElectionStatusFile(String electionName) {
        try {
            String fileName = "election_schedule_" + electionName.replace(" ", "_") + ".txt";
            File file = new File(fileName);
            if (!file.exists()) {
                return "Election '" + electionName + "' not found";
            }
            
            java.util.Map<String, String> data = readElectionFile(file);
            String name = data.getOrDefault("election_name", electionName);
            boolean isActive = Boolean.parseBoolean(data.getOrDefault("is_active", "false"));
            long startTime = Long.parseLong(data.getOrDefault("start_time", "0"));
            long endTime = Long.parseLong(data.getOrDefault("end_time", "0"));
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            if (!isActive) {
                return name + " is INACTIVE (manually disabled)";
            }
            
            long currentTime = System.currentTimeMillis();
            
            if (currentTime < startTime) {
                return name + " will start on: " + sdf.format(new java.util.Date(startTime));
            } else if (currentTime > endTime) {
                return name + " ended on: " + sdf.format(new java.util.Date(endTime));
            } else {
                return name + " is ACTIVE (ends: " + sdf.format(new java.util.Date(endTime)) + ")";
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting election status from file: " + e.getMessage());
            return "Error retrieving election status";
        }
    }
    
    /**
     * Get all elections status
     */
    public static String getAllElectionsStatus() {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            return getAllElectionsStatusFile();
        }
        
        String sql = "SELECT election_name, start_time, end_time, is_active " +
                    "FROM " + TABLE_NAME + " ORDER BY start_time";
        
        StringBuilder status = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int count = 0;
            while (rs.next()) {
                count++;
                if (status.length() > 0) {
                    status.append("\n");
                }
                
                String name = rs.getString("election_name");
                Timestamp startTime = rs.getTimestamp("start_time");
                Timestamp endTime = rs.getTimestamp("end_time");
                boolean isActive = rs.getBoolean("is_active");
                
                long currentTime = System.currentTimeMillis();
                
                status.append(count).append(". ").append(name).append(": ");
                
                if (!isActive) {
                    status.append("INACTIVE");
                } else if (currentTime < startTime.getTime()) {
                    status.append("Starts at ").append(sdf.format(startTime));
                } else if (currentTime > endTime.getTime()) {
                    status.append("Ended at ").append(sdf.format(endTime));
                } else {
                    status.append("ACTIVE (ends at ").append(sdf.format(endTime)).append(")");
                }
            }
            
            if (count == 0) {
                return "No elections scheduled";
            }
            
            return status.toString();
            
        } catch (SQLException e) {
            System.out.println("❌ Error getting all elections status: " + e.getMessage());
            return getAllElectionsStatusFile(); // Fallback to file
        }
    }
    
    /**
     * File-based get all elections status
     */
    private static String getAllElectionsStatusFile() {
        try {
            File dir = new File(".");
            File[] files = dir.listFiles((d, name) -> name.startsWith("election_schedule_") && name.endsWith(".txt"));
            if (files == null || files.length == 0) {
                return "No elections scheduled";
            }
            
            StringBuilder status = new StringBuilder();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            int count = 0;
            for (File file : files) {
                java.util.Map<String, String> data = readElectionFile(file);
                count++;
                if (status.length() > 0) {
                    status.append("\n");
                }
                
                String name = data.getOrDefault("election_name", file.getName());
                boolean isActive = Boolean.parseBoolean(data.getOrDefault("is_active", "false"));
                long startTime = Long.parseLong(data.getOrDefault("start_time", "0"));
                long endTime = Long.parseLong(data.getOrDefault("end_time", "0"));
                
                long currentTime = System.currentTimeMillis();
                
                status.append(count).append(". ").append(name).append(": ");
                
                if (!isActive) {
                    status.append("INACTIVE");
                } else if (currentTime < startTime) {
                    status.append("Starts at ").append(sdf.format(new java.util.Date(startTime)));
                } else if (currentTime > endTime) {
                    status.append("Ended at ").append(sdf.format(new java.util.Date(endTime)));
                } else {
                    status.append("ACTIVE (ends at ").append(sdf.format(new java.util.Date(endTime))).append(")");
                }
            }
            
            return status.toString();
        } catch (Exception e) {
            System.out.println("⚠️ Error getting elections status from files: " + e.getMessage());
            return "Error retrieving elections status";
        }
    }
    
    /**
     * Activate/Deactivate an election
     */
    public static boolean setElectionActive(String electionName, boolean active) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            return setElectionActiveFile(electionName, active);
        }
        
        String sql = "UPDATE " + TABLE_NAME + " SET is_active = ? WHERE election_name = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBoolean(1, active);
            pstmt.setString(2, electionName);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✅ Election '" + electionName + "' " + (active ? "activated" : "deactivated"));
                return true;
            } else {
                System.out.println("❌ Election '" + electionName + "' not found");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("❌ Error updating election status: " + e.getMessage());
            return setElectionActiveFile(electionName, active); // Fallback to file
        }
    }
    
    /**
     * File-based activate/deactivate election
     */
    private static boolean setElectionActiveFile(String electionName, boolean active) {
        try {
            String fileName = "election_schedule_" + electionName.replace(" ", "_") + ".txt";
            File file = new File(fileName);
            if (!file.exists()) {
                return false;
            }
            
            java.util.Map<String, String> data = readElectionFile(file);
            data.put("is_active", String.valueOf(active));
            
            try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(fileName))) {
                for (java.util.Map.Entry<String, String> entry : data.entrySet()) {
                    writer.println(entry.getKey() + ":" + entry.getValue());
                }
            }
            System.out.println("✅ Election '" + electionName + "' " + (active ? "activated" : "deactivated") + " (file)");
            return true;
        } catch (Exception e) {
            System.out.println("⚠️ Error updating election status in file: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Activate/Deactivate default election (for backward compatibility)
     */
    public static boolean setElectionActive(boolean active) {
        return setElectionActive("Default Election", active);
    }
    
    /**
     * Check if voting is allowed (combines schedule + status checks)
     */
    public static boolean isVotingAllowed() {
        return isElectionActive();
    }
    
    /**
     * Check if voting is allowed for specific election
     */
    public static boolean isVotingAllowed(String electionName) {
        return isElectionActive(electionName);
    }
    
    /**
     * Get current active election name
     */
    public static String getCurrentActiveElection() {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            return getCurrentActiveElectionFile();
        }
        
        String sql = "SELECT election_name FROM " + TABLE_NAME + 
                    " WHERE is_active = true AND " +
                    "start_time <= CURRENT_TIMESTAMP AND " +
                    "end_time >= CURRENT_TIMESTAMP " +
                    "ORDER BY start_time DESC LIMIT 1";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getString("election_name");
            }
            return null;
        } catch (SQLException e) {
            System.out.println("❌ Error getting current active election: " + e.getMessage());
            return getCurrentActiveElectionFile(); // Fallback to file
        }
    }
    
    /**
     * File-based get current active election
     */
    private static String getCurrentActiveElectionFile() {
        try {
            File dir = new File(".");
            File[] files = dir.listFiles((d, name) -> name.startsWith("election_schedule_") && name.endsWith(".txt"));
            if (files == null) return null;
            
            long currentTime = System.currentTimeMillis();
            for (File file : files) {
                java.util.Map<String, String> data = readElectionFile(file);
                boolean isActive = Boolean.parseBoolean(data.getOrDefault("is_active", "false"));
                long startTime = Long.parseLong(data.getOrDefault("start_time", "0"));
                long endTime = Long.parseLong(data.getOrDefault("end_time", "0"));
                
                if (isActive && currentTime >= startTime && currentTime <= endTime) {
                    return data.getOrDefault("election_name", file.getName());
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting current active election from files: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Get election end time for countdown display
     */
    public static Date getElectionEndTime() {
        return getElectionEndTime(getCurrentActiveElection());
    }
    
    /**
     * Get specific election end time
     */
    public static Date getElectionEndTime(String electionName) {
        if (electionName == null) {
            return null;
        }
        
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            return getElectionEndTimeFile(electionName);
        }
        
        String sql = "SELECT end_time FROM " + TABLE_NAME + 
                    " WHERE election_name = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, electionName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp t = rs.getTimestamp("end_time");
                    return t != null ? new Date(t.getTime()) : null;
                }
                return null;
            }
        } catch (SQLException e) {
            System.out.println("❌ Error getting election end time: " + e.getMessage());
            return getElectionEndTimeFile(electionName); // Fallback to file
        }
    }
    
    /**
     * File-based get election end time
     */
    private static Date getElectionEndTimeFile(String electionName) {
        try {
            String fileName = "election_schedule_" + electionName.replace(" ", "_") + ".txt";
            File file = new File(fileName);
            if (!file.exists()) {
                return null;
            }
            
            java.util.Map<String, String> data = readElectionFile(file);
            long endTime = Long.parseLong(data.getOrDefault("end_time", "0"));
            return endTime > 0 ? new Date(endTime) : null;
        } catch (Exception e) {
            System.out.println("⚠️ Error getting election end time from file: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Delete an election schedule
     */
    public static boolean deleteElection(String electionName) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            return deleteElectionFile(electionName);
        }
        
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE election_name = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, electionName);
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✅ Election '" + electionName + "' deleted");
                return true;
            } else {
                System.out.println("❌ Election '" + electionName + "' not found");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("❌ Error deleting election: " + e.getMessage());
            return deleteElectionFile(electionName); // Fallback to file
        }
    }
    
    /**
     * File-based delete election
     */
    private static boolean deleteElectionFile(String electionName) {
        try {
            String fileName = "election_schedule_" + electionName.replace(" ", "_") + ".txt";
            File file = new File(fileName);
            if (!file.exists()) {
                System.out.println("❌ Election '" + electionName + "' not found");
                return false;
            }
            
            if (file.delete()) {
                System.out.println("✅ Election '" + electionName + "' deleted (file)");
                return true;
            } else {
                System.out.println("❌ Could not delete election file");
                return false;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error deleting election from file: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get all scheduled elections
     */
    public static List<String> getAllElections() {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            return getAllElectionsFile();
        }
        
        String sql = "SELECT election_name FROM " + TABLE_NAME + " ORDER BY start_time";
        List<String> elections = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                elections.add(rs.getString("election_name"));
            }

            return elections;
        } catch (SQLException e) {
            System.out.println("❌ Error getting all elections: " + e.getMessage());
            return getAllElectionsFile(); // Fallback to file
        }
    }
    
    /**
     * File-based get all elections
     */
    private static List<String> getAllElectionsFile() {
        List<String> elections = new ArrayList<>();
        try {
            File dir = new File(".");
            File[] files = dir.listFiles((d, name) -> name.startsWith("election_schedule_") && name.endsWith(".txt"));
            if (files == null) return elections;
            
            for (File file : files) {
                java.util.Map<String, String> data = readElectionFile(file);
                String name = data.getOrDefault("election_name", file.getName());
                elections.add(name);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error getting elections from files: " + e.getMessage());
        }
        return elections;
    }
    
    /**
     * Get election details
     */
    public static ElectionDetails getElectionDetails(String electionName) {
        Connection conn = DatabaseManager.getConnection();
        if (conn == null) {
            return getElectionDetailsFile(electionName);
        }
        
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE election_name = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, electionName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp createdTs = rs.getTimestamp("created_at");
                    Timestamp updatedTs = rs.getTimestamp("updated_at");

                    Date createdAt = createdTs != null ? new Date(createdTs.getTime()) : new Date();
                    Date updatedAt = updatedTs != null ? new Date(updatedTs.getTime()) : new Date();

                    return new ElectionDetails(
                        rs.getString("election_name"),
                        new Date(rs.getTimestamp("start_time").getTime()),
                        new Date(rs.getTimestamp("end_time").getTime()),
                        rs.getBoolean("is_active"),
                        createdAt,
                        updatedAt
                    );
                }
                return null;
            }
        } catch (SQLException e) {
            System.out.println("❌ Error getting election details: " + e.getMessage());
            return getElectionDetailsFile(electionName); // Fallback to file
        }
    }
    
    /**
     * File-based get election details
     */
    private static ElectionDetails getElectionDetailsFile(String electionName) {
        try {
            String fileName = "election_schedule_" + electionName.replace(" ", "_") + ".txt";
            File file = new File(fileName);
            if (!file.exists()) {
                return null;
            }
            
            java.util.Map<String, String> data = readElectionFile(file);
            String name = data.getOrDefault("election_name", electionName);
            long startTime = Long.parseLong(data.getOrDefault("start_time", "0"));
            long endTime = Long.parseLong(data.getOrDefault("end_time", "0"));
            boolean isActive = Boolean.parseBoolean(data.getOrDefault("is_active", "false"));
            
            Date now = new Date();
            return new ElectionDetails(
                name,
                new Date(startTime),
                new Date(endTime),
                isActive,
                now,
                now
            );
        } catch (Exception e) {
            System.out.println("⚠️ Error getting election details from file: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Helper class to store election details
     */
    public static class ElectionDetails {
        private String electionName;
        private Date startTime;
        private Date endTime;
        private boolean isActive;
        private Date createdAt;
        private Date updatedAt;
        
        public ElectionDetails(String electionName, Date startTime, Date endTime, 
                              boolean isActive, Date createdAt, Date updatedAt) {
            this.electionName = electionName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.isActive = isActive;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
        
        // Getters
        public String getElectionName() { return electionName; }
        public Date getStartTime() { return startTime; }
        public Date getEndTime() { return endTime; }
        public boolean isActive() { return isActive; }
        public Date getCreatedAt() { return createdAt; }
        public Date getUpdatedAt() { return updatedAt; }
        
        @Override
        public String toString() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return String.format("Election: %s\n" +
                               "Start: %s\n" +
                               "End: %s\n" +
                               "Status: %s\n" +
                               "Created: %s\n" +
                               "Last Updated: %s",
                               electionName,
                               sdf.format(startTime),
                               sdf.format(endTime),
                               isActive ? "Active" : "Inactive",
                               sdf.format(createdAt),
                               sdf.format(updatedAt));
        }
    }
    
    /**
     * Migrate from properties file to database (one-time operation)
     */
    public static boolean migrateFromPropertiesFile() {
        Properties props = new Properties();
        // Prefer text file if present, fall back to properties
        String propertiesFile = "election_config.txt";
        File f = new File(propertiesFile);
        if (!f.exists()) {
            propertiesFile = "election_config.properties";
        }

        try (FileInputStream in = new FileInputStream(propertiesFile)) {
            props.load(in);

            String startTimeStr = props.getProperty("startTime");
            String endTimeStr = props.getProperty("endTime");
            String isActiveStr = props.getProperty("isActive", "true");

            if (startTimeStr != null && endTimeStr != null) {
                Date startTime = new Date(Long.parseLong(startTimeStr));
                Date endTime = new Date(Long.parseLong(endTimeStr));
                boolean isActive = Boolean.parseBoolean(isActiveStr);

                // Initialize table if needed
                initializeTable();

                // Migrate to database
                boolean success = setElectionSchedule("Migrated Election", startTime, endTime, isActive);

                if (success) {
                    System.out.println("✅ Successfully migrated election schedule from properties file");
                    return true;
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("ℹ️ No properties file to migrate or error reading: " + e.getMessage());
        }
        return false;
    }

    /**
     * File-based fallback for election schedule when database is not available
     */
    private static boolean setElectionScheduleFile(String electionName, Date startTime, Date endTime, boolean isActive) {
        try {
            String electionFile = "election_schedule_" + electionName.replace(" ", "_") + ".txt";
            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(electionFile));
            writer.println("election_name:" + electionName);
            writer.println("start_time:" + startTime.getTime());
            writer.println("end_time:" + endTime.getTime());
            writer.println("is_active:" + isActive);
            writer.close();
            System.out.println("✅ Election schedule saved to file: " + electionFile);
            return true;
        } catch (IOException e) {
            System.out.println("❌ Error saving election schedule to file: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Helper method to read election file and return key-value pairs
     */
    private static java.util.Map<String, String> readElectionFile(File file) throws IOException {
        java.util.Map<String, String> data = new java.util.HashMap<>();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                int colonIdx = line.indexOf(':');
                if (colonIdx > 0) {
                    String key = line.substring(0, colonIdx).trim();
                    String value = line.substring(colonIdx + 1).trim();
                    data.put(key, value);
                }
            }
        }
        return data;
    }
}
