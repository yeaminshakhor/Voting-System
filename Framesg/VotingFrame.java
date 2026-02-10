package Framesg;

import Data.ElectionData;
import Data.ElectionScheduler;
import Utils.Theme;
import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Enhanced VotingFrame for casting votes in the Online Voting System.
 * Integrates with ElectionScheduler for election timing validation.
 * Features improved UI, validation, and error handling.
 */
public class VotingFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private String voterId;
    private String voterName;
    private Timer countdownTimer;
    private JLabel timeRemainingLabel;
    private JLabel electionStatusLabel;
    
    public VotingFrame(String voterId, String voterName) {
        this.voterId = voterId;
        this.voterName = voterName;
        
        // First, check voting eligibility
        if (!validateVotingEligibility()) {
            return;
        }
        
        setupWindow();
        initUI();
        startCountdownTimer();
        setVisible(true);
    }
    
    private boolean validateVotingEligibility() {
        // Check if election is active
        if (!ElectionScheduler.isVotingAllowed()) {
            String status = ElectionScheduler.getElectionStatus();
            JOptionPane.showMessageDialog(null,
                "❌ Voting is not allowed at this time!\n\n" + status +
                "\n\nPlease try again during the election period.",
                "Voting Not Available", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        // Check if voter has already voted
        if (ElectionData.hasVoted(voterId)) {
            String message = "⚠️ You have already cast your vote!\n\n" +
                           "Each voter can only vote once per election.\n" +
                           "If you believe this is an error, please contact the election administrator.";
            
            JOptionPane.showMessageDialog(null, message,
                "Already Voted", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        
        return true;
    }

    private void setupWindow() {
        setTitle("Voting Booth - " + voterName);
        setSize(850, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BACKGROUND_WHITE);
    }

    private void initUI() {
        // Header Panel
        add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Content Panel
        add(createContentPanel(), BorderLayout.CENTER);
        
        // Footer Panel
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY_BLUE);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        // Welcome message
        JLabel welcomeLabel = new JLabel("🗳️ Welcome, " + voterName);
        welcomeLabel.setFont(Theme.TITLE_FONT);
        welcomeLabel.setForeground(Color.WHITE);
        header.add(welcomeLabel, BorderLayout.WEST);
        
        // Voter ID display
        JLabel idLabel = new JLabel("ID: " + voterId);
        idLabel.setFont(Theme.SMALL_FONT);
        idLabel.setForeground(new Color(220, 220, 255));
        header.add(idLabel, BorderLayout.CENTER);
        
        // Election status
        electionStatusLabel = new JLabel("Election: Active");
        electionStatusLabel.setFont(Theme.SMALL_FONT);
        electionStatusLabel.setForeground(new Color(220, 255, 220));
        updateElectionStatus();
        header.add(electionStatusLabel, BorderLayout.EAST);
        
        return header;
    }

    private void updateElectionStatus() {
        if (ElectionScheduler.isElectionActive()) {
            electionStatusLabel.setText("✅ Election: ACTIVE");
            electionStatusLabel.setForeground(new Color(220, 255, 220));
        } else {
            electionStatusLabel.setText("❌ Election: INACTIVE");
            electionStatusLabel.setForeground(new Color(255, 220, 220));
        }
    }

    private JPanel createContentPanel() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBackground(Theme.BACKGROUND_WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Time remaining display
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        timePanel.setBackground(Theme.BACKGROUND_WHITE);
        
        timeRemainingLabel = new JLabel("⏰ Calculating time remaining...");
        timeRemainingLabel.setFont(Theme.BODY_BOLD_FONT);
        timeRemainingLabel.setForeground(Theme.TEXT_DARK);
        timePanel.add(timeRemainingLabel);
        
        content.add(timePanel, BorderLayout.NORTH);
        
        // Separator
        content.add(new JSeparator(), BorderLayout.CENTER);
        
        // Nominee selection panel
        content.add(createNomineeSelectionPanel(), BorderLayout.SOUTH);
        
        return content;
    }

    private JPanel createNomineeSelectionPanel() {
        JPanel nomineePanel = new JPanel(new GridBagLayout());
        nomineePanel.setBackground(Theme.BACKGROUND_WHITE);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Title
        JLabel titleLabel = new JLabel("Select Your Preferred Candidate");
        titleLabel.setFont(Theme.SUBTITLE_FONT);
        titleLabel.setForeground(Theme.PRIMARY_BLUE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        nomineePanel.add(titleLabel, gbc);
        
        // Load nominees from ElectionData
        String[] nomineeStrings = ElectionData.getAllNominees();
        
        if (nomineeStrings.length == 0) {
            gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
            JLabel noNomineesLabel = new JLabel(
                "<html><center><font color='red'>No candidates are available for voting.</font><br>" +
                "Please contact the election administrator.</center></html>",
                SwingConstants.CENTER);
            noNomineesLabel.setFont(Theme.BODY_FONT);
            nomineePanel.add(noNomineesLabel, gbc);
        } else {
            // Nominee selection label
            gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
            JLabel selectLabel = new JLabel("Available Candidates:");
            selectLabel.setFont(Theme.BODY_BOLD_FONT);
            selectLabel.setForeground(Theme.TEXT_DARK);
            nomineePanel.add(selectLabel, gbc);
            
            // Create dropdown with styled items
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            for (String nomineeStr : nomineeStrings) {
                String[] parts = nomineeStr.split(":");
                if (parts.length >= 3) {
                    String displayText = String.format("%s - %s (%s)", 
                        parts[0], parts[1], parts[2]);
                    model.addElement(displayText);
                }
            }
            
            JComboBox<String> nomineeComboBox = new JComboBox<>(model);
            styleComboBox(nomineeComboBox);
            gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
            nomineePanel.add(nomineeComboBox, gbc);
            
            // Vote button
            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
            JButton voteButton = new JButton("Cast Your Vote");
            styleVoteButton(voteButton);
            voteButton.addActionListener(e -> castVote(nomineeComboBox));
            nomineePanel.add(voteButton, gbc);
            
            // Instruction
            gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
            JLabel instructionLabel = new JLabel(
                "<html><center><small><i>Review your selection carefully. You cannot change your vote after submission.</i></small></center></html>",
                SwingConstants.CENTER);
            instructionLabel.setFont(Theme.SMALL_FONT);
            instructionLabel.setForeground(Theme.TEXT_LIGHT);
            nomineePanel.add(instructionLabel, gbc);
        }
        
        return nomineePanel;
    }

    private void styleComboBox(JComboBox<String> comboBox) {
        comboBox.setFont(Theme.BODY_FONT);
        comboBox.setBackground(Color.WHITE);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER_GRAY, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        comboBox.setMaximumRowCount(8);
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                return label;
            }
        });
    }

    private void styleVoteButton(JButton button) {
        button.setBackground(Theme.SUCCESS_GREEN);
        button.setForeground(Color.WHITE);
        button.setFont(Theme.BUTTON_FONT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.SUCCESS_GREEN.darker(), 2),
            BorderFactory.createEmptyBorder(10, 30, 10, 30)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(Theme.SUCCESS_GREEN.brighter());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Theme.SUCCESS_GREEN);
            }
        });
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.BACKGROUND_LIGHT);
        footer.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, Theme.BORDER_GRAY),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        
        // Back button
        JButton backButton = new JButton("Cancel & Return to Login");
        styleBackButton(backButton);
        backButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel voting?\nYour vote will not be saved.",
                "Cancel Voting", JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                dispose();
                new LoginFrame().setVisible(true);
            }
        });
        
        footer.add(backButton, BorderLayout.WEST);
        
        // Current time display
        JLabel timeLabel = new JLabel(new SimpleDateFormat("HH:mm:ss").format(new Date()));
        timeLabel.setFont(Theme.SMALL_FONT);
        timeLabel.setForeground(Theme.TEXT_LIGHT);
        
        Timer timeUpdateTimer = new Timer(1000, e -> {
            timeLabel.setText(new SimpleDateFormat("HH:mm:ss").format(new Date()));
        });
        timeUpdateTimer.start();
        
        footer.add(timeLabel, BorderLayout.EAST);
        
        return footer;
    }

    private void styleBackButton(JButton button) {
        button.setBackground(Theme.ERROR_RED);
        button.setForeground(Color.WHITE);
        button.setFont(Theme.BODY_FONT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.ERROR_RED.darker(), 1),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(Theme.ERROR_RED.brighter());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Theme.ERROR_RED);
            }
        });
    }

    private void startCountdownTimer() {
        countdownTimer = new Timer(1000, e -> updateCountdown());
        countdownTimer.start();
        updateCountdown(); // Initial update
    }

    private void updateCountdown() {
        Date endTime = ElectionScheduler.getElectionEndTime();
        if (endTime != null) {
            long currentTime = System.currentTimeMillis();
            long remainingTime = endTime.getTime() - currentTime;
            
            if (remainingTime > 0) {
                long hours = remainingTime / (1000 * 60 * 60);
                long minutes = (remainingTime % (1000 * 60 * 60)) / (1000 * 60);
                long seconds = (remainingTime % (1000 * 60)) / 1000;
                
                String timeText = String.format("⏰ Time remaining: %02d:%02d:%02d", hours, minutes, seconds);
                timeRemainingLabel.setText(timeText);
                
                // Color coding based on time remaining
                if (hours < 1) {
                    timeRemainingLabel.setForeground(Theme.ERROR_RED);
                } else if (hours < 6) {
                    timeRemainingLabel.setForeground(Theme.WARNING_ORANGE);
                } else {
                    timeRemainingLabel.setForeground(Theme.SUCCESS_GREEN);
                }
            } else {
                timeRemainingLabel.setText("⏰ Election has ended");
                timeRemainingLabel.setForeground(Theme.ERROR_RED);
                
                // Disable voting if election has ended
                if (ElectionScheduler.isElectionActive()) {
                    updateElectionStatus();
                    
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                            "⏰ The election has now ended.\nVoting is no longer allowed.",
                            "Election Ended", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                        new LoginFrame().setVisible(true);
                    });
                }
            }
        } else {
            timeRemainingLabel.setText("⏰ Election schedule not set");
            timeRemainingLabel.setForeground(Theme.TEXT_LIGHT);
        }
        
        updateElectionStatus();
    }

    private void castVote(JComboBox<String> nomineeComboBox) {
        // Final validation before casting vote
        if (!ElectionScheduler.isVotingAllowed()) {
            JOptionPane.showMessageDialog(this, 
                "❌ Voting is no longer allowed!\n\n" + ElectionScheduler.getElectionStatus(),
                "Voting Closed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (ElectionData.hasVoted(voterId)) {
            JOptionPane.showMessageDialog(this,
                "⚠️ You have already cast your vote!\n" +
                "Each voter can only vote once per election.",
                "Already Voted", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String selectedNomineeDisplay = (String) nomineeComboBox.getSelectedItem();
        if (selectedNomineeDisplay == null || selectedNomineeDisplay.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please select a candidate before voting.",
                "No Candidate Selected",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Extract nominee ID from display text
        String nomineeId = selectedNomineeDisplay.split(" - ")[0];
        
        // Get nominee details for confirmation
        String nomineeName = "Unknown";
        String partyName = "Unknown";
        String[] nominees = ElectionData.getAllNominees();
        
        for (String nomineeStr : nominees) {
            String[] parts = nomineeStr.split(":");
            if (parts.length >= 3 && parts[0].equals(nomineeId)) {
                nomineeName = parts[1];
                partyName = parts[2];
                break;
            }
        }
        
        // Show confirmation dialog
        int confirm = JOptionPane.showConfirmDialog(this,
            "<html><b>Confirm Your Vote</b><br><br>" +
            "You are about to vote for:<br>" +
            "<font color='blue'><b>" + nomineeName + "</b></font><br>" +
            "Party: " + partyName + "<br>" +
            "Voter ID: " + voterId + "<br><br>" +
            "<font color='red'><b>⚠️ This action cannot be undone!</b></font><br>" +
            "Once submitted, you cannot change your vote.<br><br>" +
            "Are you sure you want to proceed?</html>",
            "Confirm Vote",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            // Cast the vote using ElectionData
            if (ElectionData.castVote(voterId, nomineeId)) {
                // Success - show confirmation
                JOptionPane.showMessageDialog(this,
                    "<html><b>✅ Vote Cast Successfully!</b><br><br>" +
                    "Thank you for participating in the election.<br>" +
                    "Your vote for <b>" + nomineeName + "</b> has been recorded.<br><br>" +
                    "Voter ID: " + voterId + "<br>" +
                    "Timestamp: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "</html>",
                    "Vote Submitted",
                    JOptionPane.INFORMATION_MESSAGE);
                
                // Log the vote
                System.out.println("Vote recorded - Voter: " + voterId + ", Nominee: " + nomineeId);
                
                // Return to login
                dispose();
                new LoginFrame().setVisible(true);
                
            } else {
                JOptionPane.showMessageDialog(this,
                    "❌ Failed to cast vote. Please try again.\n" +
                    "If the problem persists, contact the election administrator.",
                    "Voting Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void dispose() {
        // Clean up timers
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        super.dispose();
    }
}