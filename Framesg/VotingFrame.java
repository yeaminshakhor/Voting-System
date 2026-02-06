package Framesg;

import java.awt.*;
import java.io.*;
import javax.swing.*;

/**
 * VotingFrame for casting votes in the Online Voting System.
 * Displays voter name and allows voting for nominees from database_nominees.txt.
 * Expects nominee format: id:name:symbol
 * Uses a fixed-size array instead of ArrayList for loading nominees.
 */
public class VotingFrame extends JFrame {
    private static final String NOMINEES_FILE = "database_nominees.txt";
    private static final String VOTES_FILE = "votes.txt";

    public VotingFrame(String voterId, String voterName) {
        // Set up the window
        setTitle("Voting Page");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Show a friendly welcome message with the voter's name and ID
        JLabel header = new JLabel("Welcome, " + voterName + " (ID: " + voterId + ")", SwingConstants.CENTER);
        header.setFont(new Font("Serif", Font.BOLD, 24));
        header.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        add(header, BorderLayout.NORTH);

        // Create a panel for selecting nominees
        JPanel nomineePanel = new JPanel();
        nomineePanel.setLayout(new GridLayout(0, 1, 10, 10));
        nomineePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Load nominees into a dropdown menu
        String[] nominees = loadNominees();
        JComboBox<String> nomineeComboBox = new JComboBox<>(nominees);
        nomineeComboBox.setFont(new Font("Serif", Font.PLAIN, 16));
        nomineePanel.add(new JLabel("Select Nominee:"));
        nomineePanel.add(nomineeComboBox);

        // Vote button to submit the vote
        JButton voteButton = new JButton("Cast Vote");
        voteButton.setFont(new Font("Serif", Font.BOLD, 16));
        voteButton.setBackground(new Color(15, 30, 60));
        voteButton.setForeground(Color.WHITE);
        voteButton.addActionListener(e -> {
            String selectedNominee = (String) nomineeComboBox.getSelectedItem();
            if (selectedNominee == null || selectedNominee.equals("No nominees available")) {
                JOptionPane.showMessageDialog(this, "Please select a valid nominee!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Extract nominee ID from display string (id - name (symbol))
            String nomineeId = selectedNominee.split(" - ")[0];
            if (saveVote(voterId, nomineeId)) {
                JOptionPane.showMessageDialog(this, "Your vote was cast successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                setVisible(false);
                new landing().setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to cast vote. Try again.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        nomineePanel.add(voteButton);

        // Back button to return to login
        JButton backButton = new JButton("Back");
        backButton.setFont(new Font("Serif", Font.BOLD, 16));
        backButton.setBackground(new Color(15, 30, 60));
        backButton.setForeground(Color.WHITE);
        backButton.addActionListener(e -> {
            setVisible(false);
            new LoginFrame().setVisible(true);
            dispose();
        });
        nomineePanel.add(backButton);

        // Add the nominee panel to the window
        add(nomineePanel, BorderLayout.CENTER);

        // Ensure the window is focused
        SwingUtilities.invokeLater(() -> this.requestFocusInWindow());
    }

    /**
     * Loads nominees from database_nominees.txt into a fixed-size array.
     * Expects format: id:name:symbol
     * Displays as: id - name (symbol)
     */
    private String[] loadNominees() {
        // First pass: count valid nominee lines
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(NOMINEES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue; // Skip empty lines
                String[] parts = line.split(":", -1);
                if (parts.length >= 3) {
                    count++;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return new String[]{"No nominees available"};
        }

        // If no valid nominees, return default
        if (count == 0) {
            return new String[]{"No nominees available"};
        }

        // Second pass: populate the array
        String[] nominees = new String[count];
        int index = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(NOMINEES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null && index < count) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":", -1);
                if (parts.length >= 3) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String symbol = parts[2].trim();
                    nominees[index++] = id + " - " + name + " (" + symbol + ")";
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return new String[]{"No nominees available"};
        }

        return nominees;
    }

    /**
     * Saves the voter's vote to votes.txt.
     */
    private boolean saveVote(String voterId, String nomineeId) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(VOTES_FILE, true))) {
            writer.write(voterId + ":" + nomineeId);
            writer.newLine();
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}