package Framesg;

import Data.ElectionData;
import Data.SqlElectionDataManager;
import Data.ElectionScheduler;
import Utils.SecurityUtils;
import Utils.Theme;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;

/**
 * Enhanced Voter Profile Management
 * - Change password with security checks
 * - View detailed voting history
 * - View election schedule
 * - Profile statistics
 */
public class VoterProfile extends JFrame {
    private static final long serialVersionUID = 1L;
    private String voterId;
    private String voterName;
    private String voterEmail;
    private String imagePath;
    private String registrationStatus;
    private boolean hasVoted;
    
    // UI Components
    private JLabel nameLabel;
    private JLabel emailLabel;
    private JLabel imageLabel;
    private JLabel electionStatusLabel;
    
    public VoterProfile(String voterId) {
        this.voterId = voterId;
        loadVoterData();
        setupWindow();
        initUI();
        setVisible(true);
    }
    
    private void setupWindow() {
        setTitle("My Voter Profile - ID: " + voterId);
        setSize(700, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Theme.BACKGROUND_WHITE);
        
        // Set application icon if available
        try {
            // Uncomment if you have an icon
            // setIconImage(Toolkit.getDefaultToolkit().getImage("icon.png"));
        } catch (Exception e) {
            // Ignore
        }
    }
    
    private void loadVoterData() {
        // Load voter data from SQL database instead of plain text files
        String voterInfo = SqlElectionDataManager.getVoterInfo(voterId);
        
        if (voterInfo != null) {
            // Format: voterId:name:email:imagePath:isRegistered:hasVoted
            String[] parts = voterInfo.split(":", -1);
            voterName = parts.length > 1 ? parts[1] : "Unknown";
            voterEmail = parts.length > 2 ? parts[2] : "";
            imagePath = parts.length > 3 ? parts[3] : null;
            registrationStatus = parts.length > 4 && "1".equals(parts[4]) ? "Registered" : "Unregistered";
            hasVoted = parts.length > 5 && "1".equals(parts[5]);
        } else {
            voterName = "Unknown";
            voterEmail = "";
            imagePath = null;
            registrationStatus = "Unknown";
            hasVoted = false;
        }
    }
    
    private void initUI() {
        // Create main panel with border layout
        setLayout(new BorderLayout(0, 0));
        
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
        
        // Title and icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setBackground(Theme.PRIMARY_BLUE);
        
        JLabel iconLabel = new JLabel("👤");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        
        JLabel title = new JLabel("My Voter Profile");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Color.WHITE);
        
        JLabel idLabel = new JLabel("ID: " + voterId);
        idLabel.setFont(Theme.SMALL_FONT);
        idLabel.setForeground(new Color(220, 220, 255));
        
        titlePanel.add(iconLabel);
        titlePanel.add(title);
        titlePanel.add(Box.createHorizontalStrut(20));
        titlePanel.add(idLabel);
        
        header.add(titlePanel, BorderLayout.WEST);
        
        // Close button
        JButton closeBtn = createStyledButton("Close", Theme.ERROR_RED);
        closeBtn.addActionListener(e -> dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setBackground(Theme.PRIMARY_BLUE);
        buttonPanel.add(closeBtn);
        
        header.add(buttonPanel, BorderLayout.EAST);
        
        return header;
    }
    
    private JPanel createContentPanel() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Theme.BACKGROUND_WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridx = 0;
        
        // Section 0: Profile Image
        gbc.gridy = 0;
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    BufferedImage img = ImageIO.read(imageFile);
                    if (img != null) {
                        // Scale image to 150x150
                        BufferedImage scaledImg = new BufferedImage(150, 150, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2d = scaledImg.createGraphics();
                        g2d.drawImage(img, 0, 0, 150, 150, null);
                        g2d.dispose();
                        
                        imageLabel = new JLabel(new ImageIcon(scaledImg));
                        JPanel imagePanel = new JPanel();
                        imagePanel.setBackground(Theme.BACKGROUND_WHITE);
                        imagePanel.add(imageLabel);
                        content.add(imagePanel, gbc);
                        gbc.gridy++;
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ Could not load voter image: " + e.getMessage());
            }
        }
        
        // Section 1: Personal Information
        gbc.gridy++;
        content.add(createSectionTitle("Personal Information"), gbc);
        
        gbc.gridy++;
        content.add(createInfoRow("Voter ID:", voterId), gbc);
        
