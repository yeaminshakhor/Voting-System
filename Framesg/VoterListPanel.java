package Framesg;

import java.awt.*;
import java.io.*;
import javax.swing.*;

/**
 * A panel to show the voter list from data_voters.txt in a clear, readable way.
 * Uses a fixed-size array to load voters, avoiding ArrayList.
 * Expects format: id:name:age:password
 */
public class VoterListPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String VOTERS_FILE = "data_voters.txt";

    public VoterListPanel() {
        // Set up the panel with a scrollable text area
        setLayout(new BorderLayout());
        JTextArea voterListArea = new JTextArea();
        voterListArea.setEditable(false);
        voterListArea.setFont(new Font("Serif", Font.PLAIN, 16));
        JScrollPane scrollPane = new JScrollPane(voterListArea);
        add(scrollPane, BorderLayout.CENTER);

        // Load and display the voter list
        String voterList = loadVoterList();
        voterListArea.setText(voterList);
    }

    /**
     * Loads voters from data_voters.txt into a string for display.
     * Expects format: id:name:age:password
     */
    private String loadVoterList() {
        StringBuilder voterList = new StringBuilder("Voter List:\n\n");
        
        File file = new File(VOTERS_FILE);
        if (!file.exists()) {
            return "Voter List:\n\nError: data_voters.txt not found at " + file.getAbsolutePath();
        }

        // First pass: count valid voter lines
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":", -1);
                if (parts.length >= 4) {
                    count++;
                }
            }
        } catch (IOException ex) {
            voterList.append("Error loading voter list: ").append(ex.getMessage());
            ex.printStackTrace();
            return voterList.toString();
        }

        // If no voters, return message
        if (count == 0) {
            return "Voter List:\n\nNo voters found.";
        }

        // Second pass: populate fixed-size array
        String[] voters = new String[count];
        int index = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null && index < count) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":", -1);
                if (parts.length >= 4) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String password = parts[3].trim();
                    String status = password.isEmpty() ? "Unregistered" : "Registered";
                    voters[index++] = "ID: " + id + ", Name: " + name + ", Status: " + status;
                }
            }
        } catch (IOException ex) {
            voterList.append("Error loading voter list: ").append(ex.getMessage());
            ex.printStackTrace();
            return voterList.toString();
        }

        // Build the display string
        for (String voter : voters) {
            if (voter != null) {
                voterList.append(voter).append("\n");
            }
        }

        return voterList.toString();
    }
}