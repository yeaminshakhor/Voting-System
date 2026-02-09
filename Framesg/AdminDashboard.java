package Framesg;

import Data.SqlAdminManager;
import Data.SqlElectionDataManager;
import Data.ElectionScheduler;
import Entities.Nominee;
import Entities.Voter;
import Entities.Admin;
import Utils.Theme;
import Utils.AdminRole;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.*;

/**
 * Updated Admin Dashboard with SQL backend and role-based access control.
 * Different roles see different features based on their permissions.
 * NO SUPERADMIN - All roles have predefined selective permissions.
 */
public class AdminDashboard extends JFrame implements ActionListener {
    
    private String adminId;
    private String adminRole;
    private JFrame parentFrame;
    private JPanel mainPanel;

    public AdminDashboard(String adminId, JFrame parentFrame) {
        this.adminId = adminId;
        this.parentFrame = parentFrame;
        this.adminRole = SqlAdminManager.getRoleById(adminId);
        
        setupWindow();
        initTopBar();
        initMainPanel();
        setVisible(true);
    }

    private void setupWindow() {
        String adminName = SqlAdminManager.getAdminNameById(adminId);
        setTitle("Election Admin Dashboard - " + (adminName != null ? adminName : adminId));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(Theme.WINDOW_WIDTH, Theme.WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BACKGROUND_WHITE);
    }

