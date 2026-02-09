package Utils;

import java.util.*;

/**
 * Defines admin roles and their permissions.
 * SUPERADMIN role can control everything and add other admins.
 * Other operators have selective role-based permissions.
 */
public class AdminRole {
    
    // Role constants
    public static final String SUPERADMIN = "SuperAdmin";
    public static final String VOTER_MANAGER = "VoterManager";
    public static final String NOMINEE_MANAGER = "NomineeManager";
    public static final String ELECTION_MANAGER = "ElectionManager";
    public static final String REPORT_VIEWER = "ReportViewer";
    public static final String AUDIT_VIEWER = "AuditViewer";
    
    // Permission constants
    public static final String PERM_ADD_VOTER = "add_voter";
    public static final String PERM_DELETE_VOTER = "delete_voter";
    public static final String PERM_VIEW_VOTER = "view_voter";
    public static final String PERM_EDIT_VOTER = "edit_voter";
    
    public static final String PERM_ADD_NOMINEE = "add_nominee";
    public static final String PERM_DELETE_NOMINEE = "delete_nominee";
    public static final String PERM_VIEW_NOMINEE = "view_nominee";
    public static final String PERM_EDIT_NOMINEE = "edit_nominee";
    
    public static final String PERM_CONFIGURE_ELECTION = "configure_election";
    public static final String PERM_ACTIVATE_ELECTION = "activate_election";
    public static final String PERM_VIEW_ELECTION = "view_election";
    
    public static final String PERM_VIEW_RESULTS = "view_results";
    public static final String PERM_VIEW_AUDIT_LOG = "view_audit_log";
    
    private static final Map<String, Set<String>> ROLE_PERMISSIONS = new HashMap<>();
    
    static {
        // SuperAdmin: Full system access - can add admins and manage everything
        Set<String> superAdminPerms = new HashSet<>();
        superAdminPerms.add(PERM_ADD_VOTER);
        superAdminPerms.add(PERM_DELETE_VOTER);
        superAdminPerms.add(PERM_VIEW_VOTER);
        superAdminPerms.add(PERM_EDIT_VOTER);
        superAdminPerms.add(PERM_ADD_NOMINEE);
        superAdminPerms.add(PERM_DELETE_NOMINEE);
        superAdminPerms.add(PERM_VIEW_NOMINEE);
        superAdminPerms.add(PERM_EDIT_NOMINEE);
        superAdminPerms.add(PERM_CONFIGURE_ELECTION);
        superAdminPerms.add(PERM_ACTIVATE_ELECTION);
        superAdminPerms.add(PERM_VIEW_ELECTION);
        superAdminPerms.add(PERM_VIEW_RESULTS);
        superAdminPerms.add(PERM_VIEW_AUDIT_LOG);
        superAdminPerms.add("add_admin"); // SuperAdmin can add other admins
        superAdminPerms.add("delete_admin");
        superAdminPerms.add("manage_admins");
        ROLE_PERMISSIONS.put(SUPERADMIN, superAdminPerms);
        
        // VoterManager: Can manage voter registration and deletion
        Set<String> voterManagerPerms = new HashSet<>();
        voterManagerPerms.add(PERM_ADD_VOTER);
        voterManagerPerms.add(PERM_DELETE_VOTER);
        voterManagerPerms.add(PERM_VIEW_VOTER);
        voterManagerPerms.add(PERM_EDIT_VOTER);
        ROLE_PERMISSIONS.put(VOTER_MANAGER, voterManagerPerms);
        
        // NomineeManager: Can manage nominees
        Set<String> nomineeManagerPerms = new HashSet<>();
        nomineeManagerPerms.add(PERM_ADD_NOMINEE);
        nomineeManagerPerms.add(PERM_DELETE_NOMINEE);
        nomineeManagerPerms.add(PERM_VIEW_NOMINEE);
        nomineeManagerPerms.add(PERM_EDIT_NOMINEE);
        ROLE_PERMISSIONS.put(NOMINEE_MANAGER, nomineeManagerPerms);
        
        // ElectionManager: Can configure and manage election
        Set<String> electionManagerPerms = new HashSet<>();
        electionManagerPerms.add(PERM_CONFIGURE_ELECTION);
        electionManagerPerms.add(PERM_ACTIVATE_ELECTION);
        electionManagerPerms.add(PERM_VIEW_ELECTION);
        electionManagerPerms.add(PERM_VIEW_VOTER); // Can view voters for monitoring
        electionManagerPerms.add(PERM_VIEW_NOMINEE); // Can view nominees
        ROLE_PERMISSIONS.put(ELECTION_MANAGER, electionManagerPerms);
        
        // ReportViewer: Can only view results
        Set<String> reportViewerPerms = new HashSet<>();
        reportViewerPerms.add(PERM_VIEW_RESULTS);
        reportViewerPerms.add(PERM_VIEW_ELECTION);
        ROLE_PERMISSIONS.put(REPORT_VIEWER, reportViewerPerms);
        
        // AuditViewer: Can view audit logs only
        Set<String> auditViewerPerms = new HashSet<>();
        auditViewerPerms.add(PERM_VIEW_AUDIT_LOG);
        ROLE_PERMISSIONS.put(AUDIT_VIEWER, auditViewerPerms);
    }
    
    /**
     * Get all permissions for a role
     */
    public static Set<String> getPermissions(String role) {
        Set<String> perms = ROLE_PERMISSIONS.get(role);
        return perms != null ? new HashSet<>(perms) : new HashSet<>();
    }
    
    /**
     * Check if a role has a specific permission
     */
    public static boolean hasPermission(String role, String permission) {
        Set<String> perms = ROLE_PERMISSIONS.get(role);
        return perms != null && perms.contains(permission);
    }
    
    /**
     * Get all available roles
     */
    public static List<String> getAllRoles() {
        return new ArrayList<>(ROLE_PERMISSIONS.keySet());
    }
    
    /**
     * Get user-friendly description of a role
     */
    public static String getRoleDescription(String role) {
        return switch (role) {
            case SUPERADMIN -> "SuperAdmin - Full system control, can add/manage admins";
            case VOTER_MANAGER -> "Voter Manager - Manage voter registration and deletion";
            case NOMINEE_MANAGER -> "Nominee Manager - Manage election nominees";
            case ELECTION_MANAGER -> "Election Manager - Configure and manage election process";
            case REPORT_VIEWER -> "Report Viewer - View election results only";
            case AUDIT_VIEWER -> "Audit Viewer - Monitor system actions and logs";
            default -> "Unknown Role";
        };
    }
    
    /**
     * Validate if role is allowed in the system
     */
    public static boolean isValidRole(String role) {
        return ROLE_PERMISSIONS.containsKey(role);
    }
}
