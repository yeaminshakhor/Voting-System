package Framesg;

import Data.AdminData;
import Data.ElectionData;
import Entities.Nominee;
import Entities.Voter;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

/**
 * Functional AdminDashboard with working voter and nominee management + voting features.
 */
public class AdminDashboard extends JFrame implements ActionListener {
    private final Color BACKGROUND_COLOR = Color.WHITE;
    private final Color PRIMARY_BLUE = Color.decode("#2563EB");
    private final Color DARK_BLUE = Color.decode("#1E40AF");
    private final Color CARD_WHITE = Color.WHITE;
    private final Color TEXT_DARK = Color.decode("#1F2937");
    private final Color SUCCESS_GREEN = Color.decode("#22C55E");
    private final Color ORANGE = Color.decode("#FF9800");
    private final Color MAGENTA = Color.decode("#E91E63");
    private final Color RED = Color.decode("#F44336");

    private String adminId;
    private JFrame parentFrame;
    private JPanel mainPanel;

    public AdminDashboard(String adminId, JFrame parentFrame) {
        this.adminId = adminId;
        this.parentFrame = parentFrame;
        
        setupWindow();
        initTopBar();
        initMainPanel();
        setVisible(true);
    }

    private void setupWindow() {
        setTitle("Election Admin Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND_COLOR);
    }

    private void initTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(DARK_BLUE);
        topBar.setPreferredSize(new Dimension(getWidth(), 50));
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        String adminName = AdminData.getAdminNameById(adminId);
        String adminRole = AdminData.getRoleById(adminId);
        if (adminName == null) adminName = "Administrator";
        if (adminRole == null) adminRole = "admin";
        
        JLabel adminLabel = new JLabel("👤 " + adminName + " (" + adminRole + ")");
        adminLabel.setForeground(Color.WHITE);
        adminLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        topBar.add(adminLabel, BorderLayout.WEST);

        JButton logoutButton = new JButton("Logout");
        logoutButton.setBackground(PRIMARY_BLUE);
        logoutButton.setForeground(Color.WHITE);
        logoutButton.addActionListener(e -> {
            setVisible(false);
            parentFrame.setVisible(true);
        });
        topBar.add(logoutButton, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
    }

    private void initMainPanel() {
        mainPanel = new JPanel();
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setLayout(new GridLayout(0, 2, 20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Election Setup Card
        mainPanel.add(createCard("💼 Election Setup", 
            new String[]{"Configure election settings"}, 
            "Configure Election", PRIMARY_BLUE));

        // Voter Management Card
        mainPanel.add(createCard("👥 Voter Management", 
            new String[]{"Add voters", "View voter list", "Delete voters"}, 
            "Manage Voters", SUCCESS_GREEN));

        // Nominee Management Card
        mainPanel.add(createCard("🗳️ Nominee Management", 
            new String[]{"Add nominees", "View nominee list", "Delete nominees"}, 
            "Manage Nominees", PRIMARY_BLUE));

        // NEW: Live Results Card
        mainPanel.add(createCard("📊 Live Results", 
            new String[]{"Real-time vote counts", "Current standings", "Turnout statistics"}, 
            "View Live Results", ORANGE));

        // NEW: Voter Status Card
        mainPanel.add(createCard("👀 Voter Status", 
            new String[]{"Who has voted", "Who hasn't voted", "Participation tracking"}, 
            "Check Voter Status", MAGENTA));

        // NEW: Publish Results Card
        mainPanel.add(createCard("📋 Publish Results", 
            new String[]{"Generate final report", "Official results certificate"}, 
            "Publish Final Results", RED));

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createCard(String title, String[] bullets, String buttonText, Color accentColor) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(10, 10));
        card.setBackground(CARD_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_DARK);
        card.add(titleLabel, BorderLayout.NORTH);

        JPanel bulletPanel = new JPanel();
        bulletPanel.setLayout(new BoxLayout(bulletPanel, BoxLayout.Y_AXIS));
        bulletPanel.setBackground(CARD_WHITE);
        
        for (String bullet : bullets) {
            JLabel bulletLabel = new JLabel("• " + bullet);
            bulletLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            bulletLabel.setForeground(TEXT_DARK);
            bulletPanel.add(bulletLabel);
        }
        card.add(bulletPanel, BorderLayout.CENTER);

        JButton actionButton = new JButton(buttonText);
        actionButton.setBackground(accentColor);
        actionButton.setForeground(Color.WHITE);
        actionButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        actionButton.addActionListener(this);
        actionButton.setActionCommand(buttonText);
        card.add(actionButton, BorderLayout.SOUTH);

        return card;
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        switch (command) {
            case "Configure Election":
                showElectionSetup();
                break;
            case "Manage Voters":
                showVoterManagement();
                break;
            case "Manage Nominees":
                showNomineeManagement();
                break;
            case "View Live Results":
                showLiveResults();
                break;
            case "Check Voter Status":
                showVoterStatus();
                break;
            case "Publish Final Results":
                publishFinalResults();
                break;
        }
    }

    private void showElectionSetup() {
        String[] options = {"First-past-the-post", "Ranked Choice", "Proportional Representation"};
        String selection = (String) JOptionPane.showInputDialog(this, 
            "Select Election Type:", "Election Setup", 
            JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        
        if (selection != null) {
            JOptionPane.showMessageDialog(this, "Election type set to: " + selection);
        }
    }

    private void showVoterManagement() {
        String[] options = {"Add Voter", "View Voter List", "Delete Voter"};
        String choice = (String) JOptionPane.showInputDialog(this,
            "Select Action:", "Voter Management",
            JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice != null) {
            switch (choice) {
                case "Add Voter":
                    addVoterDialog();
                    break;
                case "View Voter List":
                    viewVoterList();
                    break;
                case "Delete Voter":
                    deleteVoterDialog();
                    break;
            }
        }
    }

    private void addVoterDialog() {
        JTextField idField = new JTextField(15);
        JTextField nameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);

        Object[] fields = {
            "Voter ID:", idField,
            "Full Name:", nameField,
            "Password:", passwordField
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Add New Voter", 
            JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String id = idField.getText().trim();
            String name = nameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            if (id.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Voter ID and Name are required!");
                return;
            }

            if (ElectionData.voterIdExists(id)) {
                JOptionPane.showMessageDialog(this, "Voter ID already exists!");
                return;
            }

            Voter voter = new Voter(id, name, password);
            if (ElectionData.registerVoter(voter)) {
                JOptionPane.showMessageDialog(this, "Voter added successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add voter.");
            }
        }
    }

    private void viewVoterList() {
        // Get all voters from database
        String[] voterStrings = ElectionData.getAllVoters();
        if (voterStrings.length == 0) {
            JOptionPane.showMessageDialog(this, "No voters found.");
            return;
        }

        StringBuilder list = new StringBuilder();
        list.append("═══════════════════════════════════════════════════\n");
        list.append("                     VOTER LIST                     \n");
        list.append("═══════════════════════════════════════════════════\n\n");
        
        int registeredCount = 0;
        int unregisteredCount = 0;
        
        for (String voterStr : voterStrings) {
            String[] parts = voterStr.split(":");
            if (parts.length >= 3) {
                String id = parts[0];
                String name = parts[1];
                String password = parts[2];
                String status = password.isEmpty() ? "Unregistered" : "Registered";
                
                if (password.isEmpty()) {
                    unregisteredCount++;
                } else {
                    registeredCount++;
                }
                
                list.append(String.format("ID: %-20s\n", id));
                list.append(String.format("Name: %-25s\n", name));
                list.append(String.format("Status: %-20s\n", status));
                list.append("───────────────────────────────────────────────\n");
            }
        }
        
        list.append("\n═══════════════════════════════════════════════════\n");
        list.append("SUMMARY:\n");
        list.append("  Total Voters: ").append(voterStrings.length).append("\n");
        list.append("  Registered: ").append(registeredCount).append("\n");
        list.append("  Unregistered: ").append(unregisteredCount).append("\n");
        list.append("═══════════════════════════════════════════════════\n");

        JTextArea textArea = new JTextArea(list.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 500));
        JOptionPane.showMessageDialog(this, scrollPane, "Voter List", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteVoterDialog() {
        String[] voterStrings = ElectionData.getAllVoters();
        if (voterStrings.length == 0) {
            JOptionPane.showMessageDialog(this, "No voters found.");
            return;
        }

        // Create display options with both ID and Name
        String[] voterOptions = new String[voterStrings.length];
        for (int i = 0; i < voterStrings.length; i++) {
            String[] parts = voterStrings[i].split(":");
            if (parts.length >= 2) {
                voterOptions[i] = parts[0] + " - " + parts[1];
            } else {
                voterOptions[i] = voterStrings[i];
            }
        }

        String selectedVoter = (String) JOptionPane.showInputDialog(this,
            "Select voter to delete:", "Delete Voter",
            JOptionPane.QUESTION_MESSAGE, null, voterOptions, voterOptions[0]);

        if (selectedVoter != null) {
            // Extract voter name from selection
            String voterName = selectedVoter.split(" - ")[1];
            
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete:\n" + selectedVoter + "?\n\n" +
                "This action cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                if (ElectionData.deleteVoter(voterName)) {
                    JOptionPane.showMessageDialog(this, 
                        "Voter deleted successfully!\n" + selectedVoter,
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Failed to delete voter.\n" + selectedVoter,
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void showNomineeManagement() {
        String[] options = {"Add Nominee", "View Nominee List", "Delete Nominee"};
        String choice = (String) JOptionPane.showInputDialog(this,
            "Select Action:", "Nominee Management",
            JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice != null) {
            switch (choice) {
                case "Add Nominee":
                    addNomineeDialog();
                    break;
                case "View Nominee List":
                    viewNomineeList();
                    break;
                case "Delete Nominee":
                    deleteNomineeDialog();
                    break;
            }
        }
    }

    private void addNomineeDialog() {
        JTextField idField = new JTextField(15);
        JTextField nameField = new JTextField(15);
        JTextField partyField = new JTextField(15);

        Object[] fields = {
            "Nominee ID:", idField,
            "Full Name:", nameField,
            "Political Party:", partyField
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Add New Nominee", 
            JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String id = idField.getText().trim();
            String name = nameField.getText().trim();
            String party = partyField.getText().trim();

            if (id.isEmpty() || name.isEmpty() || party.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required!");
                return;
            }

            if (ElectionData.nomineeIdExists(id)) {
                JOptionPane.showMessageDialog(this, "Nominee ID already exists!");
                return;
            }

            Nominee nominee = new Nominee(id, name, party);
            if (ElectionData.addNominee(nominee)) {
                JOptionPane.showMessageDialog(this, "Nominee added successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add nominee.");
            }
        }
    }

    private void viewNomineeList() {
        String[] nominees = ElectionData.getAllNominees();
        if (nominees.length == 0) {
            JOptionPane.showMessageDialog(this, "No nominees found.");
            return;
        }

        StringBuilder list = new StringBuilder();
        list.append("═══════════════════════════════════════════════════\n");
        list.append("                   NOMINEE LIST                    \n");
        list.append("═══════════════════════════════════════════════════\n\n");
        
        for (String nominee : nominees) {
            String[] parts = nominee.split(":");
            if (parts.length >= 3) {
                list.append(String.format("ID: %-20s\n", parts[0]));
                list.append(String.format("Name: %-25s\n", parts[1]));
                list.append(String.format("Party: %-25s\n", parts[2]));
                list.append("───────────────────────────────────────────────\n");
            }
        }
        
        list.append("\n═══════════════════════════════════════════════════\n");
        list.append("Total Nominees: ").append(nominees.length).append("\n");
        list.append("═══════════════════════════════════════════════════\n");

        JTextArea textArea = new JTextArea(list.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        JOptionPane.showMessageDialog(this, scrollPane, "Nominee List", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteNomineeDialog() {
        String[] nominees = ElectionData.getAllNominees();
        if (nominees.length == 0) {
            JOptionPane.showMessageDialog(this, "No nominees found.");
            return;
        }

        String[] nomineeOptions = new String[nominees.length];
        for (int i = 0; i < nominees.length; i++) {
            String[] parts = nominees[i].split(":");
            if (parts.length >= 2) {
                nomineeOptions[i] = parts[1] + " (" + parts[0] + ")";
            }
        }

        String selectedNominee = (String) JOptionPane.showInputDialog(this,
            "Select nominee to delete:", "Delete Nominee",
            JOptionPane.QUESTION_MESSAGE, null, nomineeOptions, nomineeOptions[0]);

        if (selectedNominee != null) {
            // Extract nominee name (remove ID from parentheses)
            String nomineeName = selectedNominee.split("\\(")[0].trim();
            
            int confirm = JOptionPane.showConfirmDialog(this,
                "Delete nominee: " + selectedNominee + "?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                if (ElectionData.deleteNominee(nomineeName)) {
                    JOptionPane.showMessageDialog(this, "Nominee deleted successfully!");
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete nominee.");
                }
            }
        }
    }

    // NEW METHODS FOR VOTE COUNTING AND MONITORING

    private void showLiveResults() {
        Map<String, Integer> voteCounts = getVoteCounts();
        int totalVotes = getTotalVotesCast();
        int totalVoters = getTotalRegisteredVoters();
        
        StringBuilder results = new StringBuilder();
        results.append("📊 LIVE ELECTION RESULTS\n\n");
        results.append("Total Votes Cast: ").append(totalVotes).append("/").append(totalVoters)
               .append(" (").append((totalVoters > 0 ? (totalVotes * 100 / totalVoters) : 0)).append("% turnout)\n\n");
        
        if (voteCounts.isEmpty()) {
            results.append("No votes cast yet.\n");
        } else {
            results.append("Vote Counts:\n");
            for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
                String nomineeId = entry.getKey();
                int votes = entry.getValue();
                
                // Find nominee name
                String nomineeName = "Unknown";
                String[] nominees = ElectionData.getAllNominees();
                for (String nominee : nominees) {
                    String[] parts = nominee.split(":");
                    if (parts.length >= 3 && parts[0].equals(nomineeId)) {
                        nomineeName = parts[1] + " (" + parts[2] + ")";
                        break;
                    }
                }
                
                results.append("• ").append(nomineeName).append(": ").append(votes).append(" votes (")
                       .append(totalVotes > 0 ? (votes * 100 / totalVotes) : 0).append("%)\n");
            }
        }
        
        JTextArea textArea = new JTextArea(results.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        
        JOptionPane.showMessageDialog(this, scrollPane, "Live Election Results", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void showVoterStatus() {
        String[] allVoters = ElectionData.getAllVoters();
        String[] votedVoters = getVotersWhoVoted();
        
        StringBuilder status = new StringBuilder();
        status.append("👥 VOTER PARTICIPATION STATUS\n\n");
        status.append("Total Registered Voters: ").append(allVoters.length).append("\n");
        status.append("Voters Who Have Voted: ").append(votedVoters.length).append("\n");
        status.append("Voters Yet to Vote: ").append(allVoters.length - votedVoters.length).append("\n");
        status.append("Turnout: ").append(allVoters.length > 0 ? 
            (votedVoters.length * 100 / allVoters.length) : 0).append("%\n\n");
        
        status.append("✅ VOTED:\n");
        for (String voterId : votedVoters) {
            // Find voter name
            for (String voterInfo : allVoters) {
                String[] parts = voterInfo.split(":");
                if (parts.length >= 2 && parts[0].equals(voterId)) {
                    status.append("• ").append(parts[1]).append(" (ID: ").append(voterId).append(")\n");
                    break;
                }
            }
        }
        
        status.append("\n❌ NOT VOTED YET:\n");
        for (String voterInfo : allVoters) {
            String[] parts = voterInfo.split(":");
            if (parts.length >= 2) {
                String voterId = parts[0];
                boolean hasVoted = false;
                for (String voted : votedVoters) {
                    if (voted.equals(voterId)) {
                        hasVoted = true;
                        break;
                    }
                }
                if (!hasVoted) {
                    status.append("• ").append(parts[1]).append(" (ID: ").append(voterId).append(")\n");
                }
            }
        }
        
        JTextArea textArea = new JTextArea(status.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        
        JOptionPane.showMessageDialog(this, scrollPane, "Voter Participation Status", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void publishFinalResults() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Publish final results? This will create an official results file.",
            "Publish Results", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter("election_results.txt")) {
                Map<String, Integer> voteCounts = getVoteCounts();
                int totalVotes = getTotalVotesCast();
                
                writer.println("OFFICIAL ELECTION RESULTS");
                writer.println("Published: " + new Date());
                writer.println("Total Votes Cast: " + totalVotes);
                writer.println();
                writer.println("RESULTS:");
                
                for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
                    String nomineeId = entry.getKey();
                    int votes = entry.getValue();
                    
                    // Find nominee details
                    String[] nominees = ElectionData.getAllNominees();
                    for (String nominee : nominees) {
                        String[] parts = nominee.split(":");
                        if (parts.length >= 3 && parts[0].equals(nomineeId)) {
                            writer.println(parts[1] + " (" + parts[2] + "): " + votes + " votes (" + 
                                (totalVotes > 0 ? (votes * 100 / totalVotes) : 0) + "%)");
                            break;
                        }
                    }
                }
                
                JOptionPane.showMessageDialog(this, 
                    "Results published to election_results.txt\nFile location: " + 
                    new java.io.File("election_results.txt").getAbsolutePath());
                    
            } catch (java.io.IOException e) {
                JOptionPane.showMessageDialog(this, "Error publishing results: " + e.getMessage());
            }
        }
    }

    // Helper methods for vote counting
    private Map<String, Integer> getVoteCounts() {
        Map<String, Integer> voteCounts = new HashMap<>();
        String[] votes = ElectionData.getAllVotes();
        
        for (String vote : votes) {
            String[] parts = vote.split(":");
            if (parts.length >= 2) {
                String nomineeId = parts[1];
                voteCounts.put(nomineeId, voteCounts.getOrDefault(nomineeId, 0) + 1);
            }
        }
        return voteCounts;
    }

    private String[] getVotersWhoVoted() {
        ArrayList<String> votedVoters = new ArrayList<>();
        String[] votes = ElectionData.getAllVotes();
        
        for (String vote : votes) {
            String[] parts = vote.split(":");
            if (parts.length >= 1) {
                votedVoters.add(parts[0]);
            }
        }
        return votedVoters.toArray(new String[0]);
    }

    private int getTotalVotesCast() {
        String[] votes = ElectionData.getAllVotes();
        return votes.length;
    }

    private int getTotalRegisteredVoters() {
        String[] voters = ElectionData.getAllVoters();
        return voters.length;
    }
}