    private void initTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Theme.PRIMARY_BLUE_DARK);
        topBar.setPreferredSize(new Dimension(getWidth(), Theme.TOPBAR_HEIGHT));
        topBar.setBorder(Theme.getTopBarBorder());

        String adminName = SqlAdminManager.getAdminNameById(adminId);
        String roleDescription = AdminRole.getRoleDescription(adminRole);
        
        if (adminName == null) adminName = "Administrator";
        
        JLabel adminLabel = new JLabel("👤 " + adminName + " | " + adminRole);
        adminLabel.setToolTipText(roleDescription);
        adminLabel.setForeground(Theme.TEXT_WHITE);
        adminLabel.setFont(Theme.BODY_BOLD_FONT);
        topBar.add(adminLabel, BorderLayout.WEST);

        JButton logoutButton = new JButton("Logout");
        logoutButton.setBackground(Theme.PRIMARY_BLUE);
        logoutButton.setForeground(Theme.TEXT_WHITE);
        logoutButton.setFont(Theme.BODY_FONT);
        logoutButton.setFocusPainted(false);
        logoutButton.addActionListener(e -> {
            setVisible(false);
            parentFrame.setVisible(true);
        });
        topBar.add(logoutButton, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
    }

    private void initMainPanel() {
        mainPanel = new JPanel();
        mainPanel.setBackground(Theme.BACKGROUND_WHITE);
        mainPanel.setLayout(new GridLayout(0, 2, 20, 20));
        mainPanel.setBorder(Theme.getMainPanelBorder());

        // SuperAdmin Management Card - SuperAdmin only
        if (adminRole.equals(AdminRole.SUPERADMIN)) {
            mainPanel.add(createCard("🔐 Admin Management", 
                new String[]{"Add new admins", "Delete admins", "Assign roles", "Reset passwords"}, 
                "Manage Admins", Theme.ERROR_RED));
        }

        // Voter Management Card - VoterManager only
        if (AdminRole.hasPermission(adminRole, AdminRole.PERM_ADD_VOTER) || 
            AdminRole.hasPermission(adminRole, AdminRole.PERM_DELETE_VOTER)) {
            mainPanel.add(createCard("👥 Voter Management", 
                new String[]{"Add voters", "View voter list", "Delete voters"}, 
                "Manage Voters", Theme.SUCCESS_GREEN));
        }

        // Nominee Management Card - NomineeManager only
        if (AdminRole.hasPermission(adminRole, AdminRole.PERM_ADD_NOMINEE) || 
            AdminRole.hasPermission(adminRole, AdminRole.PERM_DELETE_NOMINEE)) {
            mainPanel.add(createCard("🗳️ Nominee Management", 
                new String[]{"Add nominees", "View nominee list", "Delete nominees"}, 
                "Manage Nominees", Theme.PRIMARY_BLUE));
        }

        // Election Management Card - ElectionManager only
        if (AdminRole.hasPermission(adminRole, AdminRole.PERM_CONFIGURE_ELECTION)) {
            mainPanel.add(createCard("💼 Election Management", 
                new String[]{"Configure election", "Activate/deactivate", "View election status"}, 
                "Manage Election", Theme.PRIMARY_BLUE));
        }

        // Live Results Card - ReportViewer
        if (AdminRole.hasPermission(adminRole, AdminRole.PERM_VIEW_RESULTS)) {
            mainPanel.add(createCard("📊 Live Results", 
                new String[]{"Real-time vote counts", "Current standings", "Turnout statistics"}, 
                "View Live Results", Theme.WARNING_ORANGE));
        }

        // Audit Logs Card - AuditViewer only
        if (AdminRole.hasPermission(adminRole, AdminRole.PERM_VIEW_AUDIT_LOG)) {
            mainPanel.add(createCard("📋 Audit Logs", 
                new String[]{"View all admin actions", "System activity tracking"}, 
                "View Audit Logs", Theme.PRIMARY_BLUE));
        }

        // If no cards (no permissions), show message
        if (mainPanel.getComponentCount() == 0) {
            JLabel noAccess = new JLabel("<html>❌ No features available for your role:<br/>" + adminRole + "</html>");
            noAccess.setFont(Theme.SUBHEADER_FONT);
            noAccess.setForeground(Theme.ERROR_RED);
            noAccess.setHorizontalAlignment(SwingConstants.CENTER);
            mainPanel.add(noAccess);
        }

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createCard(String title, String[] bullets, String buttonText, Color accentColor) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(10, 10));
        card.setBackground(Theme.CARD_WHITE);
        card.setBorder(Theme.getCardBorder(accentColor));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.SUBHEADER_FONT);
        titleLabel.setForeground(Theme.TEXT_DARK);
        card.add(titleLabel, BorderLayout.NORTH);

        JPanel bulletPanel = new JPanel();
        bulletPanel.setLayout(new BoxLayout(bulletPanel, BoxLayout.Y_AXIS));
        bulletPanel.setBackground(Theme.CARD_WHITE);
        
        for (String bullet : bullets) {
            JLabel bulletLabel = new JLabel("• " + bullet);
            bulletLabel.setFont(Theme.BODY_FONT);
            bulletLabel.setForeground(Theme.TEXT_MEDIUM);
            bulletPanel.add(bulletLabel);
        }
        card.add(bulletPanel, BorderLayout.CENTER);

        JButton actionButton = new JButton(buttonText);
        actionButton.setBackground(accentColor);
        actionButton.setForeground(Theme.TEXT_WHITE);
        actionButton.setFont(Theme.BODY_BOLD_FONT);
        actionButton.setFocusPainted(false);
        actionButton.addActionListener(this);
        actionButton.setActionCommand(buttonText);
        
        actionButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                actionButton.setBackground(Theme.getDarkerColor(accentColor));
            }
            public void mouseExited(MouseEvent e) {
                actionButton.setBackground(accentColor);
            }
        });
        
        card.add(actionButton, BorderLayout.SOUTH);

        return card;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        switch (command) {
            case "Manage Admins":
                if (adminRole.equals(AdminRole.SUPERADMIN)) {
                    showAdminManagement();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Only SuperAdmin can manage admins");
                }
                break;
            case "Manage Voters":
                if (AdminRole.hasPermission(adminRole, AdminRole.PERM_ADD_VOTER)) {
                    showVoterManagement();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ You don't have permission for this action");
                }
                break;
            case "Manage Nominees":
                if (AdminRole.hasPermission(adminRole, AdminRole.PERM_ADD_NOMINEE)) {
                    showNomineeManagement();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ You don't have permission for this action");
                }
                break;
            case "Manage Election":
                if (AdminRole.hasPermission(adminRole, AdminRole.PERM_CONFIGURE_ELECTION)) {
                    showElectionSetup();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ You don't have permission for this action");
                }
                break;
            case "View Live Results":
                if (AdminRole.hasPermission(adminRole, AdminRole.PERM_VIEW_RESULTS)) {
                    showLiveResults();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ You don't have permission for this action");
                }
                break;
            case "View Audit Logs":
                if (AdminRole.hasPermission(adminRole, AdminRole.PERM_VIEW_AUDIT_LOG)) {
                    showAuditLogs();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ You don't have permission for this action");
                }
                break;
        }
    }

    private void showAdminManagement() {
        JDialog dialog = new JDialog(this, "Admin Management", true);
        dialog.setSize(700, 500);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Theme.BACKGROUND_WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Top button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setBackground(Theme.BACKGROUND_WHITE);
        
        JButton addBtn = new JButton("➕ Add Admin");
        addBtn.setBackground(Theme.SUCCESS_GREEN);
        addBtn.setForeground(Theme.TEXT_WHITE);
        addBtn.setFocusPainted(false);
        addBtn.addActionListener(e -> showAddAdminDialog());
        buttonPanel.add(addBtn);
        
        JButton deleteBtn = new JButton("❌ Delete Admin");
        deleteBtn.setBackground(Theme.ERROR_RED);
        deleteBtn.setForeground(Theme.TEXT_WHITE);
        deleteBtn.setFocusPainted(false);
        deleteBtn.addActionListener(e -> showDeleteAdminDialog());
        buttonPanel.add(deleteBtn);
        
        JButton changeRoleBtn = new JButton("🔄 Change Role");
        changeRoleBtn.setBackground(Theme.PRIMARY_BLUE);
        changeRoleBtn.setForeground(Theme.TEXT_WHITE);
        changeRoleBtn.setFocusPainted(false);
        changeRoleBtn.addActionListener(e -> showChangeRoleDialog());
        buttonPanel.add(changeRoleBtn);
        
        panel.add(buttonPanel, BorderLayout.NORTH);
        
        // Admin list
        refreshAdminList(panel);
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(Theme.BACKGROUND_WHITE);
        bottomPanel.add(closeBtn);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }
    
    private void refreshAdminList(JPanel container) {
        // Get all admins
        java.util.List<Admin> admins = SqlAdminManager.getAllAdmins(adminId);
        
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Theme.BACKGROUND_WHITE);
        
        if (admins.isEmpty()) {
            JLabel noAdminsLabel = new JLabel("No admins found");
            noAdminsLabel.setFont(Theme.BODY_FONT);
            listPanel.add(noAdminsLabel);
        } else {
            for (Admin admin : admins) {
                JPanel adminRow = createAdminListRow(admin);
                listPanel.add(adminRow);
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(listPanel);
        container.add(scrollPane, BorderLayout.CENTER);
    }
    
    private JPanel createAdminListRow(Admin admin) {
        JPanel row = new JPanel(new BorderLayout(10, 10));
        row.setBackground(Theme.CARD_WHITE);
        row.setBorder(Theme.getCardBorder(Theme.PRIMARY_BLUE_DARK));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        
        JLabel infoLabel = new JLabel(admin.getAdminId() + " (" + admin.getName() + ") - Role: " + admin.getRole());
        infoLabel.setFont(Theme.BODY_FONT);
        row.add(infoLabel, BorderLayout.WEST);
        
        return row;
    }
    
    private void showAddAdminDialog() {
        JDialog dialog = new JDialog(this, "Add New Admin", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel idLabel = new JLabel("Admin ID:");
        JTextField idField = new JTextField();
        panel.add(idLabel);
        panel.add(idField);
        
        JLabel nameLabel = new JLabel("Name:");
        JTextField nameField = new JTextField();
        panel.add(nameLabel);
        panel.add(nameField);
        
        JLabel passwordLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField();
        panel.add(passwordLabel);
        panel.add(passwordField);
        
        JLabel roleLabel = new JLabel("Role:");
        JComboBox<String> roleCombo = new JComboBox<>(AdminRole.getAllRoles().toArray(new String[0]));
        panel.add(roleLabel);
        panel.add(roleCombo);
        
        JButton addButton = new JButton("Add Admin");
        addButton.addActionListener(e -> {
            String newAdminId = idField.getText().trim();
            String name = nameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String role = (String) roleCombo.getSelectedItem();
            
            if (newAdminId.isEmpty() || name.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "❌ Please fill all fields");
                return;
            }
            
            if (SqlAdminManager.addAdminBySuper(adminId, newAdminId, name, password, role)) {
                JOptionPane.showMessageDialog(dialog, "✅ Admin added successfully!");
                dialog.dispose();
                resetMainPanel();
            } else {
                JOptionPane.showMessageDialog(dialog, "❌ Failed to add admin");
            }
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);
        
        panel.add(buttonPanel);
        
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }
    
    private void showDeleteAdminDialog() {
        java.util.List<Admin> admins = SqlAdminManager.getAllAdmins(adminId);
        
        if (admins.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No admins to delete");
            return;
        }
        
        JDialog dialog = new JDialog(this, "Delete Admin", true);
        dialog.setSize(400, 150);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel label = new JLabel("Select admin to delete:");
        JComboBox<String> adminCombo = new JComboBox<>();
        for (Admin admin : admins) {
            if (!admin.getAdminId().equals(adminId)) {
                adminCombo.addItem(admin.getAdminId() + " (" + admin.getName() + ")");
            }
        }
        
        panel.add(label, BorderLayout.NORTH);
        panel.add(adminCombo, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setBackground(Theme.ERROR_RED);
        deleteBtn.setForeground(Theme.TEXT_WHITE);
        deleteBtn.addActionListener(e -> {
            String selectedItem = (String) adminCombo.getSelectedItem();
            if (selectedItem != null) {
                String targetAdminId = selectedItem.split(" ")[0];
                if (SqlAdminManager.deleteAdminBySuper(adminId, targetAdminId)) {
                    JOptionPane.showMessageDialog(dialog, "✅ Admin deleted successfully!");
                    dialog.dispose();
                    resetMainPanel();
                } else {
                    JOptionPane.showMessageDialog(dialog, "❌ Failed to delete admin");
                }
            }
        });
        buttonPanel.add(deleteBtn);
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelBtn);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }
    
    private void showChangeRoleDialog() {
        java.util.List<Admin> admins = SqlAdminManager.getAllAdmins(adminId);
        
        if (admins.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No admins to change role");
            return;
        }
        
        JDialog dialog = new JDialog(this, "Change Admin Role", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel adminLabel = new JLabel("Select Admin:");
        JComboBox<String> adminCombo = new JComboBox<>();
        for (Admin admin : admins) {
            if (!admin.getAdminId().equals(adminId) && !admin.getRole().equals(AdminRole.SUPERADMIN)) {
                adminCombo.addItem(admin.getAdminId() + " (" + admin.getName() + ")");
            }
        }
        panel.add(adminLabel);
        panel.add(adminCombo);
        
        JLabel roleLabel = new JLabel("New Role:");
        JComboBox<String> roleCombo = new JComboBox<>(AdminRole.getAllRoles().toArray(new String[0]));
        panel.add(roleLabel);
        panel.add(roleCombo);
        
        JButton changeBtn = new JButton("Change Role");
        changeBtn.addActionListener(e -> {
            String selectedAdmin = (String) adminCombo.getSelectedItem();
            String newRole = (String) roleCombo.getSelectedItem();
            
            if (selectedAdmin != null && newRole != null) {
                String targetAdminId = selectedAdmin.split(" ")[0];
                if (SqlAdminManager.reassignAdminRoleBySuper(adminId, targetAdminId, newRole)) {
                    JOptionPane.showMessageDialog(dialog, "✅ Role changed successfully!");
                    dialog.dispose();
                    resetMainPanel();
                } else {
                    JOptionPane.showMessageDialog(dialog, "❌ Failed to change role");
                }
            }
        });
        panel.add(changeBtn);
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        panel.add(cancelBtn);
        
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void showVoterManagement() {
        JDialog dialog = new JDialog(this, "Voter Management", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        if (AdminRole.hasPermission(adminRole, AdminRole.PERM_ADD_VOTER)) {
            JButton addBtn = new JButton("Add Voter");
            addBtn.addActionListener(e -> addVoterDialog());
            buttonPanel.add(addBtn);
        }
        
        if (AdminRole.hasPermission(adminRole, AdminRole.PERM_DELETE_VOTER)) {
            JButton deleteBtn = new JButton("Delete Voter");
            deleteBtn.addActionListener(e -> deleteVoterDialog());
            buttonPanel.add(deleteBtn);
        }
        
        panel.add(buttonPanel, BorderLayout.NORTH);
        
        // Voter list table
        java.util.List<Voter> voters = SqlElectionDataManager.getAllVoters();
        String[] columnNames = {"Voter ID", "Name", "Registered", "Has Voted"};
        Object[][] data = new Object[voters.size()][4];
        
        for (int i = 0; i < voters.size(); i++) {
            Voter v = voters.get(i);
            data[i][0] = v.getId();
            data[i][1] = v.getName();
            data[i][2] = SqlElectionDataManager.isVoterRegistered(v.getId()) ? "✓" : "✗";
            data[i][3] = SqlElectionDataManager.hasVoterVoted(v.getId()) ? "✓" : "✗";
        }
        
        JTable table = new JTable(data, columnNames);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void addVoterDialog() {
        JDialog addDialog = new JDialog(this, "Add New Voter", true);
        addDialog.setSize(400, 200);
        addDialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Voter ID:"), gbc);
        gbc.gridx = 1;
        JTextField idField = new JTextField(20);
        panel.add(idField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Voter Name:"), gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField(20);
        panel.add(nameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JButton addBtn = new JButton("Add Voter");
        addBtn.addActionListener(e -> {
            String voterId = idField.getText().trim();
            String voterName = nameField.getText().trim();
            
            if (voterId.isEmpty() || voterName.isEmpty()) {
                JOptionPane.showMessageDialog(addDialog, "All fields are required!");
                return;
            }
            
            if (SqlElectionDataManager.addVoter(voterId, voterName)) {
                JOptionPane.showMessageDialog(addDialog, "✅ Voter added successfully!");
                SqlAdminManager.logAdminAction(adminId, "ADD_VOTER", "Added voter: " + voterId);
                addDialog.dispose();
            } else {
                JOptionPane.showMessageDialog(addDialog, "❌ Failed to add voter (may already exist)");
            }
        });
        panel.add(addBtn, gbc);
        
        addDialog.add(panel);
        addDialog.setVisible(true);
    }

    private void deleteVoterDialog() {
        String voterId = JOptionPane.showInputDialog(this, "Enter Voter ID to delete:");
        if (voterId == null || voterId.isEmpty()) return;
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete voter: " + voterId + "?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (SqlElectionDataManager.deleteVoter(voterId)) {
                JOptionPane.showMessageDialog(this, "✅ Voter deleted successfully!");
                SqlAdminManager.logAdminAction(adminId, "DELETE_VOTER", "Deleted voter: " + voterId);
            } else {
                JOptionPane.showMessageDialog(this, "❌ Failed to delete voter");
            }
        }
    }

    private void showNomineeManagement() {
        JDialog dialog = new JDialog(this, "Nominee Management", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        if (AdminRole.hasPermission(adminRole, AdminRole.PERM_ADD_NOMINEE)) {
            JButton addBtn = new JButton("Add Nominee");
            addBtn.addActionListener(e -> addNomineeDialog());
            buttonPanel.add(addBtn);
        }
        
        if (AdminRole.hasPermission(adminRole, AdminRole.PERM_DELETE_NOMINEE)) {
            JButton deleteBtn = new JButton("Delete Nominee");
            deleteBtn.addActionListener(e -> deleteNomineeDialog());
            buttonPanel.add(deleteBtn);
        }
        
        panel.add(buttonPanel, BorderLayout.NORTH);
        
        // Nominee list table
        java.util.List<Nominee> nominees = SqlElectionDataManager.getAllNominees();
        String[] columnNames = {"Nominee ID", "Name", "Party"};
        Object[][] data = new Object[nominees.size()][3];
        
        for (int i = 0; i < nominees.size(); i++) {
            Nominee n = nominees.get(i);
            data[i][0] = n.getId();
            data[i][1] = n.getName();
            data[i][2] = n.getParty();
        }
        
        JTable table = new JTable(data, columnNames);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void addNomineeDialog() {
        JDialog addDialog = new JDialog(this, "Add New Nominee", true);
        addDialog.setSize(400, 250);
        addDialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Nominee ID:"), gbc);
        gbc.gridx = 1;
        JTextField idField = new JTextField(20);
        panel.add(idField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField(20);
        panel.add(nameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Party:"), gbc);
        gbc.gridx = 1;
        JTextField partyField = new JTextField(20);
        panel.add(partyField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JButton addBtn = new JButton("Add Nominee");
        addBtn.addActionListener(e -> {
            String nomineeId = idField.getText().trim();
            String nomineeName = nameField.getText().trim();
            String party = partyField.getText().trim();
            
            if (nomineeId.isEmpty() || nomineeName.isEmpty() || party.isEmpty()) {
                JOptionPane.showMessageDialog(addDialog, "All fields are required!");
                return;
            }
            
            if (SqlElectionDataManager.addNominee(nomineeId, nomineeName, party)) {
                JOptionPane.showMessageDialog(addDialog, "✅ Nominee added successfully!");
                SqlAdminManager.logAdminAction(adminId, "ADD_NOMINEE", "Added nominee: " + nomineeId);
                addDialog.dispose();
            } else {
                JOptionPane.showMessageDialog(addDialog, "❌ Failed to add nominee");
            }
        });
        panel.add(addBtn, gbc);
        
        addDialog.add(panel);
        addDialog.setVisible(true);
    }

    private void deleteNomineeDialog() {
        String nomineeId = JOptionPane.showInputDialog(this, "Enter Nominee ID to delete:");
        if (nomineeId == null || nomineeId.isEmpty()) return;
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete nominee: " + nomineeId + "?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (SqlElectionDataManager.deleteNominee(nomineeId)) {
                JOptionPane.showMessageDialog(this, "✅ Nominee deleted successfully!");
                SqlAdminManager.logAdminAction(adminId, "DELETE_NOMINEE", "Deleted nominee: " + nomineeId);
            } else {
                JOptionPane.showMessageDialog(this, "❌ Failed to delete nominee");
            }
        }
    }

    private void showElectionSetup() {
        JOptionPane.showMessageDialog(this, 
            "⚠️ Election Management\n\n" +
            "Feature for ElectionManager role\n\n" +
            "Allows configuring and managing\n" +
            "the election schedule and status",
            "Election Setup", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showLiveResults() {
        JDialog dialog = new JDialog(this, "Live Election Results", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        
        Map<String, Integer> voteCounts = SqlElectionDataManager.getVoteCounts();
        int totalVotes = SqlElectionDataManager.getTotalVotesCast();
        int totalRegistered = SqlElectionDataManager.getTotalRegisteredVoters();
        
        StringBuilder results = new StringBuilder();
        results.append("📊 LIVE ELECTION RESULTS\n");
        results.append("═════════════════════════════════════════\n\n");
        results.append("Total Registered Voters: ").append(totalRegistered).append("\n");
        results.append("Total Votes Cast: ").append(totalVotes).append("\n");
        results.append("Turnout: ").append(String.format("%.1f%%", 
            (totalRegistered > 0 ? totalVotes * 100.0 / totalRegistered : 0))).append("\n\n");
        results.append("VOTE DISTRIBUTION:\n");
        results.append("─────────────────────────────────────────\n");
        
        if (voteCounts.isEmpty()) {
            results.append("No votes cast yet...\n");
        } else {
            for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
                results.append(String.format("%-20s: %4d votes\n", entry.getKey(), entry.getValue()));
            }
        }
        
        JTextArea textArea = new JTextArea(results.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setMargin(new Insets(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void showAuditLogs() {
        JOptionPane.showMessageDialog(this, 
            "📋 AUDIT LOG VIEWER\n\n" +
            "Feature for AuditViewer role\n\n" +
            "Tracks all admin actions and\n" +
            "system events for security monitoring",
            "Audit Logs", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Refresh the main panel to reload content
     */
    public void resetMainPanel() {
        // Remove the old scroll pane
        for (Component comp : getContentPane().getComponents()) {
            if (comp instanceof JScrollPane) {
                remove(comp);
                break;
            }
        }
        
        // Re-initialize the main panel
        initMainPanel();
        
        // Refresh the UI
        revalidate();
        repaint();
    }}