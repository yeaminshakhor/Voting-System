package Framesg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import Data.ElectionData;
import Entities.Nominee;

/**
 * Panel for managing nominees, allowing addition and deletion of nominees.
 */
public class NomineePanel extends JPanel {
    private final DefaultListModel<Nominee> nomineeListModel;
    private final JList<Nominee> nomineeJList;
    private final JTextField idField, nameField, partyField;
    private final Color BACKGROUND_COLOR = Color.decode("#F8F9FA");
    private final Color PRIMARY_BLUE = Color.decode("#2563EB");
    private final Color TEXT_DARK = Color.decode("#1F2937");
    private final AdminDashboard dashboard;

    /**
     * Initializes the NomineePanel with an array of nominees and a reference to the dashboard.
     */
    public NomineePanel(Nominee[] nominees, AdminDashboard dashboard) {
        this.dashboard = dashboard;
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        nomineeListModel = new DefaultListModel<>();
        for (Nominee nominee : nominees) {
            if (nominee != null) {
                nomineeListModel.addElement(nominee);
            }
        }
        nomineeJList = new JList<>(nomineeListModel);
        nomineeJList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        nomineeJList.setForeground(TEXT_DARK);

        JPanel addPanel = new JPanel();
        addPanel.setBackground(BACKGROUND_COLOR);
        idField = new JTextField(6);
        nameField = new JTextField(10);
        partyField = new JTextField(10);
        JButton addButton = new JButton("Add");
        JButton deleteButton = new JButton("Delete Selected");
        JButton backButton = new JButton("Back to Dashboard");

        idField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        nameField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        partyField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        addButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        addButton.setBackground(PRIMARY_BLUE);
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        deleteButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        deleteButton.setBackground(PRIMARY_BLUE);
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFocusPainted(false);
        backButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        backButton.setBackground(Color.decode("#EF4444"));
        backButton.setForeground(Color.WHITE);
        backButton.setFocusPainted(false);

        addButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { addButton.setBackground(PRIMARY_BLUE.darker()); }
            public void mouseExited(MouseEvent e) { addButton.setBackground(PRIMARY_BLUE); }
        });
        deleteButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { deleteButton.setBackground(PRIMARY_BLUE.darker()); }
            public void mouseExited(MouseEvent e) { deleteButton.setBackground(PRIMARY_BLUE); }
        });
        backButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { backButton.setBackground(Color.decode("#EF4444").darker()); }
            public void mouseExited(MouseEvent e) { backButton.setBackground(Color.decode("#EF4444")); }
        });

        addPanel.add(new JLabel("ID:"));
        addPanel.add(idField);
        addPanel.add(new JLabel("Name:"));
        addPanel.add(nameField);
        addPanel.add(new JLabel("Party:"));
        addPanel.add(partyField);
        addPanel.add(addButton);
        addPanel.add(deleteButton);
        addPanel.add(backButton);

        addButton.addActionListener(e -> {
            String id = idField.getText().trim();
            String name = nameField.getText().trim();
            String party = partyField.getText().trim();
            if (!id.isEmpty() && !name.isEmpty() && !party.isEmpty()) {
                if (id.matches("[a-zA-Z0-9\\-]+")) {
                    if (!ElectionData.nomineeIdExists(id)) {
                        Nominee newNominee = new Nominee(id, name, party);
                        if (ElectionData.addNominee(newNominee)) {
                            nomineeListModel.addElement(newNominee);
                            idField.setText("");
                            nameField.setText("");
                            partyField.setText("");
                            JOptionPane.showMessageDialog(this, "Nominee added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this, "Failed to save nominee to database", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "Nominee ID already exists", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "ID must contain only letters, numbers, or hyphens", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "All fields must be filled (Name and Party are required)", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        deleteButton.addActionListener(e -> {
            Nominee selected = nomineeJList.getSelectedValue();
            if (selected != null) {
                if (ElectionData.deleteNominee(selected.getNomineeName())) {
                    nomineeListModel.removeElement(selected);
                    JOptionPane.showMessageDialog(this, "Nominee deleted successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete nominee from database", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a nominee to delete", "Selection Error", JOptionPane.WARNING_MESSAGE);
            }
        });

        backButton.addActionListener(e -> dashboard.resetMainPanel());

        add(new JScrollPane(nomineeJList), BorderLayout.CENTER);
        add(addPanel, BorderLayout.SOUTH);
    }
}