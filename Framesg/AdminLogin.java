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
   private static final long serialVersionUID = 1L;
   private static final Color NAVY_BLUE = new Color(25, 25, 112);
   private static final Color LIGHT_BLUE = new Color(173, 216, 230);
   private static final Color DODGER_BLUE = new Color(30, 144, 255);
   private final JTextField adminIdField;
   private final JPasswordField passwordField;
   private final JButton loginButton;
   private final JButton backButton;
   private final JButton emergencyButton;
   private final JButton forgotPasswordButton;
   private final JFrame parentFrame;

   public AdminLogin(JFrame var1) {
      super("Admin Login");
      this.parentFrame = var1;
      this.setSize(450, 300);
      this.setLocationRelativeTo((Component)null);
      this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
      this.addWindowListener(new java.awt.event.WindowAdapter() {
         @Override
         public void windowClosing(java.awt.event.WindowEvent e) {
            setVisible(false);
            if (parentFrame != null) {
               parentFrame.setVisible(true);
            }
         }
      });
      
      JPanel mainPanel = new JPanel(new GridBagLayout());
      mainPanel.setBackground(NAVY_BLUE);
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.insets = new Insets(10, 10, 10, 10);
      gbc.anchor = 10;
      
      // Title Label
      JLabel titleLabel = new JLabel("Admin Login", 0);
      titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
      titleLabel.setForeground(LIGHT_BLUE);
      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.gridwidth = 2;
      mainPanel.add(titleLabel, gbc);
      
      // Admin ID Label and Field
      gbc.gridwidth = 1;
      gbc.gridy = 1;
      JLabel adminIdLabel = new JLabel("Admin ID:");
      adminIdLabel.setForeground(LIGHT_BLUE);
      mainPanel.add(adminIdLabel, gbc);
      
      gbc.gridx = 1;
      this.adminIdField = new JTextField(20);
      this.adminIdField.setEditable(true);
      this.adminIdField.setFocusable(true);
      this.adminIdField.setPreferredSize(new java.awt.Dimension(200, 25));
      mainPanel.add(this.adminIdField, gbc);
      
      // Password Label and Field
      gbc.gridx = 0;
      gbc.gridy = 2;
      JLabel passwordLabel = new JLabel("Password:");
      passwordLabel.setForeground(LIGHT_BLUE);
      mainPanel.add(passwordLabel, gbc);
      
      gbc.gridx = 1;
      this.passwordField = new JPasswordField(20);
      this.passwordField.setEditable(true);
      this.passwordField.setFocusable(true);
      this.passwordField.setPreferredSize(new java.awt.Dimension(200, 25));
      mainPanel.add(this.passwordField, gbc);
      
      // Login Button
      gbc.gridx = 0;
      gbc.gridy = 3;
      gbc.gridwidth = 2;
      this.loginButton = new JButton("Login");
      this.loginButton.setBackground(DODGER_BLUE);
      this.loginButton.setForeground(Color.WHITE);
      this.loginButton.setPreferredSize(new java.awt.Dimension(150, 30));
      this.loginButton.addActionListener(this);
      mainPanel.add(this.loginButton, gbc);
      
      // Forgot Password Button
      gbc.gridy = 4;
      this.forgotPasswordButton = new JButton("Forgot Password?");
      this.forgotPasswordButton.setBackground(Color.ORANGE);
      this.forgotPasswordButton.setForeground(Color.BLACK);
      this.forgotPasswordButton.setPreferredSize(new java.awt.Dimension(150, 25));
      this.forgotPasswordButton.addActionListener(this);
      mainPanel.add(this.forgotPasswordButton, gbc);
      
      // Emergency Button (Hidden by default)
      gbc.gridy = 5;
      this.emergencyButton = new JButton("Emergency Setup");
      this.emergencyButton.setBackground(Color.RED);
      this.emergencyButton.setForeground(Color.WHITE);
      this.emergencyButton.setPreferredSize(new java.awt.Dimension(150, 25));
      this.emergencyButton.addActionListener(this);
      this.emergencyButton.setVisible(false); // Hidden initially
      mainPanel.add(this.emergencyButton, gbc);
      
      // Back Button
      gbc.gridy = 6;
      this.backButton = new JButton("Back to Main");
      this.backButton.setBackground(DODGER_BLUE);
      this.backButton.setForeground(Color.WHITE);
      this.backButton.setPreferredSize(new java.awt.Dimension(150, 30));
      this.backButton.addActionListener(this);
      mainPanel.add(this.backButton, gbc);
      
      this.add(mainPanel);
      
      // Check if we need to show emergency button
      checkAdminSystemStatus();
   }
   
   private void checkAdminSystemStatus() {
      try {
         // Check if SQL database has any active admins
         boolean hasAdmins = !SqlAdminManager.getAllAdmins().isEmpty();
         if (!hasAdmins) {
            emergencyButton.setVisible(true);
            emergencyButton.setText("⚠️ No Admins - Emergency Setup");
            
            // Show warning to user
            JOptionPane.showMessageDialog(this,
                "<html><b>⚠️ No Admin Accounts Found</b><br><br>" +
                "System has no admin accounts in the database.<br>" +
                "Click 'Emergency Setup' to create a default admin.</html>",
                "Setup Required",
                JOptionPane.WARNING_MESSAGE);
         }
      } catch (Exception e) {
         emergencyButton.setVisible(true);
         emergencyButton.setText("⚠️ System Error - Setup");
      }
   }

   public void actionPerformed(ActionEvent evt) {
      if (evt.getSource() == this.loginButton) {
         handleLogin();
      } 
      else if (evt.getSource() == this.forgotPasswordButton) {
         handleForgotPassword();
      }
      else if (evt.getSource() == this.emergencyButton) {
         handleEmergencyButton();
      }
      else if (evt.getSource() == this.backButton) {
         this.setVisible(false);
         this.parentFrame.setVisible(true);
      }
   }
   
   private void handleLogin() {
      String adminId = this.adminIdField.getText().trim();
      String password = new String(this.passwordField.getPassword());
      
      // Input validation
      if (adminId.isEmpty()) {
         JOptionPane.showMessageDialog(this, 
             "Please enter Admin ID", 
             "Input Required", 
             JOptionPane.WARNING_MESSAGE);
         adminIdField.requestFocus();
         return;
      }
      
      if (password.isEmpty()) {
         JOptionPane.showMessageDialog(this, 
             "Please enter password", 
             "Input Required", 
             JOptionPane.WARNING_MESSAGE);
         passwordField.requestFocus();
         return;
      }

      try {
         // Use SqlAdminManager for authentication
         System.out.println("🔐 [AdminLogin] Attempting login for: " + adminId);
         if (SqlAdminManager.validateAdminCredentials(adminId, password)) {
            // Get admin details
            String adminName = SqlAdminManager.getAdminNameById(adminId);
            String role = SqlAdminManager.getRoleById(adminId);
            
            if (adminName == null || adminName.isEmpty()) {
               adminName = adminId;
            }
            if (role == null || role.isEmpty()) {
               role = "UNKNOWN";
            }
            
            System.out.println("✅ [AdminLogin] Login successful for: " + adminId);
            JOptionPane.showMessageDialog(this, 
                "<html><b>Welcome, " + adminName + "!</b><br>" +
                "Role: " + role + "</html>",
                "Login Successful", 
                JOptionPane.INFORMATION_MESSAGE);
                
            this.setVisible(false);
            
            // Open AdminDashboard - adjust this based on your AdminDashboard constructor
            (new AdminDashboard(adminId, this)).setVisible(true);
         } else {
            System.out.println("❌ [AdminLogin] Login failed for: " + adminId);
            JOptionPane.showMessageDialog(this,
                "<html><b>Login Failed</b><br><br>" +
                "Possible reasons:<br>" +
                "1. Wrong Admin ID or Password<br>" +
                "2. Admin account is disabled<br>" +
                "3. Database connection issue<br><br>" +
                "<i>Click 'Forgot Password?' for assistance</i></html>",
             "Authentication Failed",
             JOptionPane.ERROR_MESSAGE);
         }
      } catch (Exception ex) {
         System.err.println("❌ [AdminLogin] Exception during login: " + ex.getMessage());
         ex.printStackTrace();
         JOptionPane.showMessageDialog(this,
             "<html><b>Login Error</b><br><br>" +
             "An unexpected error occurred:<br>" +
             ex.getClass().getSimpleName() + ": " + ex.getMessage() + "<br><br>" +
             "Please check the console for more details.</html>",
             "System Error",
             JOptionPane.ERROR_MESSAGE);
      }
   }
   
   private void handleForgotPassword() {
      String[] options = {"Reset My Password", "Get Help Information", "Cancel"};
      
      int choice = JOptionPane.showOptionDialog(this,
          "<html><b>Password Assistance</b><br><br>" +
          "<b>Option 1:</b> Reset your password<br>" +
          "<b>Option 2:</b> Get help information<br>",
          "Forgot Password",
          JOptionPane.DEFAULT_OPTION,
          JOptionPane.QUESTION_MESSAGE,
          null,
          options,
          options[0]);
      
      if (choice == 0) {
         showPasswordResetDialog();
      } else if (choice == 1) {
         showPasswordResetInfo();
      }
   }
   
   private void showPasswordResetDialog() {
      try {
         String adminId = JOptionPane.showInputDialog(this,
             "Enter your Admin ID:",
             "Password Reset",
             JOptionPane.QUESTION_MESSAGE);
         
         if (adminId == null || adminId.trim().isEmpty()) {
            return; // User cancelled
         }
         
         adminId = adminId.trim();
         
         // Verify admin exists
         if (!SqlAdminManager.adminExists(adminId)) {
            JOptionPane.showMessageDialog(this,
                "Admin ID not found: " + adminId,
                "Not Found",
                JOptionPane.ERROR_MESSAGE);
            return;
         }
         
         String newPassword = JOptionPane.showInputDialog(this,
             "Enter new password (min 6 characters):",
             "New Password",
             JOptionPane.QUESTION_MESSAGE);
         
         if (newPassword == null || newPassword.length() < 8) {
            JOptionPane.showMessageDialog(this,
                "<html><b>Invalid Password</b><br><br>" +
                "Password must meet the following requirements:<br>" +
                "• At least 8 characters<br>" +
                "• At least one uppercase letter (A-Z)<br>" +
                "• At least one lowercase letter (a-z)<br>" +
                "• At least one digit (0-9)<br><br>" +
                "Example: Password123</html>",
                "Invalid Password",
                JOptionPane.WARNING_MESSAGE);
            return;
         }
         
         // Check password strength
         if (!isPasswordStrong(newPassword)) {
            JOptionPane.showMessageDialog(this,
                "<html><b>Weak Password</b><br><br>" +
                "Password must contain:<br>" +
                "• At least one uppercase letter (A-Z)<br>" +
                "• At least one lowercase letter (a-z)<br>" +
                "• At least one digit (0-9)<br><br>" +
                "Example: Password123</html>",
                "Weak Password",
                JOptionPane.WARNING_MESSAGE);
            return;
         }
         
         String confirmPassword = JOptionPane.showInputDialog(this,
             "Confirm new password:",
             "Confirm Password",
             JOptionPane.QUESTION_MESSAGE);
         
         if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this,
                "Passwords do not match",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
         }
         
         // Perform password reset
         System.out.println("🔐 [AdminLogin] Processing password reset for: " + adminId);
         boolean success = SqlAdminManager.forgotPassword(adminId, newPassword);
         if (success) {
            System.out.println("✅ [AdminLogin] Password reset successful for: " + adminId);
            JOptionPane.showMessageDialog(this,
                "<html><b>Password Reset Successful!</b><br><br>" +
                "Admin ID: " + adminId + "<br>" +
                "You can now login with your new password.</html>",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
         } else {
            System.out.println("❌ [AdminLogin] Password reset failed for: " + adminId);
            JOptionPane.showMessageDialog(this,
                "Password reset failed. Please try again or contact system administrator.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
         }
      } catch (Exception ex) {
         System.err.println("❌ [AdminLogin] Exception during password reset: " + ex.getMessage());
         ex.printStackTrace();
         JOptionPane.showMessageDialog(this,
             "<html><b>Password Reset Error</b><br><br>" +
             "An error occurred: " + ex.getMessage() + "<br><br>" +
             "Please check the console.</html>",
             "Error",
             JOptionPane.ERROR_MESSAGE);
      }
   }
   
   private boolean isPasswordStrong(String password) {
      if (password == null || password.length() < 8) {
         return false;
      }
      
      boolean hasUpper = false;
      boolean hasLower = false;
      boolean hasDigit = false;
      
      for (char c : password.toCharArray()) {
         if (Character.isUpperCase(c)) hasUpper = true;
         if (Character.isLowerCase(c)) hasLower = true;
         if (Character.isDigit(c)) hasDigit = true;
      }
      
      return hasUpper && hasLower && hasDigit;
   }
   
   private void showPasswordResetInfo() {
      JOptionPane.showMessageDialog(this,
          "<html><b>Password Reset Information</b><br><br>" +
          "<b>If you forgot your password:</b><br>" +
          "1. Select 'Reset My Password' option<br>" +
          "2. Enter your Admin ID<br>" +
          "3. Set a new password<br><br>" +
          
          "<b>If you don't know your Admin ID:</b><br>" +
          "1. Ask a SuperAdmin for assistance<br>" +
          "2. Use default admin if available<br>" +
          "3. Contact system administrator<br><br>" +
          
          "<b>Default Admin (if configured):</b><br>" +
          "• ID: <b>superadmin</b><br>" +
          "• Password: <b>super123</b><br><br>" +
          
          "<i>Note: Default admin may not be available in all systems.</i></html>",
          "Password Reset Help",
          JOptionPane.INFORMATION_MESSAGE);
   }
   
   private void handleEmergencyButton() {
      String[] options = {"Create Default SuperAdmin", "Reset All Passwords", "Migrate from Text File", "Cancel"};
      
      int choice = JOptionPane.showOptionDialog(this,
          "<html><b>⚠️ Emergency Admin Setup</b><br><br>" +
          "No admin accounts found or system error detected.<br>" +
          "Choose an option:</html>",
          "Emergency Setup",
          JOptionPane.DEFAULT_OPTION,
          JOptionPane.WARNING_MESSAGE,
          null,
          options,
          options[0]);
      
      if (choice == 0) {
         // Create default SuperAdmin
         createDefaultSuperAdmin();
      }
      else if (choice == 1) {
         // Reset all passwords
         resetAllPasswords();
      }
      else if (choice == 2) {
         // Migrate from text file
         migrateFromTextFile();
      }
   }
   
   private void createDefaultSuperAdmin() {
      int confirm = JOptionPane.showConfirmDialog(this,
          "<html><b>Create Default SuperAdmin?</b><br><br>" +
          "This will create a default admin with:<br>" +
          "• ID: <b>superadmin</b><br>" +
          "• Password: <b>super123</b><br>" +
          "• Role: SuperAdmin<br><br>" +
          "<i>You must change this password after first login!</i></html>",
          "Confirm Creation",
          JOptionPane.YES_NO_OPTION);
      
      if (confirm == JOptionPane.YES_OPTION) {
         boolean success = SqlAdminManager.addAdmin("superadmin", "System Administrator", "super123", AdminRole.SUPERADMIN);
         
         if (success) {
            JOptionPane.showMessageDialog(this,
                "<html><b>Default SuperAdmin Created!</b><br><br>" +
                "Login with:<br>" +
                "• ID: <b>superadmin</b><br>" +
                "• Password: <b>super123</b><br><br>" +
                "<i>⚠️ Change password immediately after login!</i></html>",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
            
            // Pre-fill the fields for convenience
            adminIdField.setText("superadmin");
            passwordField.setText("super123");
            
            // Hide emergency button
            emergencyButton.setVisible(false);
         } else {
            JOptionPane.showMessageDialog(this,
                "Failed to create default SuperAdmin. Database may not be accessible.",
                "Creation Failed",
                JOptionPane.ERROR_MESSAGE);
         }
      }
   }
   
   private void resetAllPasswords() {
      int confirm = JOptionPane.showConfirmDialog(this,
          "<html><b>⚠️ Reset ALL Admin Passwords?</b><br><br>" +
          "This will reset <b>ALL</b> admin passwords to: <b>Reset123!</b><br>" +
          "All admins will need to reset their password on next login.<br><br>" +
          "<i>This action cannot be undone!</i></html>",
          "Confirm Reset",
          JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE);
      
      if (confirm == JOptionPane.YES_OPTION) {
         SqlAdminManager.emergencyResetAllAdminPasswords();
         
         JOptionPane.showMessageDialog(this,
             "<html><b>Passwords Reset Complete!</b><br><br>" +
             "All admin passwords have been reset to: <b>Reset123!</b><br><br>" +
             "Use your Admin ID with password: <b>Reset123!</b><br>" +
             "You will be prompted to change it on first login.</html>",
             "Passwords Reset",
             JOptionPane.INFORMATION_MESSAGE);
      }
   }
   
   private void migrateFromTextFile() {
      int confirm = JOptionPane.showConfirmDialog(this,
          "<html><b>Migrate Admins from Text File?</b><br><br>" +
          "This will import all admins from the old text file.<br>" +
          "Each migrated admin will get password: <b>Reset123!</b><br>" +
          "Admins already in SQL will be skipped.<br><br>" +
          "Proceed?</html>",
          "Confirm Migration",
          JOptionPane.YES_NO_OPTION);
      
      if (confirm == JOptionPane.YES_OPTION) {
         int migrated = SqlAdminManager.migrateAllAdminsFromTextFile();
         
         JOptionPane.showMessageDialog(this,
             "<html><b>Migration Complete!</b><br><br>" +
             "Migrated <b>" + migrated + "</b> admin(s) from text file.<br>" +
             "All passwords set to: <b>Reset123!</b><br><br>" +
             "Use your Admin ID with password: <b>Reset123!</b><br>" +
             "You will be prompted to change it on first login.</html>",
             "Migration Complete",
             JOptionPane.INFORMATION_MESSAGE);
         
         // Hide emergency button if admins were migrated
         if (migrated > 0) {
            emergencyButton.setVisible(false);
         }
      }
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