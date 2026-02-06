package Framesg;

import Data.ElectionData;
import Entities.Voter;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class VoterRegistration extends JFrame implements ActionListener {
    private static final Color NAVY_BLUE = new Color(25, 25, 112);
    private static final Color LIGHT_BLUE = new Color(173, 216, 230);
    private static final Color DODGER_BLUE = new Color(30, 144, 255);
    private static final int WINDOW_WIDTH = 450;
    private static final int WINDOW_HEIGHT = 350;

    private final JTextField voterIdField;
    private final JPasswordField passwordField;
    private final JPasswordField confirmPasswordField;
    private final JButton registerButton;
    private final JButton backButton;
    private final JFrame parentFrame;

    public VoterRegistration(JFrame parentFrame) {
        super("Voter Registration / Password Setup");
        this.parentFrame = parentFrame;
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(NAVY_BLUE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;

        // Title
        JLabel titleLabel = new JLabel("Voter Registration", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(LIGHT_BLUE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        // Subtitle
        JLabel subtitleLabel = new JLabel("Set your password to claim your voter account", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitleLabel.setForeground(LIGHT_BLUE);
        gbc.gridy = 1;
        panel.add(subtitleLabel, gbc);

        // Voter ID Label
        gbc.gridwidth = 1;
        gbc.gridy = 2;
        JLabel idLabel = new JLabel("Voter ID:");
        idLabel.setForeground(LIGHT_BLUE);
        panel.add(idLabel, gbc);

        // Voter ID Field
        gbc.gridx = 1;
        voterIdField = new JTextField(20);
        voterIdField.setEditable(true);
        voterIdField.setFocusable(true);
        panel.add(voterIdField, gbc);

        // Password Label
        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(LIGHT_BLUE);
        panel.add(passwordLabel, gbc);

        // Password Field
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setEditable(true);
        passwordField.setFocusable(true);
        panel.add(passwordField, gbc);

        // Confirm Password Label
        gbc.gridx = 0;
        gbc.gridy = 4;
        JLabel confirmLabel = new JLabel("Confirm Password:");
        confirmLabel.setForeground(LIGHT_BLUE);
        panel.add(confirmLabel, gbc);

        // Confirm Password Field
        gbc.gridx = 1;
        confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setEditable(true);
        confirmPasswordField.setFocusable(true);
        panel.add(confirmPasswordField, gbc);

        // Info Label
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        JLabel infoLabel = new JLabel("Password must be at least 6 characters", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        infoLabel.setForeground(LIGHT_BLUE);
        panel.add(infoLabel, gbc);

        // Register Button
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        registerButton = new JButton("Set Password & Register");
        registerButton.setBackground(DODGER_BLUE);
        registerButton.setForeground(Color.WHITE);
        registerButton.addActionListener(this);
        panel.add(registerButton, gbc);

        // Back Button
        gbc.gridy = 7;
        backButton = new JButton("Back to Main");
        backButton.setBackground(DODGER_BLUE);
        backButton.setForeground(Color.WHITE);
        backButton.addActionListener(this);
        panel.add(backButton, gbc);

        add(panel);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == registerButton) {
            String voterId = voterIdField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

            if (voterId.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Voter ID and Password are required");
                return;
            }

            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match");
                return;
            }

            if (password.length() < 6) {
                JOptionPane.showMessageDialog(this, "Password must be at least 6 characters");
                return;
            }

            // Check if voter is already registered
            if (ElectionData.isVoterRegistered(voterId)) {
                int choice = JOptionPane.showConfirmDialog(this,
                    "Voter ID '" + voterId + "' is already registered!\n\n" +
                    "• If this is your account, please login instead\n" +
                    "• If you forgot your password, contact administrator\n\n" +
                    "Go to login page?",
                    "Account Already Exists",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                    
                if (choice == JOptionPane.YES_OPTION) {
                    setVisible(false);
                    new VoterLogin(parentFrame).setVisible(true);
                }
                return;
            }

            // Check if voter ID exists in database (for unregistered voters)
            boolean voterExists = ElectionData.voterIdExists(voterId);
            
            if (!voterExists) {
                int confirm = JOptionPane.showConfirmDialog(this,
                    "Voter ID '" + voterId + "' is not in the official voter list.\n" +
                    "Do you want to register as a new voter?",
                    "New Voter Registration",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
                    
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            // Proceed with registration
            Voter voter = new Voter(voterId, "", password);
            if (ElectionData.registerVoter(voter)) {
                String message;
                if (voterExists) {
                    message = "✅ Password Set Successfully!\n\n" +
                             "Your voter account has been activated.\n" +
                             "You can now login to vote.";
                } else {
                    message = "✅ New Voter Registered!\n\n" +
                             "Your account has been created.\n" +
                             "You can now login to vote.";
                }
                
                JOptionPane.showMessageDialog(this, 
                    message,
                    "Registration Complete", 
                    JOptionPane.INFORMATION_MESSAGE);
                setVisible(false);
                parentFrame.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Registration failed. Please try again or contact administrator.",
                    "Registration Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
            
        } else if (e.getSource() == backButton) {
            setVisible(false);
            parentFrame.setVisible(true);
        }
    }
}