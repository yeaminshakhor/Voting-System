package Framesg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import Data.AdminData;

/**
 * Admin login panel for the election management system.
 */
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
    private final JFrame parentFrame;

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
        loginButton = new JButton("Login");
        loginButton.setBackground(DODGER_BLUE);
        loginButton.setForeground(Color.WHITE);
        loginButton.addActionListener(this);
        panel.add(loginButton, gbc);

        gbc.gridy = 4;
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

            if (AdminData.validateAdminCredentials(adminId, password)) {
                setVisible(false);
                new AdminDashboard(adminId, this).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Admin ID or Password!");
            }
        } else if (e.getSource() == backButton) {
            setVisible(false);
            parentFrame.setVisible(true);
        }
    }
}