        gbc.gridy++;
        nameLabel = createInfoValue(voterName);
        JPanel nameRow = new JPanel(new BorderLayout(10, 0));
        nameRow.setBackground(Theme.BACKGROUND_WHITE);
        nameRow.add(createInfoLabel("Full Name:"), BorderLayout.WEST);
        nameRow.add(nameLabel, BorderLayout.CENTER);
        content.add(nameRow, gbc);
        
        // Email (from SQL database)
        gbc.gridy++;
        emailLabel = createInfoValue(voterEmail != null && !voterEmail.isEmpty() ? voterEmail : "Not provided");
        JPanel emailRow = new JPanel(new BorderLayout(10, 0));
        emailRow.setBackground(Theme.BACKGROUND_WHITE);
        emailRow.add(createInfoLabel("Email:"), BorderLayout.WEST);
        emailRow.add(emailLabel, BorderLayout.CENTER);
        content.add(emailRow, gbc);
        
        // Registration Status
        gbc.gridy++;
        content.add(createInfoRow("Registration Status:", registrationStatus), gbc);
        
        // Section 2: Voting Status
        gbc.gridy++;
        gbc.insets = new Insets(20, 8, 8, 8);
        content.add(createSectionTitle("Voting Status"), gbc);
        
        gbc.gridy++;
        gbc.insets = new Insets(8, 8, 8, 8);
        String voteStatus = hasVoted ? "✅ You have already voted" : "❌ You have not voted yet";
        content.add(createInfoRow("Current Status:", voteStatus), gbc);
        
        // Section 3: Election Information
        gbc.gridy++;
        gbc.insets = new Insets(20, 8, 8, 8);
        content.add(createSectionTitle("Election Information"), gbc);
        
        gbc.gridy++;
        gbc.insets = new Insets(8, 8, 8, 8);
        
        String electionStatus = ElectionScheduler.isElectionActive() ? 
            "✅ Election is currently active" : "❌ Election is not active";
        electionStatusLabel = new JLabel(electionStatus);
        electionStatusLabel.setFont(Theme.BODY_FONT);
        electionStatusLabel.setForeground(ElectionScheduler.isElectionActive() ? 
            Theme.SUCCESS_GREEN : Theme.ERROR_RED);
        
        JPanel electionPanel = new JPanel(new BorderLayout(10, 0));
        electionPanel.setBackground(Theme.BACKGROUND_WHITE);
        electionPanel.add(createInfoLabel("Current Status:"), BorderLayout.WEST);
        electionPanel.add(electionStatusLabel, BorderLayout.CENTER);
        content.add(electionPanel, gbc);
        
        // Election schedule button
        gbc.gridy++;
        JButton scheduleBtn = createStyledButton("View Election Schedule", Theme.INFO_CYAN);
        scheduleBtn.addActionListener(e -> showElectionSchedule());
        content.add(scheduleBtn, gbc);
        
