package Framesg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import Data.SqlAdminManager;

/**
 * Admin login panel for the election management system.
 * Now uses SQL-based authentication.
 */
public class AdminLogin extends JFrame implements ActionListener {
    private static final Color NAVY_BLUE = new Color(25, 25, 112);
    private static final Color LIGHT_BLUE = new Color(173, 216, 230);
    private static final Color DODGER_BLUE = new Color(30, 144, 255);
    private static final int WINDOW_WIDTH = 450;
    private static final int WINDOW_HEIGHT = 350;

    private final JTextField adminIdField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
    private final JButton backButton;
    private final JButton setupButton; // For first-time setup
    private final JFrame parentFrame;
    private final JLabel setupLabel;

    public AdminLogin(JFrame parentFrame) {
        super("Admin Login");
        this.parentFrame = parentFrame;
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(NAVY_BLUE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel titleLabel = new JLabel("Admin Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(LIGHT_BLUE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        JLabel idLabel = new JLabel("Admin ID:");
        idLabel.setForeground(LIGHT_BLUE);
        panel.add(idLabel, gbc);

        gbc.gridx = 1;
        adminIdField = new JTextField(20);
        adminIdField.setEditable(true);
        adminIdField.setFocusable(true);
        panel.add(adminIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(LIGHT_BLUE);
        panel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setEditable(true);
        passwordField.setFocusable(true);
        panel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        
        // Show setup message if no admins exist
        setupLabel = new JLabel("");
        setupLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        setupLabel.setForeground(new Color(255, 200, 0)); // Yellow warning color
        // Show warning if there are no admins at all OR no SuperAdmin yet
        if (SqlAdminManager.isFirstSetup()) {
            setupLabel.setText("⚠️ First setup: Create initial SuperAdmin account");
        } else if (!SqlAdminManager.superAdminExists()) {
            setupLabel.setText("⚠️ No SuperAdmin found: Please create a SuperAdmin account");
        }
        panel.add(setupLabel, gbc);
        
        gbc.gridy = 4;
        loginButton = new JButton("Login");
        loginButton.setBackground(DODGER_BLUE);
        loginButton.setForeground(Color.WHITE);
        loginButton.addActionListener(this);
        panel.add(loginButton, gbc);

        // Setup button (only visible on first setup)
        setupButton = new JButton("Setup Initial SuperAdmin");
        setupButton.setBackground(new Color(34, 139, 34)); // Dark green
        setupButton.setForeground(Color.WHITE);
        // Visible when no admins at all OR when there is no SuperAdmin yet
        setupButton.setVisible(SqlAdminManager.isFirstSetup() || !SqlAdminManager.superAdminExists());
        setupButton.addActionListener(this);
        gbc.gridy = 5;
        panel.add(setupButton, gbc);

        gbc.gridy = 6;
        backButton = new JButton("Back to Main");
        backButton.setBackground(DODGER_BLUE);
        backButton.setForeground(Color.WHITE);
        backButton.addActionListener(this);
        panel.add(backButton, gbc);

        add(panel);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == loginButton) {
            String adminId = adminIdField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            if (adminId.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Admin ID and Password are required");
                return;
            }

            if (SqlAdminManager.validateAdminCredentials(adminId, password)) {
                setVisible(false);
                new AdminDashboard(adminId, this).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Admin ID or Password!");
            }
        } else if (e.getSource() == setupButton) {
            // Show setup dialog for creating initial admin
            showAdminSetupDialog();
        } else if (e.getSource() == backButton) {
            setVisible(false);
            parentFrame.setVisible(true);
        }
    }
    
    /**
     * Show dialog to create initial admin account
     */
    private void showAdminSetupDialog() {
        JDialog setupDialog = new JDialog(this, "Setup Initial Admin", true);
        setupDialog.setSize(450, 280);
        setupDialog.setLocationRelativeTo(this);
        
        JPanel setupPanel = new JPanel(new GridBagLayout());
        setupPanel.setBackground(NAVY_BLUE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        JLabel titleLabel = new JLabel("Create Initial SuperAdmin Account");
        titleLabel.setForeground(LIGHT_BLUE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        setupPanel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        JLabel idLabel = new JLabel("Admin ID:");
        idLabel.setForeground(LIGHT_BLUE);
        setupPanel.add(idLabel, gbc);
        
        gbc.gridx = 1;
        JTextField idField = new JTextField(20);
        setupPanel.add(idField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel nameLabel = new JLabel("Full Name:");
        nameLabel.setForeground(LIGHT_BLUE);
        setupPanel.add(nameLabel, gbc);
        
        gbc.gridx = 1;
        JTextField nameField = new JTextField(20);
        setupPanel.add(nameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setForeground(LIGHT_BLUE);
        setupPanel.add(passLabel, gbc);
        
        gbc.gridx = 1;
        JPasswordField passField = new JPasswordField(20);
        setupPanel.add(passField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        JLabel roleLabel = new JLabel("Role:");
        roleLabel.setForeground(LIGHT_BLUE);
        setupPanel.add(roleLabel, gbc);
        
        gbc.gridx = 1;
        // For initial setup we ALWAYS create a SuperAdmin
        JTextField roleField = new JTextField("SuperAdmin");
        roleField.setEditable(false);
        setupPanel.add(roleField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        JButton createBtn = new JButton("Create Admin");
        createBtn.setBackground(new Color(34, 139, 34));
        createBtn.setForeground(Color.WHITE);
        createBtn.addActionListener(e -> {
            String adminId = idField.getText().trim();
            String name = nameField.getText().trim();
            String password = new String(passField.getPassword()).trim();
            String role = Utils.AdminRole.SUPERADMIN;
            
            if (adminId.isEmpty() || name.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(setupDialog, "All fields are required");
                return;
            }
            
            if (SqlAdminManager.addAdmin(adminId, name, password, role)) {
                JOptionPane.showMessageDialog(setupDialog, "SuperAdmin created successfully!");
                setupDialog.dispose();
                setupButton.setVisible(false); // Hide setup button
                adminIdField.setText(adminId);
                passwordField.setText(password);
            } else {
                JOptionPane.showMessageDialog(setupDialog, "Failed to create admin. Check the app logs.");
            }
        });
        setupPanel.add(createBtn, gbc);
        
        gbc.gridy = 6;
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setBackground(DODGER_BLUE);
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.addActionListener(e -> setupDialog.dispose());
        setupPanel.add(cancelBtn, gbc);
        
        setupDialog.add(setupPanel);
        setupDialog.setVisible(true);
    }
}