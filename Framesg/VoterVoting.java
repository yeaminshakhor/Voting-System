package Framesg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import Data.ElectionData;

public class VoterVoting extends JFrame implements ActionListener {
    private String voterId;
    private JFrame parentFrame;
    private JComboBox<String> nomineeComboBox;

    public VoterVoting(String voterId, JFrame parentFrame) {
        this.voterId = voterId;
        this.parentFrame = parentFrame;
        
        setTitle("Cast Your Vote");
        setSize(500, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        initUI();
        setVisible(true);
    }

    private void initUI() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("🗳️ Cast Your Vote", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel votePanel = new JPanel(new GridLayout(3, 1, 10, 10));
        
        JLabel instructionLabel = new JLabel("Select your preferred candidate:");
        instructionLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        // Load nominees
        String[] nomineeStrings = ElectionData.getAllNominees();
        String[] nomineeOptions = new String[nomineeStrings.length];
        for (int i = 0; i < nomineeStrings.length; i++) {
            String[] parts = nomineeStrings[i].split(":");
            if (parts.length >= 3) {
                nomineeOptions[i] = parts[1] + " (" + parts[2] + ")";
            } else {
                nomineeOptions[i] = "Unknown";
            }
        }
        
        nomineeComboBox = new JComboBox<>(nomineeOptions);
        nomineeComboBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        JButton voteButton = new JButton("Submit Vote");
        voteButton.setBackground(Color.GREEN);
        voteButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        voteButton.addActionListener(this);
        
        votePanel.add(instructionLabel);
        votePanel.add(nomineeComboBox);
        votePanel.add(voteButton);
        
        panel.add(votePanel, BorderLayout.CENTER);

        JButton backButton = new JButton("Back to Login");
        backButton.addActionListener(e -> {
            setVisible(false);
            parentFrame.setVisible(true);
        });
        panel.add(backButton, BorderLayout.SOUTH);

        add(panel);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String selectedNomineeDisplay = (String) nomineeComboBox.getSelectedItem();
        if (selectedNomineeDisplay != null) {
            // Find the actual nominee ID
            String nomineeId = "";
            String[] nominees = ElectionData.getAllNominees();
            for (String nominee : nominees) {
                String[] parts = nominee.split(":");
                if (parts.length >= 3) {
                    String displayName = parts[1] + " (" + parts[2] + ")";
                    if (displayName.equals(selectedNomineeDisplay)) {
                        nomineeId = parts[0];
                        break;
                    }
                }
            }
            
            int confirm = JOptionPane.showConfirmDialog(this,
                "Confirm vote for: " + selectedNomineeDisplay + "?",
                "Confirm Vote", JOptionPane.YES_NO_OPTION);
                
            if (confirm == JOptionPane.YES_OPTION) {
                if (ElectionData.castVote(voterId, nomineeId)) {
                    JOptionPane.showMessageDialog(this, "✅ Vote cast successfully! Thank you for voting.");
                    setVisible(false);
                    parentFrame.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Failed to cast vote. Please try again.");
                }
            }
        }
    }
}