import Framesg.AdminLogin;
import Framesg.VoterLogin;
import Framesg.VoterRegistration;
import Data.DatabaseManager;
import Data.DataMigrationUtility;
import Data.SqlAdminManager;
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
        // Initialize database first
        System.out.println("🔄 Initializing Election Management System (SQL Version)...");
        if (DatabaseManager.getConnection() == null) {
            System.err.println("❌ CRITICAL ERROR: Cannot connect to database!");
            System.err.println("   SQLite JDBC driver may not be installed.");
            System.err.println("   System will attempt to use SQL database anyway.");
        }
        
        // Check if migration is needed
        if (DataMigrationUtility.isMigrationNeeded()) {
            int response = JOptionPane.showConfirmDialog(null,
                "Plain text database files detected.\n\n" +
                "Would you like to migrate your data to the SQL system?\n\n" +
                "This is a one-time process and highly recommended.",
                "Data Migration Available",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);
            
            if (response == JOptionPane.YES_OPTION) {
                System.out.println("\n🔄 Starting data migration...");
                DataMigrationUtility.migrateAllData();
                System.out.println("✅ Migration completed!\n");
                JOptionPane.showMessageDialog(null,
                    "✅ Data migration completed successfully!\n\n" +
                    "Your data has been migrated to the SQL system.\n" +
                    "You can safely delete the old plain text files if desired.",
                    "Migration Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        }

        // Ensure there is at least one default SuperAdmin in the SQL database
        // ID: superadmin, Password: super123
        SqlAdminManager.ensureDefaultSuperAdmin();
        
        // Create and show main window
        JFrame frame = new JFrame("Election Management System");
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

        JButton adminLoginButton = new JButton("Admin Login");
        adminLoginButton.setFont(new Font("Arial", Font.BOLD, 14));
        adminLoginButton.setBackground(DODGER_BLUE);
        adminLoginButton.setForeground(Color.WHITE);
        adminLoginButton.setFocusPainted(false);
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        panel.add(adminLoginButton, gbc);

        JButton voterLoginButton = new JButton("Voter Login");
        voterLoginButton.setFont(new Font("Arial", Font.BOLD, 14));
        voterLoginButton.setBackground(DODGER_BLUE);
        voterLoginButton.setForeground(Color.WHITE);
        voterLoginButton.setFocusPainted(false);
        gbc.gridx = 1;
        panel.add(voterLoginButton, gbc);

        JButton voterRegisterButton = new JButton("Voter Registration");
        voterRegisterButton.setFont(new Font("Arial", Font.BOLD, 14));
        voterRegisterButton.setBackground(DODGER_BLUE);
        voterRegisterButton.setForeground(Color.WHITE);
        voterRegisterButton.setFocusPainted(false);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(voterRegisterButton, gbc);

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
    }
}