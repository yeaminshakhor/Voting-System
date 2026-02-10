import Framesg.AdminLogin;
import Framesg.VoterLogin;
import Framesg.VoterRegistration;
import Data.DatabaseManager;
import Data.DataMigrationUtility;
import Data.SqlAdminManager;
import Data.SqlElectionDataManager;
import javax.swing.*;
import java.awt.*;

/**
 * Main entry point for the election management system (SQL Version).
 * Initializes the SQL database and handles data migration if needed.
 */
public class Main {
    private static final Color NAVY_BLUE = new Color(25, 25, 112);
    private static final Color DODGER_BLUE = new Color(30, 144, 255);
    private static final Color LIGHT_BLUE = new Color(173, 216, 230);
    private static final int WINDOW_WIDTH = 400;
    private static final int WINDOW_HEIGHT = 300;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  Election Management System v2.0");
        System.out.println("  (SQL Database Version)");
        System.out.println("========================================\n");
        
        // Step 1: Initialize database FIRST
        System.out.println("🔄 Step 1: Initializing Database...");
        // Use DatabaseManager initialization and verify connection
        DatabaseManager.initializeDatabase();
        boolean dbInitialized = DatabaseManager.getConnection() != null;

        if (!dbInitialized) {
            JOptionPane.showMessageDialog(null,
                "❌ CRITICAL ERROR: Cannot connect to database!\n\n" +
                "Please check:\n" +
                "1. SQLite JDBC driver is installed\n" +
                "2. Write permissions in current directory\n" +
                "3. Disk space is available",
                "Database Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        System.out.println("✅ Database initialized successfully");
        
        // Step 2: Initialize Admin System with proper schema
        System.out.println("\n🔄 Step 2: Initializing Admin System...");
        initializeAdminSystemWithRetry();
        
        // Step 3: Check for data migration
        System.out.println("\n🔄 Step 3: Checking for data migration...");
        handleDataMigration();
        
        // Step 4: Create default SuperAdmin if needed
        System.out.println("\n🔄 Step 4: Ensuring default SuperAdmin...");
        SqlAdminManager.ensureDefaultSuperAdmin();
        
        // Step 5: Show login statistics
        showSystemStatus();
        
        // Step 6: Create and show main window
        System.out.println("\n🚀 Starting GUI...");
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }
    
    /**
     * Initialize admin system with retry logic for schema issues
     */
    private static void initializeAdminSystemWithRetry() {
        try {
            // First try to initialize normally
            SqlAdminManager.initializeAdminSystem();
            System.out.println("✅ Admin system initialized");
        } catch (Exception e) {
            System.out.println("⚠️ First initialization failed: " + e.getMessage());
            System.out.println("🔄 Attempting schema repair...");
            
            try {
                // Try to fix database schema
                DatabaseManager.fixDatabaseSchema();
                
                // Try initialization again
                SqlAdminManager.initializeAdminSystem();
                System.out.println("✅ Admin system initialized after repair");
            } catch (Exception e2) {
                System.out.println("❌ Schema repair failed: " + e2.getMessage());
                System.out.println("⚠️ Continuing with basic initialization...");
            }
        }
    }
    
    /**
     * Handle data migration from text files to SQL
     */
    private static void handleDataMigration() {
        if (DataMigrationUtility.isMigrationNeeded()) {
            int response = JOptionPane.showConfirmDialog(null,
                "⚠️  PLAIN TEXT DATABASE FILES DETECTED\n\n" +
                "Old text files (database_*.txt) have been found.\n\n" +
                "Would you like to migrate your data to the SQL system?\n\n" +
                "✅ Recommended: Migrate for better security & performance\n" +
                "❌ Cancel: Continue with text files (not recommended)\n\n" +
                "Note: Original files will be backed up automatically.",
                "Data Migration Available",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (response == JOptionPane.YES_OPTION) {
                System.out.println("\n🔄 Starting data migration...");
                
                // Show migration progress dialog
                JDialog progressDialog = createMigrationProgressDialog();
                progressDialog.setVisible(true);
                
                // Run migration in background
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        DataMigrationUtility.migrateAllData();
                        return null;
                    }
                    
                    @Override
                    protected void done() {
                        progressDialog.dispose();
                        
                        // Show completion message
                        JOptionPane.showMessageDialog(null,
                            "✅ DATA MIGRATION COMPLETED!\n\n" +
                            "Your data has been successfully migrated to SQL.\n\n" +
                            "Migration Summary:\n" +
                            "• Admin accounts migrated\n" +
                            "• Voter data migrated\n" +
                            "• Nominee data migrated\n" +
                            "• Voting records migrated\n\n" +
                            "Original files have been backed up as *.backup",
                            "Migration Complete", 
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        System.out.println("✅ Migration completed!");
                    }
                };
                worker.execute();
            } else {
                System.out.println("⚠️ Migration cancelled by user");
                JOptionPane.showMessageDialog(null,
                    "⚠️ Migration Cancelled\n\n" +
                    "You have chosen not to migrate data.\n" +
                    "The system will continue using text files.\n\n" +
                    "Note: Some features may not work correctly.",
                    "Migration Cancelled",
                    JOptionPane.WARNING_MESSAGE);
            }
        } else {
            System.out.println("✅ No migration needed - SQL database is ready");
        }
    }
    
    /**
     * Create migration progress dialog
     */
    private static JDialog createMigrationProgressDialog() {
        JDialog dialog = new JDialog((JFrame) null, "Data Migration", true);
        dialog.setSize(350, 150);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Migrating Data to SQL Database...", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));
        
        JLabel infoLabel = new JLabel("Please wait, this may take a moment...", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 20, 10));
        
        dialog.add(titleLabel, BorderLayout.NORTH);
        dialog.add(progressBar, BorderLayout.CENTER);
        dialog.add(infoLabel, BorderLayout.SOUTH);
        
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        return dialog;
    }
    
