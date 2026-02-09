package Framesg;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

/**
 * LoginFrame for voter login in the Online Voting System.
 * Validates credentials against data_voters.txt and navigates to VotingFrame.
 * Expects format: id:name:age:password
 */
public class LoginFrame extends JFrame {
    private static final String VOTERS_FILE = "data_voters.txt";

    public LoginFrame() {
        // Set up the login window
        setTitle("Login Page");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2)); // Split into left and right panels

        // Left panel with logo
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(new Color(173, 216, 230)); // Light blue
        leftPanel.setLayout(new GridBagLayout());

        try {
            ImageIcon icon = new ImageIcon("aucsulogo.jpeg");
            Image img = icon.getImage().getScaledInstance(400, 470, Image.SCALE_SMOOTH);
            icon = new ImageIcon(img);
            JLabel logo = new JLabel(icon);
            logo.setHorizontalAlignment(SwingConstants.CENTER);
            logo.setVerticalAlignment(SwingConstants.CENTER);
            leftPanel.add(logo);
        } catch (Exception e) {
            JLabel logo = new JLabel("Logo Not Found", SwingConstants.CENTER);
            leftPanel.add(logo);
        }

        // Right panel with login form
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel loginTitle = new JLabel("Login");
        loginTitle.setFont(new Font("Serif", Font.BOLD, 44));
        loginTitle.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        rightPanel.add(loginTitle, gbc);

        JTextField studentId = new JTextField();
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        studentId.setPreferredSize(new Dimension(200, 50));
        rightPanel.add(studentId, gbc);

        String idPlaceholder = "Student ID";
        studentId.setText(idPlaceholder);
        studentId.setForeground(Color.GRAY);

        studentId.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (studentId.getText().equals(idPlaceholder)) {
                    studentId.setText("");
                    studentId.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (studentId.getText().isEmpty()) {
                    studentId.setText(idPlaceholder);
                    studentId.setForeground(Color.GRAY);
                }
            }
        });

        JPasswordField password = new JPasswordField("Password");
        gbc.gridy = 2;
        password.setPreferredSize(new Dimension(250, 50));
        password.setForeground(Color.GRAY);
        char defaultEcho = password.getEchoChar();
        password.setEchoChar((char) 0);
        rightPanel.add(password, gbc);

        String passPlaceholder = "Password";
        password.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (String.valueOf(password.getPassword()).equals(passPlaceholder)) {
                    password.setText("");
                    password.setForeground(Color.BLACK);
                    password.setEchoChar(defaultEcho);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (String.valueOf(password.getPassword()).isEmpty()) {
                    password.setForeground(Color.GRAY);
                    password.setText(passPlaceholder);
                    password.setEchoChar((char) 0);
                }
            }
        });

        // Buttons
        JButton backBtn = new JButton("Back");
        backBtn.addActionListener(e -> {
            setVisible(false);
            new landing().setVisible(true);
            dispose();
        });
        backBtn.setBackground(new Color(15, 30, 60));
        backBtn.setForeground(Color.WHITE);
        gbc.gridy = 3;
        backBtn.setPreferredSize(new Dimension(200, 50));
        rightPanel.add(backBtn, gbc);

        JButton adminBtn = new JButton("Admin Login");
        adminBtn.setBackground(new Color(15, 30, 60));
        adminBtn.setForeground(Color.WHITE);
        gbc.gridy = 4;
        adminBtn.setPreferredSize(new Dimension(200, 50));
        adminBtn.addActionListener(e -> {
            setVisible(false);
            new AdminDashboard("admin", LoginFrame.this).setVisible(true);
            dispose();
        });
        rightPanel.add(adminBtn, gbc);

        JButton loginBtn = new JButton("Login");
        loginBtn.addActionListener(e -> {
            String id = studentId.getText().trim().replace("ID:", "").trim();
            String passwordInput = new String(password.getPassword()).trim();

            // Check for empty or placeholder inputs
            if (id.isEmpty() || id.equals("Student ID") || passwordInput.equals("Password")) {
                JOptionPane.showMessageDialog(this, "Please enter Student ID and Password!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Validate credentials
            String[] voterDetails = getVoterDetailsById(id);
            if (voterDetails == null) {
                JOptionPane.showMessageDialog(this, "Invalid Student ID!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String storedPassword = voterDetails[2].trim();
            System.out.println("Input: ID=" + id + ", Password=" + passwordInput);
            System.out.println("Stored: ID=" + voterDetails[0] + ", Name=" + voterDetails[1] + ", Password=" + storedPassword);
            // Allow login if password matches or is empty (unregistered voter)
            if (storedPassword.equals(passwordInput) || storedPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Login successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
                setVisible(false);
                new VotingFrame(voterDetails[0], voterDetails[1]).setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Password!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        loginBtn.setBackground(new Color(15, 30, 60));
        loginBtn.setForeground(Color.WHITE);
        gbc.gridy = 5;
        loginBtn.setPreferredSize(new Dimension(200, 50));
        rightPanel.add(loginBtn, gbc);

        // Add panels to frame
        add(leftPanel);
        add(rightPanel);

        SwingUtilities.invokeLater(() -> this.requestFocusInWindow());
    }

    /**
     * Fetches voter details by ID from data_voters.txt.
     * Expects format: id:name:age:password
     * Returns [id, name, password] or null if not found.
     */
    private String[] getVoterDetailsById(String id) {
        File file = new File(VOTERS_FILE);
        if (!file.exists()) {
            System.out.println("Error: data_voters.txt not found at " + file.getAbsolutePath());
            JOptionPane.showMessageDialog(this, "Voter database not found!", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    System.out.println("Skipping empty line at line " + lineNumber);
                    continue;
                }
                System.out.println("Parsing line " + lineNumber + ": " + line);
                String[] parts = line.split(":", -1); // -1 to include empty fields
                if (parts.length >= 4) {
                    String idPart = parts[0].trim();
                    String namePart = parts[1].trim();
                    String passwordPart = parts[3].trim();
                    if (idPart.equalsIgnoreCase(id)) {
                        System.out.println("Found voter at line " + lineNumber + ": ID=" + idPart + ", Name=" + namePart + ", Password=" + passwordPart);
                        return new String[]{idPart, namePart, passwordPart};
                    }
                } else {
                    System.out.println("Invalid line format at line " + lineNumber + ": " + line);
                }
            }
        } catch (IOException ex) {
            System.out.println("Error reading data_voters.txt: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error reading voter database!", "Error", JOptionPane.ERROR_MESSAGE);
        }
        System.out.println("Voter ID not found: " + id);
        return null;
    }
}