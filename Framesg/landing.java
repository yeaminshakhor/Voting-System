package Framesg;

import java.awt.*;
import javax.swing.*;

/**
 * Landing page for the Online Voting System, displaying a welcome message and logo.
 * Navigates to RegisterFrame when "Let's Go" is clicked.
 */
public class landing extends JFrame {

    public landing() {
        // Frame setup
        setTitle("Online Voting System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null); // Center
        setLayout(new BorderLayout());

        // Title
        JLabel title = new JLabel("Welcome to Online Voting System", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        // Logo placeholder panel
        JPanel logoPanel = new JPanel();
        logoPanel.setPreferredSize(new Dimension(150, 150));
        logoPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));

        try {
            ImageIcon icon = new ImageIcon("officiallogo.jpeg");
            Image img = icon.getImage().getScaledInstance(800, 380, Image.SCALE_SMOOTH);
            icon = new ImageIcon(img);
            JLabel logo = new JLabel(icon);
            logo.setHorizontalAlignment(SwingConstants.CENTER);
            logo.setVerticalAlignment(SwingConstants.CENTER);
            logoPanel.add(logo);
        } catch (Exception e) {
            JLabel logo = new JLabel("Logo Not Found", SwingConstants.CENTER);
            logoPanel.add(logo);
        }

        // Button
        JButton button = new JButton("Let's Go");
        button.setFont(new Font("Serif", Font.BOLD, 16));
        button.setBackground(new Color(20, 40, 80));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.addActionListener(e -> {
            this.setVisible(false);
            new RegisterFrame().setVisible(true);
            dispose();
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(button);

        // Add to frame
        add(title, BorderLayout.NORTH);
        add(logoPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
}