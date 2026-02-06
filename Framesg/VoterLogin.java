package Framesg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import Data.ElectionData;

public class VoterLogin extends JFrame implements ActionListener {
    private static final Color NAVY_BLUE = new Color(25, 25, 112);
    private static final Color LIGHT_BLUE = new Color(173, 216, 230);
    private static final Color DODGER_BLUE = new Color(30, 144, 255);

    private final JTextField voterIdField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
    private final JButton backButton;
    private final JButton registerButton;
    private final JFrame parentFrame;

    public VoterLogin(JFrame parentFrame) {
        super("Voter Login");
        this.parentFrame = parentFrame;
        setSize(400, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(NAVY_BLUE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel titleLabel = new JLabel("Voter Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(LIGHT_BLUE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        JLabel idLabel = new JLabel("Voter ID:");
        idLabel.setForeground(LIGHT_BLUE);
        panel.add(idLabel, gbc);

        gbc.gridx = 1;
        voterIdField = new JTextField(15);
        panel.add(voterIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(LIGHT_BLUE);
        panel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
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
        registerButton = new JButton("Need to Register?");
        registerButton.setBackground(new Color(60, 179, 113)); // Green
        registerButton.setForeground(Color.WHITE);
        registerButton.addActionListener(e -> {
            setVisible(false);
            new VoterRegistration(parentFrame).setVisible(true);
        });
        panel.add(registerButton, gbc);

        gbc.gridy = 5;
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
            String voterId = voterIdField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            if (voterId.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Voter ID and Password are required");
                return;
            }

            // Check if voter exists
            if (!ElectionData.voterIdExists(voterId)) {
                int choice = JOptionPane.showConfirmDialog(this,
                    "Voter ID '" + voterId + "' not found.\n\n" +
                    "Do you want to register as a new voter?",
                    "Voter Not Found",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
                    
                if (choice == JOptionPane.YES_OPTION) {
                    setVisible(false);
                    new VoterRegistration(parentFrame).setVisible(true);
                }
                return;
            }

            // Check if voter is registered (has password)
            if (!ElectionData.isVoterRegistered(voterId)) {
                int choice = JOptionPane.showConfirmDialog(this,
                    "Voter ID '" + voterId + "' is not registered yet.\n\n" +
                    "You need to set a password first.\n" +
                    "Go to registration page?",
                    "Not Registered",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                    
                if (choice == JOptionPane.YES_OPTION) {
                    setVisible(false);
                    new VoterRegistration(parentFrame).setVisible(true);
                }
                return;
            }

            // Validate login
            if (!ElectionData.validateVoter(voterId, password)) {
                JOptionPane.showMessageDialog(this, "Invalid Password!");
                return;
            }

            if (ElectionData.hasVoted(voterId)) {
                JOptionPane.showMessageDialog(this, "You have already voted. Thank you!");
            } else {
                setVisible(false);
                new VoterVoting(voterId, this).setVisible(true);
            }

        } else if (e.getSource() == backButton) {
            setVisible(false);
            parentFrame.setVisible(true);
        }
    }
}