package Framesg;

import Data.ElectionData;
import Data.ElectionScheduler;
import Utils.Theme;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Date;

/**
 * Enhanced Voter Voting Interface with Election Schedule Validation
 */
public class VoterVoting extends JFrame implements ActionListener {
    private String voterId;
    private JFrame parentFrame;
    private JComboBox<String> nomineeComboBox;
    private JLabel electionStatusLabel;
    private JLabel timeRemainingLabel;
    private Timer countdownTimer;
    
    public VoterVoting(String voterId, JFrame parentFrame) {
        this.voterId = voterId;
        this.parentFrame = parentFrame;
        
        setupWindow();
        checkVotingEligibility();
        initUI();
        startCountdownTimer();
        setVisible(true);
    }

    private void setupWindow() {
        setTitle("Cast Your Vote - Voter: " + voterId);
        setSize(600, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(Theme.BACKGROUND_WHITE);
    }

    private void checkVotingEligibility() {
        // Check if election is active
        if (!ElectionScheduler.isVotingAllowed()) {
            String status = ElectionScheduler.getElectionStatus();
            JOptionPane.showMessageDialog(this, 
                "❌ Voting is not allowed at this time!\n\n" + status +
                "\n\nPlease try again during the election period.",
                "Voting Not Available", JOptionPane.WARNING_MESSAGE);
            
            // Optionally close the window or show disabled interface
            setVisible(false);
            parentFrame.setVisible(true);
            return;
        }
        
        // Check if voter has already voted
        if (ElectionData.hasVoted(voterId)) {
            JOptionPane.showMessageDialog(this,
                "⚠️ You have already cast your vote!\n\n" +
                "Each voter can only vote once per election.\n" +
                "If you believe this is an error, please contact the election administrator.",
                "Already Voted", JOptionPane.INFORMATION_MESSAGE);
            
            // Show voting history
            showVotingHistory();
            setVisible(false);
            parentFrame.setVisible(true);
        }
    }

    private void showVotingHistory() {
        String history = ElectionData.getVoterHistory(voterId);
        if (history != null && !history.isEmpty()) {
            JTextArea textArea = new JTextArea(history);
            textArea.setEditable(false);
            textArea.setFont(Theme.MONOSPACE_FONT);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(400, 200));
            
            JOptionPane.showMessageDialog(this, scrollPane, 
                "Your Voting History", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        
        // Header Panel
        add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Main Voting Panel
        add(createVotingPanel(), BorderLayout.CENTER);
        
        // Footer Panel
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY_BLUE);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        JLabel title = new JLabel("🗳️ Cast Your Vote");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        
        // Election status indicator
        electionStatusLabel = new JLabel("Election: Active");
        electionStatusLabel.setFont(Theme.SMALL_FONT);
        electionStatusLabel.setForeground(new Color(220, 255, 220));
        updateElectionStatus();
        // Right-side panel: election status + profile access
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(Theme.PRIMARY_BLUE);

        JButton profileBtn = new JButton("My Profile");
        styleButton(profileBtn, Theme.INFO_CYAN);
        profileBtn.setFont(Theme.SMALL_FONT);
        profileBtn.setToolTipText("Open your voter profile and settings");
        profileBtn.addActionListener(e -> {
            // Open profile in a separate window
            new VoterProfile(voterId);
        });

        rightPanel.add(electionStatusLabel);
        rightPanel.add(profileBtn);
        header.add(rightPanel, BorderLayout.EAST);
        
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

    private JPanel createVotingPanel() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(Theme.BACKGROUND_WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Title
        JLabel titleLabel = new JLabel("Select Your Preferred Candidate");
        titleLabel.setFont(Theme.SUBTITLE_FONT);
        titleLabel.setForeground(Theme.PRIMARY_BLUE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);
        
        // Voter ID display
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        JLabel voterIdLabel = new JLabel("Voter ID: " + voterId, SwingConstants.CENTER);
        voterIdLabel.setFont(Theme.BODY_BOLD_FONT);
        voterIdLabel.setForeground(Theme.TEXT_MEDIUM);
        mainPanel.add(voterIdLabel, gbc);
        
        // Time remaining display
        timeRemainingLabel = new JLabel("Time remaining: Calculating...", SwingConstants.CENTER);
        timeRemainingLabel.setFont(Theme.SMALL_FONT);
        timeRemainingLabel.setForeground(Theme.TEXT_LIGHT);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        mainPanel.add(timeRemainingLabel, gbc);
        
        // Separator
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        mainPanel.add(new JSeparator(), gbc);
        
        // Nominee selection label
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JLabel selectLabel = new JLabel("Available Candidates:");
        selectLabel.setFont(Theme.BODY_BOLD_FONT);
        selectLabel.setForeground(Theme.TEXT_DARK);
        mainPanel.add(selectLabel, gbc);
        
        // Load nominees
        String[] nomineeStrings = ElectionData.getAllNominees();
        if (nomineeStrings.length == 0) {
            gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
            JLabel noNomineesLabel = new JLabel("No candidates available for voting.", SwingConstants.CENTER);
            noNomineesLabel.setFont(Theme.BODY_FONT);
            noNomineesLabel.setForeground(Theme.ERROR_RED);
            mainPanel.add(noNomineesLabel, gbc);
        } else {
            // Create combo box with styled items
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            
            for (String nomineeStr : nomineeStrings) {
                String[] parts = nomineeStr.split(":");
                if (parts.length >= 3) {
                    String displayText = String.format("%s - %s (%s)", 
                        parts[0], parts[1], parts[2]);
                    model.addElement(displayText);
                }
            }
            
            nomineeComboBox = new JComboBox<>(model);
            nomineeComboBox.setFont(Theme.BODY_FONT);
            nomineeComboBox.setBackground(Color.WHITE);
            nomineeComboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_GRAY, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
            nomineeComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value,
                        int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                    label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                    return label;
                }
            });
            
            gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.BOTH;
            mainPanel.add(nomineeComboBox, gbc);
            
            // Voting instruction
            gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
            JLabel instructionLabel = new JLabel(
                "<html><small><i>Please review your selection carefully before submitting.</i></small></html>",
                SwingConstants.CENTER);
            instructionLabel.setFont(Theme.SMALL_FONT);
            instructionLabel.setForeground(Theme.TEXT_LIGHT);
            mainPanel.add(instructionLabel, gbc);
        }
        
