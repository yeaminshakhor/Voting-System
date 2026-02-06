package Framesg;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

/**
 * RegisterFrame for voter registration in the Online Voting System.
 * Takes Student ID and Password, fetches Name from data_voters.txt, and updates password.
 * Expects format: id:name:age:password
 * Allows registration only for voters with empty passwords.
 */
public class RegisterFrame extends JFrame {

    private static final String VOTERS_FILE = "data_voters.txt";

    public RegisterFrame() {
        setTitle("Register");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2)); // Split into 2 panels

        // Left panel (Light blue background with logo)
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(new Color(173, 216, 230)); // Light blue
        leftPanel.setLayout(new GridBagLayout());

        try {
            ImageIcon icon = new ImageIcon("officiallogo.jpeg");
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

        // Right panel (Sky blue background with form)
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(new Color(135, 206, 235)); // Sky blue
        rightPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        JLabel title = new JLabel("Register", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 42));
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        rightPanel.add(title, gbc);

        gbc.gridwidth = 1;

        // Fields: Only ID, Password, Confirm Password
        JTextField studentId = createPlaceholderField("Enter Student ID");
        JPasswordField pass = createPlaceholderPassword("Enter Password");
        JPasswordField confirmPass = createPlaceholderPassword("Confirm Password");

        // Add fields
        gbc.gridy = 2; rightPanel.add(studentId, gbc);
        gbc.gridy = 4; rightPanel.add(pass, gbc);
        gbc.gridy = 6; rightPanel.add(confirmPass, gbc);

        // Buttons
        JButton done = new JButton("Done");
        JButton back = new JButton("Back");

        // Done button: Validate ID, check registration status, update password
        done.addActionListener(e -> {
            String id = studentId.getText().trim().replace("ID:", "").trim();
            String password = new String(pass.getPassword()).trim();
            String confirmPassword = new String(confirmPass.getPassword()).trim();

            // Validation: Check for empty fields or placeholders
            if (id.isEmpty() || id.equals("Enter Student ID") ||
                password.isEmpty() || password.equals("Enter Password") ||
                confirmPassword.isEmpty() || confirmPassword.equals("Confirm Password")) {
                JOptionPane.showMessageDialog(this, "Student ID and Password are required!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Password match and length
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (password.length() < 6) {
                JOptionPane.showMessageDialog(this, "Password must be at least 6 characters!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if ID exists and fetch details
            String[] voterDetails = getVoterDetailsById(id);
            if (voterDetails == null) {
                JOptionPane.showMessageDialog(this, "Student ID not found!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if already registered (non-empty password)
            if (!voterDetails[3].isEmpty()) {
                JOptionPane.showMessageDialog(this, "This voter is already registered!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Update voter record with new password
            if (updateVoterPassword(id, voterDetails[1], voterDetails[2], password)) {
                JOptionPane.showMessageDialog(this, "Registration successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
                setVisible(false);
                new LoginFrame().setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update password. Try again.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Back button: Return to landing page
        back.addActionListener(e -> {
            setVisible(false);
            new landing().setVisible(true);
            dispose();
        });

        // Style buttons
        done.setBackground(new Color(15, 30, 60));
        done.setForeground(Color.WHITE);
        back.setBackground(new Color(15, 30, 60));
        back.setForeground(Color.WHITE);
        gbc.gridy = 8;
        gbc.gridwidth = 1;
        rightPanel.add(done, gbc);
        gbc.gridy = 9;
        rightPanel.add(back, gbc);

        // Add both panels to frame
        add(leftPanel);
        add(rightPanel);

        SwingUtilities.invokeLater(() -> this.requestFocusInWindow());
    }

    /**
     * Creates a text field with placeholder text.
     */
    private JTextField createPlaceholderField(String placeholder) {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(250, 40));
        field.setForeground(Color.GRAY);
        field.setText(placeholder);

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(Color.GRAY);
                    field.setText(placeholder);
                }
            }
        });
        return field;
    }

    /**
     * Creates a password field with placeholder text.
     */
    private JPasswordField createPlaceholderPassword(String placeholder) {
        JPasswordField field = new JPasswordField(15);
        field.setPreferredSize(new Dimension(250, 40));
        field.setForeground(Color.GRAY);
        field.setEchoChar((char) 0);
        field.setText(placeholder);

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (String.valueOf(field.getPassword()).equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                    field.setEchoChar('•');
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (String.valueOf(field.getPassword()).isEmpty()) {
                    field.setForeground(Color.GRAY);
                    field.setText(placeholder);
                    field.setEchoChar((char) 0);
                }
            }
        });
        return field;
    }

    /**
     * Fetches voter details by ID from data_voters.txt.
     * Expects format: id:name:age:password
     * Returns [id, name, age, password] or null if not found.
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
                    String agePart = parts[2].trim();
                    String passwordPart = parts[3].trim();
                    if (idPart.equalsIgnoreCase(id.trim())) {
                        System.out.println("Found voter at line " + lineNumber + ": ID=" + idPart + ", Name=" + namePart + ", Age=" + agePart + ", Password=" + passwordPart);
                        return new String[]{idPart, namePart, agePart, passwordPart};
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

    /**
     * Updates the voter's password in data_voters.txt.
     */
    private boolean updateVoterPassword(String id, String name, String age, String newPassword) {
        // Read all lines
        String[] lines = new String[100]; // Adjust size if needed
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null && count < lines.length) {
                if (line.trim().isEmpty()) continue; // Skip empty lines
                lines[count++] = line;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }

        // Update the matching line
        boolean updated = false;
        for (int i = 0; i < count; i++) {
            String[] parts = lines[i].split(":", -1);
            if (parts.length >= 4 && parts[0].trim().equalsIgnoreCase(id)) {
                lines[i] = id + ":" + name + ":" + age + ":" + newPassword;
                updated = true;
            }
        }

        if (!updated) return false;

        // Write back to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(VOTERS_FILE))) {
            for (int i = 0; i < count; i++) {
                if (!lines[i].isEmpty()) {
                    writer.write(lines[i]);
                    writer.newLine();
                }
            }
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}