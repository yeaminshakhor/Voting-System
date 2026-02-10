package Data;

import java.io.*;
import java.net.*;
import javax.net.ssl.HttpsURLConnection;

/**
 * PortalIntegration - Handles integration with external institute portals.
 * Fetches student data (ID, Name, Email) from institute's student management system.
 * 
 * Usage:
 * 1. Configure portals in portal_config.txt
 * 2. Call fetchStudentData(portalType, query) to get voter data from portal
 * 3. Automatically creates voter records in the system
 */
public class PortalIntegration {
    
    private static final String CONFIG_FILE = "portal_config.txt";
    private static final int TIMEOUT = 10000; // 10 seconds timeout
    
    /**
     * Represents a student record fetched from portal
     */
    public static class StudentRecord {
        public String id;
        public String name;
        public String email;
        public String department;
        public String batch;
        public String status; // active, inactive, graduated
        public String dob;    // Date of birth (yyyy-MM-dd)
        public String bloodGroup; // Blood group (A+, O-, etc.)
        
        public StudentRecord(String id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.dob = "";
            this.bloodGroup = "";
        }
        
        public StudentRecord(String id, String name, String email, String department, String batch, String status) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.department = department;
            this.batch = batch;
            this.status = status;
            this.dob = "";
            this.bloodGroup = "";
        }
        