        return mainPanel;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout(10, 10));
        footer.setBackground(Theme.BACKGROUND_LIGHT);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        // Submit vote button (centered)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setBackground(Theme.BACKGROUND_LIGHT);
        
        JButton voteButton = new JButton("Submit Vote");
        styleButton(voteButton, Theme.SUCCESS_GREEN);
        voteButton.setFont(Theme.BUTTON_FONT);
        voteButton.addActionListener(this);
        voteButton.setToolTipText("Cast your vote for the selected candidate");
        buttonPanel.add(voteButton);
        
        footer.add(buttonPanel, BorderLayout.CENTER);
        
        // Back button (right aligned)
        JButton backButton = new JButton("Cancel & Return");
        styleButton(backButton, Theme.ERROR_RED);
        backButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel voting?\nYour vote will not be saved.",
                "Cancel Voting", JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                setVisible(false);
                parentFrame.setVisible(true);
            }
        });
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setBackground(Theme.BACKGROUND_LIGHT);
        rightPanel.add(backButton);
        footer.add(rightPanel, BorderLayout.EAST);
        
        return footer;
    }

    private void styleButton(JButton button, Color bgColor) {
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(Theme.BODY_BOLD_FONT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgColor.darker(), 1),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.brighter());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
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
                
                timeRemainingLabel.setText(String.format(
                    "⏰ Time remaining: %02d:%02d:%02d", hours, minutes, seconds));
                
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
                    // Election just ended, update status
                    updateElectionStatus();
                    
                    // Show message and close voting window
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                            "⏰ The election has now ended.\nVoting is no longer allowed.",
                            "Election Ended", JOptionPane.INFORMATION_MESSAGE);
                        setVisible(false);
                        parentFrame.setVisible(true);
                    });
                }
            }
        } else {
            timeRemainingLabel.setText("Election end time not set");
            timeRemainingLabel.setForeground(Theme.TEXT_LIGHT);
        }
        
        updateElectionStatus();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
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
            showVotingHistory();
            return;
        }
        
        String selectedNomineeDisplay = (String) nomineeComboBox.getSelectedItem();
        if (selectedNomineeDisplay != null && !selectedNomineeDisplay.isEmpty()) {
            // Extract nominee ID from display text
            String nomineeId = selectedNomineeDisplay.split(" - ")[0];
            
            // Show confirmation dialog with candidate details
            String[] nominees = ElectionData.getAllNominees();
            String nomineeName = "Unknown";
            String partyName = "Unknown";
            
            for (String nomineeStr : nominees) {
                String[] parts = nomineeStr.split(":");
                if (parts.length >= 3 && parts[0].equals(nomineeId)) {
                    nomineeName = parts[1];
                    partyName = parts[2];
                    break;
                }
            }
            
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
                // Cast the vote
                if (ElectionData.castVote(voterId, nomineeId)) {
                    // Success - show confirmation and close
                    JOptionPane.showMessageDialog(this,
                        "<html><b>✅ Vote Cast Successfully!</b><br><br>" +
                        "Thank you for participating in the election.<br>" +
                        "Your vote for <b>" + nomineeName + "</b> has been recorded.<br><br>" +
                        "Voter ID: " + voterId + "<br>" +
                        "Timestamp: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "</html>",
                        "Vote Submitted",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    // Log the vote
                    System.out.println("Vote recorded - Voter: " + voterId + ", Nominee: " + nomineeId);
                    
                    // Close voting window
                    setVisible(false);
                    parentFrame.setVisible(true);
                    
                } else {
                    JOptionPane.showMessageDialog(this,
                        "❌ Failed to cast vote. Please try again.\n" +
                        "If the problem persists, contact the election administrator.",
                        "Voting Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this,
                "Please select a candidate before voting.",
                "No Candidate Selected",
                JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override
    public void dispose() {
        // Stop the countdown timer when window closes
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        super.dispose();
    }

    // Helper method to refresh nominee list (can be called if needed)
    public void refreshNomineeList() {
        if (nomineeComboBox != null) {
            String[] nomineeStrings = ElectionData.getAllNominees();
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            
            for (String nomineeStr : nomineeStrings) {
                String[] parts = nomineeStr.split(":");
                if (parts.length >= 3) {
                    String displayText = String.format("%s - %s (%s)", 
                        parts[0], parts[1], parts[2]);
                    model.addElement(displayText);
                }
            }
            
            nomineeComboBox.setModel(model);
            
            if (model.getSize() == 0) {
                JOptionPane.showMessageDialog(this,
                    "No candidates are currently available for voting.",
                    "No Candidates",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
}