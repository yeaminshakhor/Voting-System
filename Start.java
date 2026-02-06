package Framesg;

import javax.swing.*;

/**
 * Entry point for the Online Voting System, launching the landing page.
 */
public class Start {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("Starting landing page...");
            landing landing = new landing();
            landing.setVisible(true);
        });
    }
}