        public StudentRecord(String id, String name, String email, String department, String batch, String status, String dob, String bloodGroup) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.department = department;
            this.batch = batch;
            this.status = status;
            this.dob = dob != null ? dob : "";
            this.bloodGroup = bloodGroup != null ? bloodGroup : "";
        }
        
        @Override
        public String toString() {
            return String.format("%s:%s:%s:%s:%s:%s", id, name, email, department, batch, status);
        }
    }
    
    /**
     * Fetch student data from configured portal
     * 
     * @param portalType Type of portal (e.g., "moodle", "jisc", "ldap")
     * @param studentId The ID to search for
     * @return StudentRecord with all available data, or null if not found
     */
    public static StudentRecord fetchStudentData(String portalType, String studentId) {
        try {
            String portalUrl = getPortalUrl(portalType);
            if (portalUrl == null) {
                System.out.println("❌ Portal not configured: " + portalType);
                return null;
            }
            
            // Build query URL
            String query = buildQuery(portalUrl, studentId);
            
            System.out.println("🔍 Fetching student data from " + portalType);
            System.out.println("   Query: " + query);
            
            HttpsURLConnection connection = null;
            try {
                URL url = new URL(query);
                connection = (HttpsURLConnection) url.openConnection();
                connection.setConnectTimeout(TIMEOUT);
                connection.setReadTimeout(TIMEOUT);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");
                
                // Add authentication header if configured
                String authToken = getAuthToken(portalType);
                if (authToken != null) {
                    connection.setRequestProperty("Authorization", "Bearer " + authToken);
                }
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    return parseResponse(connection.getInputStream(), portalType);
                } else if (responseCode == 404) {
                    System.out.println("⚠️ Student not found in portal: " + studentId);
                    return null;
                } else {
                    System.out.println("❌ Portal error: HTTP " + responseCode);
                    return null;
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Portal integration error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Register a new voter from portal data
     */
    public static boolean registerVoterFromPortal(String portalType, String studentId) {
        StudentRecord student = fetchStudentData(portalType, studentId);
        if (student == null) {
            System.out.println("❌ Could not fetch student data from portal");
            return false;
        }
        
        // Check if voter already exists
        String[] voters = ElectionData.getAllVoters();
        for (String voter : voters) {
            String[] parts = voter.split(":");
            if (parts.length > 0 && parts[0].equals(student.id)) {
                System.out.println("⚠️ Voter already registered: " + student.id);
                return false;
            }
        }
        
        // Create new voter with empty password (for manual registration)
        String newVoter = student.id + ":" + student.name + ":" + (student.email != null ? student.email : "");
        System.out.println("✓ Voter data imported from portal: " + student.id);
        System.out.println("  Name: " + student.name);
        System.out.println("  Email: " + student.email);
        System.out.println("  DOB: " + (student.dob.isEmpty() ? "Not provided" : student.dob));
        System.out.println("  Blood Group: " + (student.bloodGroup.isEmpty() ? "Not provided" : student.bloodGroup));
        
        // Add voter and extended info to database
        return ElectionData.addVoterFromPortal(newVoter, student.dob, student.bloodGroup, student.department);
    }
    
    /**
     * Bulk import students from portal
     */
    public static int bulkImportFromPortal(String portalType, String[] studentIds) {
        int imported = 0;
        int failed = 0;
        
        System.out.println("📥 Bulk importing " + studentIds.length + " students from " + portalType);
        
        for (String studentId : studentIds) {
            if (registerVoterFromPortal(portalType, studentId)) {
                imported++;
            } else {
                failed++;
            }
        }
        
        System.out.println("✓ Import complete: " + imported + " imported, " + failed + " failed");
        return imported;
    }
    
    /**
     * Get portal URL from configuration
     */
    private static String getPortalUrl(String portalType) {
        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(portalType + "_url=")) {
                    return line.substring((portalType + "_url=").length()).trim();
                }
            }
        } catch (IOException e) {
            System.out.println("⚠️ Portal config file not found: " + CONFIG_FILE);
        }
        return null;
    }
    
    /**
     * Get authentication token from configuration
     */
    private static String getAuthToken(String portalType) {
        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(portalType + "_token=")) {
                    return line.substring((portalType + "_token=").length()).trim();
                }
            }
        } catch (IOException e) {
            // No token configured
        }
        return null;
    }
    
    /**
     * Build query URL for portal
     */
    private static String buildQuery(String baseUrl, String studentId) {
        if (baseUrl.contains("?")) {
            return baseUrl + "&student_id=" + URLEncoder.encode(studentId, java.nio.charset.StandardCharsets.UTF_8);
        } else {
            return baseUrl + "?student_id=" + URLEncoder.encode(studentId, java.nio.charset.StandardCharsets.UTF_8);
        }
    }
    
    /**
     * Parse response from portal
     */
    private static StudentRecord parseResponse(InputStream input, String portalType) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            
            // Simple JSON-like parsing (no external library needed)
            String jsonStr = response.toString();
            
            // Extract fields using simple string parsing
            String id = extractJsonField(jsonStr, "id", jsonStr.contains("username") ? "username" : "student_id");
            String name = extractJsonField(jsonStr, "fullname", jsonStr.contains("firstname") ? "firstname" : "name");
            String email = extractJsonField(jsonStr, "email", "");
            String department = extractJsonField(jsonStr, "department", "");
            String batch = extractJsonField(jsonStr, "batch", jsonStr.contains("ou") ? "ou" : "");
            String dob = extractJsonField(jsonStr, "dob", "date_of_birth", "birthdate", "birth_date");
            String bloodGroup = extractJsonField(jsonStr, "blood_group", "bloodtype", "blood_type", "bg");
            
            // Normalize DOB to yyyy-MM-dd format
            dob = normalizeDateFormat(dob);
            
            // Standardize blood group format (e.g., "A+" or "O-")
            bloodGroup = normalizeBloodGroup(bloodGroup);
            
            // Determine status
            String status = "active";
            if (jsonStr.contains("\"suspended\":1") || jsonStr.contains("\"suspended\":true")) {
                status = "inactive";
            }
            
            if (id.isEmpty() || name.isEmpty()) {
                System.out.println("❌ Could not parse student data from portal");
                return null;
            }
            
            return new StudentRecord(id, name, email, department, batch, status, dob, bloodGroup);
        } catch (Exception e) {
            System.out.println("❌ Error parsing portal response: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Normalize date to yyyy-MM-dd format
     * Tries to parse common date formats: yyyy-MM-dd, dd/MM/yyyy, MM/dd/yyyy, yyyy/MM/dd
     */
    private static String normalizeDateFormat(String date) {
        if (date == null || date.isEmpty()) return "";
        
        date = date.trim();
        
        // Already in correct format
        if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return date;
        }
        
        // dd/MM/yyyy
        if (date.matches("\\d{2}/\\d{2}/\\d{4}")) {
            String[] parts = date.split("/");
            return parts[2] + "-" + parts[1] + "-" + parts[0];
        }
        
        // MM/dd/yyyy
        if (date.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            String[] parts = date.split("/");
            if (parts[0].length() == 2 && parts[1].length() == 2) {
                // Could be either format; assume MM/dd/yyyy (US)
                return parts[2] + "-" + parts[0] + "-" + parts[1];
            }
        }
        
        // yyyy/MM/dd
        if (date.matches("\\d{4}/\\d{2}/\\d{2}")) {
            return date.replace("/", "-");
        }
        
        // If format is unclear, return as-is with warning
        System.out.println("⚠️  Could not normalize date format: " + date);
        return date;
    }
    
    /**
     * Normalize blood group format
     * Standardizes common variations: A+, O-, etc.
     */
    private static String normalizeBloodGroup(String bg) {
        if (bg == null || bg.isEmpty()) return "";
        
        bg = bg.trim().toUpperCase();
        
        // Valid blood groups
        String[] validBgs = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        for (String valid : validBgs) {
            if (bg.equalsIgnoreCase(valid)) {
                return valid;
            }
        }
        
        // Try to extract blood type from variations like "A Positive", "O negative"
        if (bg.contains("A") && (bg.contains("POS") || bg.contains("+"))) return "A+";
        if (bg.contains("A") && (bg.contains("NEG") || bg.contains("-"))) return "A-";
        if (bg.contains("B") && (bg.contains("POS") || bg.contains("+"))) return "B+";
        if (bg.contains("B") && (bg.contains("NEG") || bg.contains("-"))) return "B-";
        if (bg.contains("AB") && (bg.contains("POS") || bg.contains("+"))) return "AB+";
        if (bg.contains("AB") && (bg.contains("NEG") || bg.contains("-"))) return "AB-";
        if (bg.contains("O") && (bg.contains("POS") || bg.contains("+"))) return "O+";
        if (bg.contains("O") && (bg.contains("NEG") || bg.contains("-"))) return "O-";
        
        // If format is unclear, return as-is
        return bg;
    }
    
    /**
     * Extract JSON field value using simple string parsing
     */
    private static String extractJsonField(String jsonStr, String... fieldNames) {
        for (String fieldName : fieldNames) {
            // Look for "fieldName":"value" pattern
            String pattern1 = "\"" + fieldName + "\":\"";
            int idx = jsonStr.indexOf(pattern1);
            if (idx >= 0) {
                int start = idx + pattern1.length();
                int end = jsonStr.indexOf("\"", start);
                if (end > start) {
                    return jsonStr.substring(start, end);
                }
            }
            
            // Look for "fieldName":123 pattern (numbers)
            String pattern2 = "\"" + fieldName + "\":";
            idx = jsonStr.indexOf(pattern2);
            if (idx >= 0) {
                int start = idx + pattern2.length();
                int end = start;
                while (end < jsonStr.length() && Character.isDigit(jsonStr.charAt(end))) {
                    end++;
                }
                if (end > start) {
                    return jsonStr.substring(start, end);
                }
            }
        }
        return "";
    }
    
    /**
     * Test portal connection
     */
    public static boolean testConnection(String portalType) {
        String url = getPortalUrl(portalType);
        if (url == null) {
            System.out.println("❌ Portal URL not configured: " + portalType);
            return false;
        }
        
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(TIMEOUT);
            connection.setRequestMethod("HEAD");
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            if (responseCode == 200 || responseCode == 404) {
                System.out.println("✓ Portal connection successful: " + portalType);
                return true;
            } else {
                System.out.println("❌ Portal returned error: HTTP " + responseCode);
                return false;
            }
        } catch (Exception e) {
            System.out.println("❌ Portal connection failed: " + e.getMessage());
            return false;
        }
    }
}
