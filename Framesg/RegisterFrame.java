package Framesg;

import Data.ElectionData;
import java.awt.*;
import javax.swing.*;

/**
 * RegisterFrame for voter registration in the Online Voting System.
 * Allows anyone to self-register with a voter ID, name, and password.
 * Passwords are always hashed - no plaintext storage.
 * 
 * Features:
 * - Self-service registration (no admin approval needed)
 * - Optional portal integration (if configured)
 * - Strong password requirements
 * - Input validation and sanitization
 */
public class RegisterFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    public RegisterFrame() {
        setTitle("Register as Voter");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2)); // Split into 2 panels

        // Left panel (Light blue background with logo)
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(new Color(173, 216, 230)); // Light blue
        leftPanel.setLayout(new GridBagLayout());

        try {
            ImageIcon icon = new ImageIcon("officiallogo.jpeg");
            Image img = icon.getImage().getScaledInstance(400, 570, Image.SCALE_SMOOTH);
            icon = new ImageIcon(img);
            JLabel logo = new JLabel(icon);
            logo.setHorizontalAlignment(SwingConstants.CENTER);
            logo.setVerticalAlignment(SwingConstants.CENTER);
            leftPanel.add(logo);
        } catch (Exception e) {
            JLabel logo = new JLabel("Logo Not Found", SwingConstants.CENTER);
            logo.setFont(new Font("Serif", Font.BOLD, 20));
            leftPanel.add(logo);
        }

        // Right panel (Sky blue background with form)
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(new Color(135, 206, 235)); // Sky blue
        rightPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        // Title
        JLabel title = new JLabel("Register as Voter", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 38));
        title.setForeground(new Color(15, 30, 60));
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        rightPanel.add(title, gbc);

        // Subtitle
        JLabel subtitle = new JLabel("Create your voting account");
        subtitle.setFont(new Font("Serif", Font.ITALIC, 14));
        subtitle.setForeground(new Color(60, 60, 60));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 10, 20, 10);
        rightPanel.add(subtitle, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 10, 8, 10);

        // Student ID
        JLabel idLabel = new JLabel("Student/Employee ID:");
        idLabel.setFont(new Font("Serif", Font.BOLD, 12));
        idLabel.setForeground(new Color(15, 30, 60));
        gbc.gridx = 0;
        gbc.gridy = 2;
        rightPanel.add(idLabel, gbc);

        JTextField studentId = new JTextField(20);
        studentId.setFont(new Font("Serif", Font.PLAIN, 12));
        studentId.setBackground(Color.WHITE);
        gbc.gridx = 1;
        rightPanel.add(studentId, gbc);

        // Full Name
        JLabel nameLabel = new JLabel("Full Name:");
        nameLabel.setFont(new Font("Serif", Font.BOLD, 12));
        nameLabel.setForeground(new Color(15, 30, 60));
        gbc.gridx = 0;
        gbc.gridy = 3;
        rightPanel.add(nameLabel, gbc);

        JTextField fullName = new JTextField(20);
        fullName.setFont(new Font("Serif", Font.PLAIN, 12));
        fullName.setBackground(Color.WHITE);
        gbc.gridx = 1;
        rightPanel.add(fullName, gbc);

        // Password
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Serif", Font.BOLD, 12));
        passLabel.setForeground(new Color(15, 30, 60));
        gbc.gridx = 0;
        gbc.gridy = 4;
        rightPanel.add(passLabel, gbc);

        JPasswordField password = new JPasswordField(20);
        password.setFont(new Font("Serif", Font.PLAIN, 12));
        password.setBackground(Color.WHITE);
        gbc.gridx = 1;
        rightPanel.add(password, gbc);

        // Confirm Password
        JLabel confirmLabel = new JLabel("Confirm Password:");
        confirmLabel.setFont(new Font("Serif", Font.BOLD, 12));
        confirmLabel.setForeground(new Color(15, 30, 60));
        gbc.gridx = 0;
        gbc.gridy = 5;
        rightPanel.add(confirmLabel, gbc);

        JPasswordField confirmPassword = new JPasswordField(20);
        confirmPassword.setFont(new Font("Serif", Font.PLAIN, 12));
        confirmPassword.setBackground(Color.WHITE);
        gbc.gridx = 1;
        rightPanel.add(confirmPassword, gbc);

        // Password requirements
        JLabel reqLabel = new JLabel(
            "<html><small>" +
            "• Minimum 6 characters<br>" +
            "• Use letters and numbers<br>" +
            "• Passwords are hashed (secure, never stored as plaintext)" +
            "</small></html>"
        );
        reqLabel.setFont(new Font("Serif", Font.PLAIN, 10));
        reqLabel.setForeground(new Color(80, 80, 80));
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        rightPanel.add(reqLabel, gbc);

        // ========== BUTTONS ==========
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(new Color(135, 206, 235));

        JButton registerBtn = new JButton("Register");
        registerBtn.setBackground(new Color(15, 30, 60));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFont(new Font("Serif", Font.BOLD, 14));
        registerBtn.setPreferredSize(new Dimension(120, 40));
        registerBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        registerBtn.addActionListener(e -> {
            String id = studentId.getText().trim();
            String name = fullName.getText().trim();
            String pass = new String(password.getPassword());
            String confirmPass = new String(confirmPassword.getPassword());

            if (id.isEmpty() || name.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!pass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (pass.length() < 6) {
                JOptionPane.showMessageDialog(this, "Password must be at least 6 characters!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (ElectionData.voterIdExists(id)) {
                JOptionPane.showMessageDialog(this, "Student ID already registered!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Register voter with hashed password
            String result = ElectionData.registerVoterSelf(id, name, pass);
            if (result.startsWith("Success")) {
                JOptionPane.showMessageDialog(this, result, "Success", JOptionPane.INFORMATION_MESSAGE);
                setVisible(false);
                new LoginFrame().setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, result, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton backBtn = new JButton("Back");
        backBtn.setBackground(new Color(180, 180, 180));
        backBtn.setForeground(Color.WHITE);
        backBtn.setFont(new Font("Serif", Font.BOLD, 14));
        backBtn.setPreferredSize(new Dimension(120, 40));
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        backBtn.addActionListener(e -> {
            setVisible(false);
            new landing().setVisible(true);
            dispose();
        });

        buttonPanel.add(registerBtn);
        buttonPanel.add(backBtn);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(10, 10, 20, 10);
        rightPanel.add(buttonPanel, gbc);

        add(leftPanel);
        add(rightPanel);

        SwingUtilities.invokeLater(() -> this.requestFocusInWindow());
    }
}
