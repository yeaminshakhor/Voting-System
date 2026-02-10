package Framesg;

import Data.ElectionData;
import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.ListSelectionModel;

/**
 * GUI interface to display election results following AdminDashboard design patterns
 */
public class ElectionResults extends JFrame implements ActionListener {
    private static final long serialVersionUID = 1L;
    private final Color BACKGROUND_COLOR = Color.WHITE;
    private final Color DARK_BLUE = Color.decode("#1E40AF");
    private final Color PRIMARY_BLUE = Color.decode("#2563EB");
    private final Color SUCCESS_GREEN = Color.decode("#22C55E");
    private final Color TEXT_DARK = Color.decode("#1F2937");
    private final Color ORANGE = Color.decode("#FF9800");
    private final Color MAGENTA = Color.decode("#E91E63");
    
    private JFrame parentFrame;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JLabel totalVotesLabel, totalVotersLabel, turnoutLabel, leadingLabel;

    public ElectionResults(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        
        setupWindow();
        initTopBar();
        initMainPanel();
        loadResultsData();
        setVisible(true);
    }

    private void setupWindow() {
        setTitle("Election Results Dashboard");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND_COLOR);
    }

    private void initTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(DARK_BLUE);
        topBar.setPreferredSize(new Dimension(getWidth(), 50));
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel titleLabel = new JLabel("📊 ELECTION RESULTS DASHBOARD");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        topBar.add(titleLabel, BorderLayout.WEST);

        JButton backButton = new JButton("← Back to Dashboard");
        backButton.setBackground(PRIMARY_BLUE);
        backButton.setForeground(Color.WHITE);
        backButton.setFocusPainted(false);
        backButton.addActionListener(e -> {
            dispose();
            parentFrame.setVisible(true);
        });
        topBar.add(backButton, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
    }

    private void initMainPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setLayout(new BorderLayout(20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Statistics Cards Panel (Top)
        mainPanel.add(createStatisticsPanel(), BorderLayout.NORTH);
        
        // Results Table Panel (Center)
        mainPanel.add(createResultsTablePanel(), BorderLayout.CENTER);
        
        // Action Buttons Panel (South)
        mainPanel.add(createActionButtonsPanel(), BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createStatisticsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        statsPanel.setBackground(BACKGROUND_COLOR);
        
        // Card 1: Total Votes
        JPanel votesCard = createStatCard("Total Votes Cast", "0", PRIMARY_BLUE);
        totalVotesLabel = (JLabel) ((JPanel) votesCard.getComponent(1)).getComponent(0);
        statsPanel.add(votesCard);

        // Card 2: Total Voters
        JPanel votersCard = createStatCard("Total Voters", "0", MAGENTA);
        totalVotersLabel = (JLabel) ((JPanel) votersCard.getComponent(1)).getComponent(0);
        statsPanel.add(votersCard);

        // Card 3: Turnout Percentage
        JPanel turnoutCard = createStatCard("Voter Turnout", "0%", SUCCESS_GREEN);
        turnoutLabel = (JLabel) ((JPanel) turnoutCard.getComponent(1)).getComponent(0);
        statsPanel.add(turnoutCard);

        // Card 4: Leading Candidate
        JPanel leadingCard = createStatCard("Leading Candidate", "None", ORANGE);
        leadingLabel = (JLabel) ((JPanel) leadingCard.getComponent(1)).getComponent(0);
        statsPanel.add(leadingCard);

        return statsPanel;
    }

    private JPanel createStatCard(String title, String value, Color accentColor) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(10, 10));
        card.setBackground(BACKGROUND_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLabel.setForeground(TEXT_DARK);
        card.add(titleLabel, BorderLayout.NORTH);

        JPanel valuePanel = new JPanel();
        valuePanel.setBackground(BACKGROUND_COLOR);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        valueLabel.setForeground(accentColor);
        valuePanel.add(valueLabel);
        card.add(valuePanel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createResultsTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(BACKGROUND_COLOR);
        tablePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Detailed Election Results"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Create table model
        String[] columns = {"Rank", "Candidate ID", "Candidate Name", "Political Party", "Votes Received", "Percentage", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
        };

        resultsTable = new JTable(tableModel);
        resultsTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        resultsTable.setRowHeight(30);
        resultsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        resultsTable.getTableHeader().setBackground(DARK_BLUE);
        resultsTable.getTableHeader().setForeground(Color.WHITE);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Center align all columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < resultsTable.getColumnCount(); i++) {
            resultsTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setPreferredSize(new Dimension(800, 300));
        
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        return tablePanel;
    }

    private JPanel createActionButtonsPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(BACKGROUND_COLOR);

        // Refresh Button
        JButton refreshButton = new JButton("🔄 Refresh Results");
        styleButton(refreshButton, PRIMARY_BLUE);
        refreshButton.addActionListener(e -> refreshResults());
        buttonPanel.add(refreshButton);

        // Export Button
        JButton exportButton = new JButton("📄 Export to File");
        styleButton(exportButton, SUCCESS_GREEN);
        exportButton.addActionListener(e -> exportResults());
        buttonPanel.add(exportButton);

        // View Voter Status Button
        JButton voterStatusButton = new JButton("👥 View Voter Status");
        styleButton(voterStatusButton, MAGENTA);
        voterStatusButton.addActionListener(e -> showVoterStatus());
        buttonPanel.add(voterStatusButton);

        // Close Button
        JButton closeButton = new JButton("❌ Close");
        styleButton(closeButton, Color.decode("#F44336"));
        closeButton.addActionListener(e -> {
            dispose();
            parentFrame.setVisible(true);
        });
        buttonPanel.add(closeButton);

        return buttonPanel;
    }

    private void styleButton(JButton button, Color backgroundColor) {
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(backgroundColor.darker(), 1),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        
        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(backgroundColor.brighter());
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(backgroundColor);
            }
        });
    }

    private void loadResultsData() {
        Map<String, Integer> voteCounts = ElectionData.getVoteCounts();
        int totalVotes = ElectionData.getTotalVotesCast();
        int totalVoters = ElectionData.getTotalRegisteredVoters();
        int turnoutPercentage = totalVoters > 0 ? (totalVotes * 100 / totalVoters) : 0;

        // Update statistics cards
        totalVotesLabel.setText(String.valueOf(totalVotes));
        totalVotersLabel.setText(String.valueOf(totalVoters));
        turnoutLabel.setText(turnoutPercentage + "%");

        // Clear existing table data
        tableModel.setRowCount(0);

        if (voteCounts.isEmpty()) {
            tableModel.addRow(new Object[]{
                "-", "N/A", "No votes cast yet", "N/A", "0", "0%", "PENDING"
            });
            leadingLabel.setText("None");
        } else {
            // Find maximum votes for winner determination
            int maxVotes = 0;
            String leadingCandidate = "None";
            for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
                if (entry.getValue() > maxVotes) {
                    maxVotes = entry.getValue();
                    // Find nominee name
                    String nomineeId = entry.getKey();
                    String[] nominees = ElectionData.getAllNominees();
                    for (String nominee : nominees) {
                        String[] parts = nominee.split(":");
                        if (parts.length >= 3 && parts[0].equals(nomineeId)) {
                            leadingCandidate = parts[1];
                            break;
                        }
                    }
                }
            }
            
            leadingLabel.setText(leadingCandidate);

            int rank = 1;
            for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
                String nomineeId = entry.getKey();
                int votes = entry.getValue();
                int percentage = totalVotes > 0 ? (votes * 100 / totalVotes) : 0;

                // Find nominee details
                String nomineeName = "Unknown";
                String partyName = "Unknown";
                String[] nominees = ElectionData.getAllNominees();
                for (String nominee : nominees) {
                    String[] parts = nominee.split(":");
                    if (parts.length >= 3 && parts[0].equals(nomineeId)) {
                        nomineeName = parts[1];
                        partyName = parts[2];
                        break;
                    }
                }

                String status = (votes == maxVotes) ? "🏆 LEADING" : "RUNNING";
                
                tableModel.addRow(new Object[]{
                    String.valueOf(rank++),
                    nomineeId,
                    nomineeName,
                    partyName,
                    String.valueOf(votes),
                    percentage + "%",
                    status
                });
            }
        }
    }

    private void refreshResults() {
        loadResultsData();
        JOptionPane.showMessageDialog(this,
            "Results refreshed successfully!",
            "Refresh Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportResults() {
        try (java.io.PrintWriter writer = new java.io.PrintWriter("election_results_" + System.currentTimeMillis() + ".txt")) {
            Map<String, Integer> voteCounts = ElectionData.getVoteCounts();
            int totalVotes = ElectionData.getTotalVotesCast();
            int totalVoters = ElectionData.getTotalRegisteredVoters();
            
            writer.println("═══════════════════════════════════════════════════");
            writer.println("           OFFICIAL ELECTION RESULTS               ");
            writer.println("═══════════════════════════════════════════════════");
            writer.println("Generated: " + new Date());
            writer.println("Total Registered Voters: " + totalVoters);
            writer.println("Total Votes Cast: " + totalVotes);
            writer.println("Voter Turnout: " + (totalVoters > 0 ? (totalVotes * 100 / totalVoters) : 0) + "%");
            writer.println("═══════════════════════════════════════════════════\n");
            
            if (voteCounts.isEmpty()) {
                writer.println("No votes have been cast in this election.");
            } else {
                writer.println("DETAILED RESULTS:");
                writer.println(String.format("%-5s %-15s %-25s %-20s %-10s %-12s %-10s", 
                    "Rank", "Candidate ID", "Candidate Name", "Party", "Votes", "Percentage", "Status"));
                writer.println("─────────────────────────────────────────────────────────────────────────────────────────────────────");
                
                int maxVotes = 0;
                for (int votes : voteCounts.values()) {
                    if (votes > maxVotes) maxVotes = votes;
                }
                
                int rank = 1;
                for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
                    String nomineeId = entry.getKey();
                    int votes = entry.getValue();
                    int percentage = totalVotes > 0 ? (votes * 100 / totalVotes) : 0;
                    
                    String nomineeName = "Unknown";
                    String partyName = "Unknown";
                    String[] nominees = ElectionData.getAllNominees();
                    for (String nominee : nominees) {
                        String[] parts = nominee.split(":");
                        if (parts.length >= 3 && parts[0].equals(nomineeId)) {
                            nomineeName = parts[1];
                            partyName = parts[2];
                            break;
                        }
                    }
                    
                    String status = (votes == maxVotes) ? "WINNER" : "RUNNER UP";
                    writer.println(String.format("%-5d %-15s %-25s %-20s %-10d %-12s %-10s", 
                        rank++, nomineeId, nomineeName, partyName, votes, percentage + "%", status));
                }
            }
            
            writer.println("\n═══════════════════════════════════════════════════");
            writer.println("END OF OFFICIAL REPORT");
            writer.println("═══════════════════════════════════════════════════");
            
            JOptionPane.showMessageDialog(this,
                "✅ Results exported successfully!\n" +
                "File saved in current directory.",
                "Export Complete",
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "❌ Error exporting results: " + e.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showVoterStatus() {
        String[] allVoters = ElectionData.getAllVoters();
        String[] votedVoters = getVotersWhoVoted();
        
        StringBuilder status = new StringBuilder();
        status.append("VOTER PARTICIPATION STATUS\n\n");
        status.append("Total Registered Voters: ").append(allVoters.length).append("\n");
        status.append("Voters Who Have Voted: ").append(votedVoters.length).append("\n");
        status.append("Voters Yet to Vote: ").append(allVoters.length - votedVoters.length).append("\n");
        status.append("Turnout: ").append(allVoters.length > 0 ? 
            (votedVoters.length * 100 / allVoters.length) : 0).append("%\n\n");
        
        status.append("✅ VOTED:\n");
        for (String voterId : votedVoters) {
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

    private String[] getVotersWhoVoted() {
        java.util.ArrayList<String> votedVoters = new java.util.ArrayList<>();
        String[] votes = ElectionData.getAllVotes();
        
        for (String vote : votes) {
            String[] parts = vote.split(":");
            if (parts.length >= 1) {
                votedVoters.add(parts[0]);
            }
        }
        return votedVoters.toArray(new String[0]);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Action handling is done through individual button listeners
    }
}