    /**
     * Show system status information
     */
    private static void showSystemStatus() {
        try {
            int adminCount = SqlAdminManager.getAllAdmins().size();
            int voterCount = SqlElectionDataManager.getAllVoters().size();
            int nomineeCount = SqlElectionDataManager.getAllNominees().size();
            
            System.out.println("\n📊 SYSTEM STATUS:");
            System.out.println("   • Admins: " + adminCount);
            System.out.println("   • Voters: " + voterCount);
            System.out.println("   • Nominees: " + nomineeCount);
            
            if (adminCount == 0) {
                System.out.println("⚠️  WARNING: No admin accounts found!");
                System.out.println("   Creating default admin: superadmin/super123");
            }
            
        } catch (Exception e) {
            System.out.println("⚠️ Could not get system status: " + e.getMessage());
        }
    }
    
    /**
     * Create and show the main GUI
     */
    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Election Management System v2.0");
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(NAVY_BLUE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel titleLabel = new JLabel("Election Management System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(LIGHT_BLUE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);
        
        JLabel versionLabel = new JLabel("(SQL Database Version)", SwingConstants.CENTER);
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        versionLabel.setForeground(new Color(200, 200, 200));
        gbc.gridy = 1;
        panel.add(versionLabel, gbc);
        
        // Add system info label
        try {
            int adminCount = SqlAdminManager.getAllAdmins().size();
            JLabel infoLabel = new JLabel("Admins: " + adminCount + " | Database: Ready", SwingConstants.CENTER);
            infoLabel.setFont(new Font("Arial", Font.ITALIC, 10));
            infoLabel.setForeground(new Color(150, 200, 255));
            gbc.gridy = 2;
            panel.add(infoLabel, gbc);
        } catch (Exception e) {
            // Ignore if status can't be shown
        }

        JButton adminLoginButton = new JButton("Admin Login");
        adminLoginButton.setFont(new Font("Arial", Font.BOLD, 14));
        adminLoginButton.setBackground(DODGER_BLUE);
        adminLoginButton.setForeground(Color.WHITE);
        adminLoginButton.setFocusPainted(false);
        adminLoginButton.setPreferredSize(new Dimension(150, 40));
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        panel.add(adminLoginButton, gbc);

        JButton voterLoginButton = new JButton("Voter Login");
        voterLoginButton.setFont(new Font("Arial", Font.BOLD, 14));
        voterLoginButton.setBackground(DODGER_BLUE);
        voterLoginButton.setForeground(Color.WHITE);
        voterLoginButton.setFocusPainted(false);
        voterLoginButton.setPreferredSize(new Dimension(150, 40));
        gbc.gridx = 1;
        panel.add(voterLoginButton, gbc);

        JButton voterRegisterButton = new JButton("Voter Registration");
        voterRegisterButton.setFont(new Font("Arial", Font.BOLD, 14));
        voterRegisterButton.setBackground(DODGER_BLUE);
        voterRegisterButton.setForeground(Color.WHITE);
        voterRegisterButton.setFocusPainted(false);
        voterRegisterButton.setPreferredSize(new Dimension(320, 40));
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(voterRegisterButton, gbc);
        
        // Add emergency button (hidden by default)
        JButton emergencyButton = new JButton("Emergency Setup");
        emergencyButton.setFont(new Font("Arial", Font.PLAIN, 10));
        emergencyButton.setBackground(new Color(255, 100, 100));
        emergencyButton.setForeground(Color.WHITE);
        emergencyButton.setFocusPainted(false);
        emergencyButton.setVisible(false); // Hidden by default
        gbc.gridy = 5;
        panel.add(emergencyButton, gbc);

        frame.add(panel);
        frame.setVisible(true);

        adminLoginButton.addActionListener(e -> {
            frame.setVisible(false);
            new AdminLogin(frame).setVisible(true);
        });

        voterLoginButton.addActionListener(e -> {
            frame.setVisible(false);
            new VoterLogin(frame).setVisible(true);
        });

        voterRegisterButton.addActionListener(e -> {
            frame.setVisible(false);
            new VoterRegistration(frame).setVisible(true);
        });
        
        emergencyButton.addActionListener(e -> {
            handleEmergencySetup(frame);
        });
        
        // Check if we need to show emergency button
        try {
            int adminCount = SqlAdminManager.getAllAdmins().size();
            if (adminCount == 0) {
                emergencyButton.setVisible(true);
                emergencyButton.setText("⚠️ NO ADMINS - Click to Setup");
            }
        } catch (Exception e) {
            emergencyButton.setVisible(true);
            emergencyButton.setText("⚠️ SYSTEM ERROR - Click to Fix");
        }
    }
    
    /**
     * Handle emergency setup when no admins exist
     */
    private static void handleEmergencySetup(JFrame parentFrame) {
        int option = JOptionPane.showConfirmDialog(parentFrame,
            "⚠️  EMERGENCY SETUP REQUIRED\n\n" +
            "No admin accounts found or system error detected.\n\n" +
            "Choose option:\n" +
            "1. Create default admin (admin/Admin@123)\n" +
            "2. Reset all passwords to 'Reset123!'\n" +
            "3. View system status\n" +
            "4. Cancel",
            "Emergency Setup",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (option == JOptionPane.YES_OPTION) {
            // Create default admin
            boolean success = SqlAdminManager.addAdmin("admin", "System Administrator", "Admin@123", "SUPERADMIN");
            if (success) {
                JOptionPane.showMessageDialog(parentFrame,
                    "✅ Emergency Admin Created!\n\n" +
                    "Admin ID: admin\n" +
                    "Password: Admin@123\n\n" +
                    "Use these credentials to login.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } else if (option == JOptionPane.NO_OPTION) {
            // Reset all passwords
            int confirm = JOptionPane.showConfirmDialog(parentFrame,
                "This will reset ALL admin passwords to 'Reset123!'.\n" +
                "Continue?",
                "Confirm Password Reset",
                JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                SqlAdminManager.emergencyResetAllAdminPasswords();
                JOptionPane.showMessageDialog(parentFrame,
                    "✅ All admin passwords reset to: Reset123!\n\n" +
                    "Try login with your admin ID and password: Reset123!",
                    "Passwords Reset",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } else if (option == JOptionPane.CANCEL_OPTION) {
            // Show system status
            try {
                int adminCount = SqlAdminManager.getAllAdmins().size();
                JOptionPane.showMessageDialog(parentFrame,
                    "📊 SYSTEM STATUS:\n\n" +
                    "Admin Accounts: " + adminCount + "\n" +
                    "Database: " + (DatabaseManager.getConnection() != null ? "Connected" : "Disconnected"),
                    "System Status",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(parentFrame,
                    "❌ Error getting system status: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}