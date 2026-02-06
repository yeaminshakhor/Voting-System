import Framesg.AdminLogin;
import Framesg.VoterLogin;
import Framesg.VoterRegistration;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Main entry point for the election management system.
 */
public class Main {
    private static final Color NAVY_BLUE = new Color(25, 25, 112);
    private static final Color DODGER_BLUE = new Color(30, 144, 255);
    private static final Color LIGHT_BLUE = new Color(173, 216, 230);
    private static final int WINDOW_WIDTH = 400;
    private static final int WINDOW_HEIGHT = 300;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Election Management System");
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(NAVY_BLUE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel titleLabel = new JLabel("Election Management System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(LIGHT_BLUE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        JButton adminLoginButton = new JButton("Admin Login");
        adminLoginButton.setFont(new Font("Arial", Font.BOLD, 14));
        adminLoginButton.setBackground(DODGER_BLUE);
        adminLoginButton.setForeground(Color.WHITE);
        adminLoginButton.setFocusPainted(false);
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(adminLoginButton, gbc);

        JButton voterLoginButton = new JButton("Voter Login");
        voterLoginButton.setFont(new Font("Arial", Font.BOLD, 14));
        voterLoginButton.setBackground(DODGER_BLUE);
        voterLoginButton.setForeground(Color.WHITE);
        voterLoginButton.setFocusPainted(false);
        gbc.gridx = 1;
        panel.add(voterLoginButton, gbc);

        JButton voterRegisterButton = new JButton("Voter Registration");
        voterRegisterButton.setFont(new Font("Arial", Font.BOLD, 14));
        voterRegisterButton.setBackground(DODGER_BLUE);
        voterRegisterButton.setForeground(Color.WHITE);
        voterRegisterButton.setFocusPainted(false);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(voterRegisterButton, gbc);

        frame.add(panel);
        frame.setVisible(true);

        adminLoginButton.addActionListener(e -> {
            frame.setVisible(false);
            new AdminLogin(frame).setVisible(true);
        });

        voterLoginButton.addActionListener(e -> {
            frame.setVisible(false);
            new VoterLogin(frame).setVisible(true);
        });

        voterRegisterButton.addActionListener(e -> {
            frame.setVisible(false);
            new VoterRegistration(frame).setVisible(true);
        });
    }
}