        // Add scrollpane for long content
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Theme.BACKGROUND_WHITE);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        
        return wrapper;
    }
    
    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        footer.setBackground(Theme.BACKGROUND_LIGHT);
        footer.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, Theme.BORDER_GRAY),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        
        // Action buttons
        JButton changePassBtn = createStyledButton("Change Password", Theme.PRIMARY_BLUE);
        changePassBtn.addActionListener(e -> openChangePasswordDialog());
        changePassBtn.setToolTipText("Change your login password");
        
        JButton historyBtn = createStyledButton("View Voting History", Theme.SUCCESS_GREEN);
        historyBtn.addActionListener(e -> showVotingHistory());
        historyBtn.setToolTipText("View your voting history");
        
        JButton updateProfileBtn = createStyledButton("Update Profile", Theme.WARNING_ORANGE);
        updateProfileBtn.addActionListener(e -> updateProfile());
        updateProfileBtn.setToolTipText("Update your profile information");
        
        footer.add(changePassBtn);
        footer.add(historyBtn);
        footer.add(updateProfileBtn);
        
        return footer;
    }
    
    private JLabel createSectionTitle(String title) {
        JLabel label = new JLabel(title);
        label.setFont(Theme.SUBTITLE_FONT);
        label.setForeground(Theme.NAVY_BLUE);
        label.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.PRIMARY_BLUE));
        return label;
    }
    
    private JPanel createInfoRow(String labelText, String valueText) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(Theme.BACKGROUND_WHITE);
        
        JLabel label = createInfoLabel(labelText);
        JLabel value = createInfoValue(valueText);
        
        panel.add(label, BorderLayout.WEST);
        panel.add(value, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.BODY_BOLD_FONT);
        label.setForeground(Theme.TEXT_DARK);
        label.setPreferredSize(new Dimension(150, 25));
        return label;
    }
    
    private JLabel createInfoValue(String value) {
        JLabel label = new JLabel(value != null ? value : "N/A");
        label.setFont(Theme.BODY_FONT);
        label.setForeground(Theme.TEXT_MEDIUM);
        return label;
    }
    
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(Theme.BUTTON_FONT);
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
        
        return button;
    }
    
    private void openChangePasswordDialog() {
        JDialog dialog = new JDialog(this, "Change Password", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(Theme.BACKGROUND_WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JLabel title = new JLabel("Change Your Password", SwingConstants.CENTER);
        title.setFont(Theme.SUBTITLE_FONT);
        title.setForeground(Theme.PRIMARY_BLUE);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(title, gbc);
        
        JPasswordField currentPass = new JPasswordField(20);
        JPasswordField newPass = new JPasswordField(20);
        JPasswordField confirmPass = new JPasswordField(20);
        
        // Current password
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        mainPanel.add(createInfoLabel("Current Password:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(currentPass, gbc);
        
        // New password
        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(createInfoLabel("New Password:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(newPass, gbc);
        
        // Confirm password
        gbc.gridx = 0; gbc.gridy = 3;
        mainPanel.add(createInfoLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(confirmPass, gbc);
        
        // Password requirements
        JLabel requirements = new JLabel("<html><small>• At least 6 characters<br>• Include letters and numbers</small></html>");
        requirements.setFont(Theme.SMALL_FONT);
        requirements.setForeground(Theme.TEXT_LIGHT);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        mainPanel.add(requirements, gbc);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(Theme.BACKGROUND_WHITE);
        
        JButton changeBtn = createStyledButton("Change Password", Theme.PRIMARY_BLUE);
        changeBtn.addActionListener(e -> {
            String current = new String(currentPass.getPassword());
            String newP = new String(newPass.getPassword());
            String confirm = new String(confirmPass.getPassword());
            
            if (validateAndChangePassword(current, newP, confirm)) {
                dialog.dispose();
            }
        });
        
        JButton cancelBtn = createStyledButton("Cancel", Theme.ERROR_RED);
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(changeBtn);
        buttonPanel.add(cancelBtn);
        
        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    private boolean validateAndChangePassword(String current, String newP, String confirm) {
        // Validate inputs
        if (current.isEmpty() || newP.isEmpty() || confirm.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields!", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        // Check if current password is correct
        if (!ElectionData.validateVoter(voterId, current)) {
            JOptionPane.showMessageDialog(this, "Current password is incorrect!", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        // Validate new password strength
        if (newP.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters long!", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (newP.equals(current)) {
            JOptionPane.showMessageDialog(this, "New password must be different from current password!", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (!newP.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match!", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        // Additional security checks
        if (!SecurityUtils.isPasswordStrong(newP)) {
            JOptionPane.showMessageDialog(this, 
                "Password must be at least 6 characters long.", 
                "Weak Password", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        // Update password
        if (ElectionData.updateVoterPassword(voterId, newP)) {
            JOptionPane.showMessageDialog(this, "Password changed successfully!", 
                "Success", JOptionPane.INFORMATION_MESSAGE);
            
            // Log the password change
            System.out.println("Password changed for voter: " + voterId);
            
            return true;
        } else {
            JOptionPane.showMessageDialog(this, "Failed to change password!", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    private void showVotingHistory() {
        JDialog historyDialog = new JDialog(this, "Voting History", true);
        historyDialog.setSize(500, 400);
        historyDialog.setLocationRelativeTo(this);
        historyDialog.setLayout(new BorderLayout());
        
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setBackground(Theme.BACKGROUND_WHITE);
        textArea.setForeground(Theme.TEXT_DARK);
        
        StringBuilder history = new StringBuilder();
        history.append("         VOTING HISTORY\n");
        history.append("         ==============\n\n");
        history.append("Voter ID: ").append(voterId).append("\n");
        history.append("Name: ").append(voterName).append("\n");
        history.append("Date: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        history.append("-".repeat(50)).append("\n\n");
        
        if (!this.hasVoted) {
            history.append("No voting history found.\n");
            history.append("You have not participated in any election yet.\n");
        } else {
            // Get voting history from database
            String[] votes = ElectionData.getVoterVotes(voterId);
            if (votes != null && votes.length > 0) {
                history.append("Total Votes: ").append(votes.length).append("\n\n");
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                int count = 1;
                
                for (String vote : votes) {
                    String[] parts = vote.split(":");
                    if (parts.length >= 3) {
                        String nomineeId = parts[1];
                        long timestamp = Long.parseLong(parts[2]);
                        Date voteTime = new Date(timestamp);
                        
                        String nomineeName = ElectionData.getNomineeName(nomineeId);
                        
                        history.append(String.format("%2d. %s\n", count++, nomineeName));
                        history.append("    Election: ").append(parts.length > 3 ? parts[3] : "General Election").append("\n");
                        history.append("    Time: ").append(sdf.format(voteTime)).append("\n");
                        history.append("    Nominee ID: ").append(nomineeId).append("\n");
                        history.append("-".repeat(50)).append("\n");
                    }
                }
            } else {
                history.append("Voting records could not be retrieved.\n");
            }
        }
        
        textArea.setText(history.toString());
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JButton closeBtn = createStyledButton("Close", Theme.PRIMARY_BLUE);
        closeBtn.addActionListener(e -> historyDialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Theme.BACKGROUND_WHITE);
        buttonPanel.add(closeBtn);
        
        historyDialog.add(scrollPane, BorderLayout.CENTER);
        historyDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        historyDialog.setVisible(true);
    }
    
    private void showElectionSchedule() {
        String schedule = ElectionScheduler.getElectionStatus();
        
        JTextArea textArea = new JTextArea(schedule);
        textArea.setEditable(false);
        textArea.setFont(Theme.BODY_FONT);
        textArea.setBackground(Theme.BACKGROUND_WHITE);
        textArea.setForeground(Theme.TEXT_DARK);
        textArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        
        JOptionPane.showMessageDialog(this, scrollPane, "Election Schedule", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void updateProfile() {
        // Create a panel for profile update with image upload option
        JPanel updatePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        // Name field
        JLabel nameLabel1 = new JLabel("Full Name:");
        JTextField nameField = new JTextField(voterName, 20);
        updatePanel.add(nameLabel1, gbc);
        gbc.gridx = 1;
        updatePanel.add(nameField, gbc);
        
        // Email field
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel emailLabel1 = new JLabel("Email:");
        JTextField emailField = new JTextField(voterEmail != null ? voterEmail : "", 20);
        updatePanel.add(emailLabel1, gbc);
        gbc.gridx = 1;
        updatePanel.add(emailField, gbc);
        
        // Image upload button
        gbc.gridx = 0;
        gbc.gridy = 2;
        JButton imageBtn = new JButton("Upload Image");
        final File[] selectedImage = {null};
        imageBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files", "jpg", "jpeg", "png", "gif"));
            
            if (fileChooser.showOpenDialog(VoterProfile.this) == JFileChooser.APPROVE_OPTION) {
                selectedImage[0] = fileChooser.getSelectedFile();
                imageBtn.setText("✓ Image Selected: " + selectedImage[0].getName());
                imageBtn.setBackground(Theme.SUCCESS_GREEN);
                imageBtn.setForeground(Color.WHITE);
            }
        });
        updatePanel.add(imageBtn, gbc);
        
        // Current image preview
        gbc.gridx = 1;
        if (imagePath != null && !imagePath.isEmpty()) {
            JLabel currentImageLabel = new JLabel("Current: " + imagePath);
            currentImageLabel.setFont(Theme.SMALL_FONT);
            updatePanel.add(currentImageLabel, gbc);
        } else {
            JLabel noImageLabel = new JLabel("No image set yet");
            noImageLabel.setFont(Theme.SMALL_FONT);
            updatePanel.add(noImageLabel, gbc);
        }
        
        int result = JOptionPane.showConfirmDialog(this, updatePanel, 
            "Update Profile", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String newName = nameField.getText().trim();
            String newEmail = emailField.getText().trim();
            String newImagePath = imagePath;  // Keep existing image if no new one selected
            
            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name cannot be empty!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // If new image selected, set it as the image path
            if (selectedImage[0] != null) {
                newImagePath = selectedImage[0].getAbsolutePath();
            }
            
            // Update voter information in SQL database
            if (SqlElectionDataManager.updateVoterProfile(voterId, newName, newEmail, newImagePath)) {
                voterName = newName;
                voterEmail = newEmail;
                imagePath = newImagePath;
                
                if (nameLabel != null) {
                    nameLabel.setText(voterName);
                }
                if (emailLabel != null) {
                    emailLabel.setText(!voterEmail.isEmpty() ? voterEmail : "Not provided");
                }
                
                JOptionPane.showMessageDialog(this, "Profile updated successfully!", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                
                // Refresh the display
                getContentPane().removeAll();
                initUI();
                revalidate();
                repaint();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update profile!", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    // Main method for testing
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new VoterProfile("VOTER001");
        });
    }
}