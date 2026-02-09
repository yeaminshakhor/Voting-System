package Framesg;

import Data.SqlAdminManager;
import Utils.AdminRole;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class AdminLogin extends JFrame implements ActionListener {
   private static final Color NAVY_BLUE = new Color(25, 25, 112);
   private static final Color LIGHT_BLUE = new Color(173, 216, 230);
   private static final Color DODGER_BLUE = new Color(30, 144, 255);
   private static final int WINDOW_WIDTH = 450;
   private static final int WINDOW_HEIGHT = 300;
   private final JTextField adminIdField;
   private final JPasswordField passwordField;
   private final JButton loginButton;
   private final JButton backButton;
   private final JButton emergencyButton; // Added emergency button
   private final JFrame parentFrame;

   public AdminLogin(JFrame var1) {
      super("Admin Login");
      this.parentFrame = var1;
      this.setSize(450, 300);
      this.setLocationRelativeTo((Component)null);
      this.setDefaultCloseOperation(3);
      JPanel var2 = new JPanel(new GridBagLayout());
      var2.setBackground(NAVY_BLUE);
      GridBagConstraints var3 = new GridBagConstraints();
      var3.insets = new Insets(10, 10, 10, 10);
      var3.anchor = 10;
      
      // Title Label
      JLabel var4 = new JLabel("Admin Login", 0);
      var4.setFont(new Font("Arial", 1, 20));
      var4.setForeground(LIGHT_BLUE);
      var3.gridx = 0;
      var3.gridy = 0;
      var3.gridwidth = 2;
      var2.add(var4, var3);
      
      // Admin ID Label and Field
      var3.gridwidth = 1;
      var3.gridy = 1;
      JLabel var5 = new JLabel("Admin ID:");
      var5.setForeground(LIGHT_BLUE);
      var2.add(var5, var3);
      
      var3.gridx = 1;
      this.adminIdField = new JTextField(20);
      this.adminIdField.setEditable(true);
      this.adminIdField.setFocusable(true);
      this.adminIdField.setPreferredSize(new java.awt.Dimension(200, 25)); // Fixed size
      var2.add(this.adminIdField, var3);
      
      // Password Label and Field
      var3.gridx = 0;
      var3.gridy = 2;
      JLabel var6 = new JLabel("Password:");
      var6.setForeground(LIGHT_BLUE);
      var2.add(var6, var3);
      
      var3.gridx = 1;
      this.passwordField = new JPasswordField(20);
      this.passwordField.setEditable(true);
      this.passwordField.setFocusable(true);
      this.passwordField.setPreferredSize(new java.awt.Dimension(200, 25)); // Fixed size
      var2.add(this.passwordField, var3);
      
      // Login Button
      var3.gridx = 0;
      var3.gridy = 3;
      var3.gridwidth = 2;
      this.loginButton = new JButton("Login");
      this.loginButton.setBackground(DODGER_BLUE);
      this.loginButton.setForeground(Color.WHITE);
      this.loginButton.setPreferredSize(new java.awt.Dimension(150, 30)); // Fixed size
      this.loginButton.addActionListener(this);
      var2.add(this.loginButton, var3);
      
      // Emergency Reset Button (Hidden by default, shown if needed)
      var3.gridy = 4;
      this.emergencyButton = new JButton("Forgot Password?");
      this.emergencyButton.setBackground(Color.ORANGE);
      this.emergencyButton.setForeground(Color.BLACK);
      this.emergencyButton.setPreferredSize(new java.awt.Dimension(150, 25));
      this.emergencyButton.addActionListener(this);
      // Hide by default, show only if no admins exist
      this.emergencyButton.setVisible(false);
      var2.add(this.emergencyButton, var3);
      
      // Back Button
      var3.gridy = 5;
      this.backButton = new JButton("Back to Main");
      this.backButton.setBackground(DODGER_BLUE);
      this.backButton.setForeground(Color.WHITE);
      this.backButton.setPreferredSize(new java.awt.Dimension(150, 30)); // Fixed size
      this.backButton.addActionListener(this);
      var2.add(this.backButton, var3);
      
      this.add(var2);
      
      // Check if we need to show emergency button
      checkAdminSystemStatus();
   }
   
   private void checkAdminSystemStatus() {
      try {
         // Check if SQL database has any active admins
         boolean hasAdmins = !SqlAdminManager.getAllAdmins().isEmpty();
         if (!hasAdmins) {
            emergencyButton.setVisible(true);
            emergencyButton.setText("No Admins - Click to Setup");
         }
      } catch (Exception e) {
         emergencyButton.setVisible(true);
         emergencyButton.setText("System Error - Setup Required");
      }
   }

   public void actionPerformed(ActionEvent var1) {
      if (var1.getSource() == this.loginButton) {
         String var2 = this.adminIdField.getText().trim();
         String var3 = (new String(this.passwordField.getPassword())).trim();
         
         if (var2.isEmpty() || var3.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Admin ID and Password are required", 
                                         "Error", JOptionPane.ERROR_MESSAGE);
            return;
         }

         // Use SqlAdminManager instead of AdminData
         if (SqlAdminManager.validateAdminCredentials(var2, var3)) {
            // Get admin details
            String adminName = SqlAdminManager.getAdminNameById(var2);
            String role = SqlAdminManager.getRoleById(var2);
            
            JOptionPane.showMessageDialog(this, 
                "Welcome, " + adminName + "!\nRole: " + role,
                "Login Successful", JOptionPane.INFORMATION_MESSAGE);
                
            this.setVisible(false);
            
            // Open AdminDashboard - adjust this based on your AdminDashboard constructor
            (new AdminDashboard(var2, this)).setVisible(true);
         } else {
            JOptionPane.showMessageDialog(this, 
                "Invalid Admin ID or Password!\n\nTry:\n- ID: superadmin\n- Password: super123\n\nOr click 'Forgot Password?' to reset.",
                "Login Failed", JOptionPane.ERROR_MESSAGE);
         }
      } 
      else if (var1.getSource() == this.emergencyButton) {
         handleEmergencyButton();
      }
      else if (var1.getSource() == this.backButton) {
         this.setVisible(false);
         this.parentFrame.setVisible(true);
      }
   }
   
   private void handleEmergencyButton() {
      int option = JOptionPane.showConfirmDialog(this,
          "Emergency Admin Setup\n\n" +
          "No admin accounts found or system error.\n" +
          "Do you want to:\n" +
          "1. Create default admin (admin/admin123)\n" +
          "2. Reset all passwords to 'Reset123!'\n" +
          "3. Migrate from text file",
          "Emergency Setup", JOptionPane.YES_NO_CANCEL_OPTION);
      
      if (option == JOptionPane.YES_OPTION) {
         // Create default admin
         boolean success = SqlAdminManager.addAdmin("admin", "System Administrator", "admin123", AdminRole.SUPERADMIN);
         if (success) {
            JOptionPane.showMessageDialog(this,
                "Default admin created!\n\n" +
                "ID: admin\n" +
                "Password: admin123\n\n" +
                "Use these credentials to login.",
                "Success", JOptionPane.INFORMATION_MESSAGE);
            adminIdField.setText("admin");
            passwordField.setText("admin123");
         }
      }
      else if (option == JOptionPane.NO_OPTION) {
         // Reset all passwords
         int confirm = JOptionPane.showConfirmDialog(this,
             "This will reset ALL admin passwords to 'Reset123!'.\n" +
             "Continue?",
             "Confirm Reset", JOptionPane.YES_NO_OPTION);
         
         if (confirm == JOptionPane.YES_OPTION) {
            // Call emergency reset method
            SqlAdminManager.emergencyResetAllAdminPasswords();
            JOptionPane.showMessageDialog(this,
                "All admin passwords reset to: Reset123!\n\n" +
                "Try your admin ID with password: Reset123!",
                "Passwords Reset", JOptionPane.INFORMATION_MESSAGE);
         }
      }
      else if (option == JOptionPane.CANCEL_OPTION) {
         // Migrate from text file
         int migrated = SqlAdminManager.migrateAllAdminsFromTextFile();
         JOptionPane.showMessageDialog(this,
             "Migrated " + migrated + " admins from text file.\n" +
             "All passwords reset to: Reset123!\n" +
             "Use your admin ID with password: Reset123!",
             "Migration Complete", JOptionPane.INFORMATION_MESSAGE);
      }
      
      // Hide emergency button after setup
      emergencyButton.setVisible(false);
   }
   
   // Main method for testing
   public static void main(String[] args) {
      // Test the frame
      javax.swing.SwingUtilities.invokeLater(() -> {
         AdminLogin frame = new AdminLogin(null);
         frame.setVisible(true);
      });
   }
}