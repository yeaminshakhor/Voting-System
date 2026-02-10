package Utils;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AdminRole {
    // Role constants - MUST match these exactly in code
    public static final String SUPERADMIN = "SuperAdmin";
    public static final String VOTER_MANAGER = "VoterManager";
    public static final String NOMINEE_MANAGER = "NomineeManager";
    public static final String ELECTION_MANAGER = "ElectionManager";
    public static final String VIEW_ONLY = "ViewOnly";
    
    // Permission constants
    public static final String PERM_VIEW_ADMINS = "view_admins";
    public static final String PERM_MANAGE_ADMINS = "manage_admins";
    public static final String PERM_MANAGE_VOTERS = "manage_voters";
    public static final String PERM_MANAGE_NOMINEES = "manage_nominees";
    public static final String PERM_MANAGE_ELECTIONS = "manage_elections";
    public static final String PERM_VIEW_REPORTS = "view_reports";
    public static final String PERM_EXPORT_DATA = "export_data";
    public static final String PERM_CHANGE_PASSWORD = "change_password";
    public static final String PERM_VIEW_AUDIT_LOGS = "view_audit_logs";
    
    // Valid roles array
    private static final String[] VALID_ROLES = {
        SUPERADMIN,
        VOTER_MANAGER,
        NOMINEE_MANAGER,
        ELECTION_MANAGER,
        VIEW_ONLY
    };
    
    // Role to permissions mapping
    private static final Map<String, Set<String>> ROLE_PERMISSIONS = new HashMap<>();
    
    static {
        // Initialize role permissions
        
        // SuperAdmin has all permissions
        Set<String> superAdminPerms = new HashSet<>();
        superAdminPerms.add(PERM_VIEW_ADMINS);
        superAdminPerms.add(PERM_MANAGE_ADMINS);
        superAdminPerms.add(PERM_MANAGE_VOTERS);
        superAdminPerms.add(PERM_MANAGE_NOMINEES);
        superAdminPerms.add(PERM_MANAGE_ELECTIONS);
        superAdminPerms.add(PERM_VIEW_REPORTS);
        superAdminPerms.add(PERM_EXPORT_DATA);
        superAdminPerms.add(PERM_CHANGE_PASSWORD);
        superAdminPerms.add(PERM_VIEW_AUDIT_LOGS);
        ROLE_PERMISSIONS.put(SUPERADMIN, superAdminPerms);
        
        // VoterManager permissions
        Set<String> voterManagerPerms = new HashSet<>();
        voterManagerPerms.add(PERM_MANAGE_VOTERS);
        voterManagerPerms.add(PERM_VIEW_REPORTS);
        voterManagerPerms.add(PERM_CHANGE_PASSWORD);
        ROLE_PERMISSIONS.put(VOTER_MANAGER, voterManagerPerms);
        
        // NomineeManager permissions
        Set<String> nomineeManagerPerms = new HashSet<>();
        nomineeManagerPerms.add(PERM_MANAGE_NOMINEES);
        nomineeManagerPerms.add(PERM_VIEW_REPORTS);
        nomineeManagerPerms.add(PERM_CHANGE_PASSWORD);
        ROLE_PERMISSIONS.put(NOMINEE_MANAGER, nomineeManagerPerms);
        
        // ElectionManager permissions
        Set<String> electionManagerPerms = new HashSet<>();
        electionManagerPerms.add(PERM_MANAGE_ELECTIONS);
        electionManagerPerms.add(PERM_VIEW_REPORTS);
        electionManagerPerms.add(PERM_CHANGE_PASSWORD);
        ROLE_PERMISSIONS.put(ELECTION_MANAGER, electionManagerPerms);
        
        // ViewOnly permissions
        Set<String> viewOnlyPerms = new HashSet<>();
        viewOnlyPerms.add(PERM_VIEW_REPORTS);
        viewOnlyPerms.add(PERM_CHANGE_PASSWORD);
        ROLE_PERMISSIONS.put(VIEW_ONLY, viewOnlyPerms);
    }
    
    /**
     * Check if a role string is valid
     */
    public static boolean isValidRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return false;
        }
        
        String normalized = normalizeRole(role);
        for (String validRole : VALID_ROLES) {
            if (validRole.equals(normalized)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Normalize role string to standard format
     */
    public static String normalizeRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return VIEW_ONLY; // Default role
        }
        
        String trimmed = role.trim();
        
        // Check for common variations
        if (trimmed.equalsIgnoreCase("superadmin") || 
            trimmed.equalsIgnoreCase("super_admin") ||
            trimmed.equalsIgnoreCase("super admin")) {
            return SUPERADMIN;
        }
        
        if (trimmed.equalsIgnoreCase("votermanager") || 
            trimmed.equalsIgnoreCase("voter_manager") ||
            trimmed.equalsIgnoreCase("voter manager")) {
            return VOTER_MANAGER;
        }
        
        if (trimmed.equalsIgnoreCase("nomineemanager") || 
            trimmed.equalsIgnoreCase("nominee_manager") ||
            trimmed.equalsIgnoreCase("nominee manager")) {
            return NOMINEE_MANAGER;
        }
        
        if (trimmed.equalsIgnoreCase("electionmanager") || 
            trimmed.equalsIgnoreCase("election_manager") ||
            trimmed.equalsIgnoreCase("election manager")) {
            return ELECTION_MANAGER;
        }
        
        if (trimmed.equalsIgnoreCase("viewonly") || 
            trimmed.equalsIgnoreCase("view_only") ||
            trimmed.equalsIgnoreCase("view only")) {
            return VIEW_ONLY;
        }
        
        // If it matches exactly one of our valid roles, return it
        for (String validRole : VALID_ROLES) {
            if (validRole.equals(trimmed)) {
                return trimmed;
            }
        }
        
        // Default to ViewOnly for unknown roles
        return VIEW_ONLY;
    }
    
    /**
     * Get permissions for a role
     */
    public static Set<String> getPermissions(String role) {
        String normalizedRole = normalizeRole(role);
        Set<String> permissions = ROLE_PERMISSIONS.get(normalizedRole);
        
        if (permissions == null) {
            // Return default permissions if role not found
            permissions = new HashSet<>();
            permissions.add(PERM_CHANGE_PASSWORD);
        }
        
        return new HashSet<>(permissions); // Return copy
    }
    
    /**
     * Check if a role has a specific permission
     */
    public static boolean hasPermission(String role, String permission) {
        String normalizedRole = normalizeRole(role);
        Set<String> permissions = ROLE_PERMISSIONS.get(normalizedRole);
        
        if (permissions == null) {
            return false;
        }
        
        return permissions.contains(permission);
    }
    
    /**
     * Get all valid roles
     */
    public static String[] getAllRoles() {
        return VALID_ROLES.clone();
    }
    
    /**
     * Get role display name (formatted for UI)
     */
    public static String getDisplayName(String role) {
        String normalized = normalizeRole(role);
        
        switch (normalized) {
            case SUPERADMIN:
                return "Super Administrator";
            case VOTER_MANAGER:
                return "Voter Manager";
            case NOMINEE_MANAGER:
                return "Nominee Manager";
            case ELECTION_MANAGER:
                return "Election Manager";
            case VIEW_ONLY:
                return "View Only";
            default:
                return normalized;
        }
    }
    
    /**
     * Get role description
     */
    public static String getDescription(String role) {
        String normalized = normalizeRole(role);
        
        switch (normalized) {
            case SUPERADMIN:
                return "Full system access - can manage all admins and system settings";
            case VOTER_MANAGER:
                return "Can manage voter accounts and voter-related operations";
            case NOMINEE_MANAGER:
                return "Can manage nominee/candidate information";
            case ELECTION_MANAGER:
                return "Can manage election settings and results";
            case VIEW_ONLY:
                return "Can view reports and data but cannot make changes";
            default:
                return "Limited access role";
        }
    }
    
    /**
     * Check if role can manage other admins
     */
    public static boolean canManageAdmins(String role) {
        return hasPermission(role, PERM_MANAGE_ADMINS);
    }
    
    /**
     * Check if role can manage voters
     */
    public static boolean canManageVoters(String role) {
        return hasPermission(role, PERM_MANAGE_VOTERS);
    }
    
    /**
     * Check if role can manage nominees
     */
    public static boolean canManageNominees(String role) {
        return hasPermission(role, PERM_MANAGE_NOMINEES);
    }
    
    /**
     * Check if role can manage elections
     */
    public static boolean canManageElections(String role) {
        return hasPermission(role, PERM_MANAGE_ELECTIONS);
    }
    
    /**
     * Check if role is SuperAdmin
     */
    public static boolean isSuperAdmin(String role) {
        return SUPERADMIN.equals(normalizeRole(role));
    }
}