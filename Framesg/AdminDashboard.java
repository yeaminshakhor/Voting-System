package Framesg;

import Data.SqlAdminManager;
import Data.SqlElectionDataManager;
import Entities.Voter;
import Entities.Admin;
import Entities.Nominee;
import Utils.Theme;
import Utils.AdminRole;
import java.awt.*;
import java.awt.event.*;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.*;
import javax.swing.*;

/**
 * Updated Admin Dashboard with SQL backend and role-based access control.
 * Different roles see different features based on their permissions.
 */
public class AdminDashboard extends JFrame implements ActionListener {
    private static final long serialVersionUID = 1L;
    
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
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
        String roleDescription = AdminRole.getDescription(adminRole);
        
        if (adminName == null) adminName = "Administrator";
        
        JLabel adminLabel = new JLabel("👤 " + adminName + " | " + AdminRole.getDisplayName(adminRole));
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
        if (AdminRole.isSuperAdmin(adminRole)) {
            mainPanel.add(createCard("🔐 Admin Management", 
                new String[]{"Add new admins", "Delete admins", "Assign roles", "Reset passwords"}, 
                "Manage Admins", Theme.ERROR_RED));
        }

        // Voter Management Card - Check permissions properly
        if (AdminRole.hasPermission(adminRole, AdminRole.PERM_MANAGE_VOTERS)) {
            mainPanel.add(createCard("👥 Voter Management", 
                new String[]{"Add voters", "View voter list", "Delete voters", "Export voter data"}, 
                "Manage Voters", Theme.SUCCESS_GREEN));
        }

        // Nominee Management Card - Check permissions properly
        if (AdminRole.hasPermission(adminRole, AdminRole.PERM_MANAGE_NOMINEES)) {
            mainPanel.add(createCard("🗳️ Nominee Management", 
                new String[]{"Add nominees", "View nominee list", "Delete nominees", "Manage parties"}, 
                "Manage Nominees", Theme.PRIMARY_BLUE));
        }

        // Election Management Card - Check permissions properly
        if (AdminRole.hasPermission(adminRole, AdminRole.PERM_MANAGE_ELECTIONS)) {
            mainPanel.add(createCard("💼 Election Management", 
                new String[]{"Configure election", "Activate/deactivate", "View election status", "Set dates"}, 
                "Manage Election", Theme.INFO_CYAN));
        }

        // Voting Policy Management Card - Check permissions properly
        if (AdminRole.hasPermission(adminRole, AdminRole.PERM_MANAGE_ELECTIONS)) {
            mainPanel.add(createCard("🛡️ Voting Policy", 
                new String[]{"Control multi-election voting", "Set voter restrictions", "View voter history", "Configure permissions"}, 
                "Manage Voting Policy", Theme.PRIMARY_BLUE));
        }

        // Live Results Card - Check permissions properly
        if (AdminRole.hasPermission(adminRole, AdminRole.PERM_VIEW_REPORTS)) {
            mainPanel.add(createCard("📊 Live Results", 
                new String[]{"Real-time vote counts", "Current standings", "Turnout statistics", "Export reports"}, 
                "View Live Results", Theme.WARNING_ORANGE));
        }

        // Audit Logs Card - Check permissions properly
        if (AdminRole.hasPermission(adminRole, AdminRole.PERM_VIEW_AUDIT_LOGS)) {
            mainPanel.add(createCard("📋 Audit Logs", 
                new String[]{"View all admin actions", "System activity tracking", "Export logs"}, 
                "View Audit Logs", Theme.NAVY_BLUE));
        }
        
        // Change Password Card - Available to all
        mainPanel.add(createCard("🔑 Change Password", 
            new String[]{"Change your login password", "Security settings"}, 
            "Change Password", Theme.INFO_BLUE));

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
            public void mouseEntered(MouseEvent evt) {
                Color darker = Theme.getDarkerColor(accentColor);
                actionButton.setBackground(darker);
            }
            public void mouseExited(MouseEvent evt) {
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
                if (AdminRole.isSuperAdmin(adminRole)) {
                    showAdminManagement();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Only SuperAdmin can manage admins");
                }
                break;
            case "Manage Voters":
                if (AdminRole.hasPermission(adminRole, AdminRole.PERM_MANAGE_VOTERS)) {
                    showVoterManagement();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ You don't have permission for this action");
                }
                break;
            case "Manage Nominees":
                if (AdminRole.hasPermission(adminRole, AdminRole.PERM_MANAGE_NOMINEES)) {
                    showNomineeManagement();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ You don't have permission for this action");
                }
                break;
            case "Manage Election":
                if (AdminRole.hasPermission(adminRole, AdminRole.PERM_MANAGE_ELECTIONS)) {
                    showElectionManagement();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ You don't have permission for this action");
                }
                break;
            case "Manage Voting Policy":
                if (AdminRole.hasPermission(adminRole, AdminRole.PERM_MANAGE_ELECTIONS)) {
                    showVotingPolicyManagement();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ You don't have permission for this action");
                }
                break;
            case "View Live Results":
                if (AdminRole.hasPermission(adminRole, AdminRole.PERM_VIEW_REPORTS)) {
                    showLiveResults();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ You don't have permission for this action");
                }
                break;
            case "View Audit Logs":
                if (AdminRole.hasPermission(adminRole, AdminRole.PERM_VIEW_AUDIT_LOGS)) {
                    showAuditLogs();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ You don't have permission for this action");
                }
                break;
            case "Change Password":
                showChangePasswordDialog();
                break;
        }
    }

    // ==================== ADMIN MANAGEMENT ====================
    
    private void showAdminManagement() {
        JDialog dialog = new JDialog(this, "Admin Management", true);
        dialog.setSize(700, 500);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Theme.BACKGROUND_WHITE);
        panel.setBorder(Theme.getDialogBorder());
        
        // Top button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setBackground(Theme.BACKGROUND_WHITE);
        
        JButton addBtn = new JButton("➕ Add Admin");
        addBtn.setBackground(Theme.SUCCESS_GREEN);
        addBtn.setForeground(Theme.TEXT_WHITE);
        addBtn.setFont(Theme.BODY_FONT);
        addBtn.setFocusPainted(false);
        addBtn.addActionListener(evt -> showAddAdminDialog(dialog));
        buttonPanel.add(addBtn);
        
        JButton deleteBtn = new JButton("❌ Delete Admin");
        deleteBtn.setBackground(Theme.ERROR_RED);
        deleteBtn.setForeground(Theme.TEXT_WHITE);
        deleteBtn.setFont(Theme.BODY_FONT);
        deleteBtn.setFocusPainted(false);
        deleteBtn.addActionListener(evt -> showDeleteAdminDialog(dialog));
        buttonPanel.add(deleteBtn);
        
        JButton changeRoleBtn = new JButton("🔄 Change Role");
        changeRoleBtn.setBackground(Theme.PRIMARY_BLUE);
        changeRoleBtn.setForeground(Theme.TEXT_WHITE);
        changeRoleBtn.setFont(Theme.BODY_FONT);
        changeRoleBtn.setFocusPainted(false);
        changeRoleBtn.addActionListener(evt -> showChangeRoleDialog(dialog));
        buttonPanel.add(changeRoleBtn);
        
        panel.add(buttonPanel, BorderLayout.NORTH);
        
        // Admin list
        refreshAdminList(panel);
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(evt -> dialog.dispose());
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(Theme.BACKGROUND_WHITE);
        bottomPanel.add(closeBtn);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }
    
    private void refreshAdminList(JPanel container) {
        // Remove existing scroll pane if any
        Component[] comps = container.getComponents();
        for (Component comp : comps) {
            if (comp instanceof JScrollPane) {
                container.remove(comp);
                break;
            }
        }
        
        // Get all admins
        java.util.List<Admin> admins = SqlAdminManager.getAllAdmins(adminId);
        
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Theme.BACKGROUND_WHITE);
        
        if (admins.isEmpty()) {
            JLabel noAdminsLabel = new JLabel("No admins found");
            noAdminsLabel.setFont(Theme.BODY_FONT);
            noAdminsLabel.setForeground(Theme.TEXT_MEDIUM);
            listPanel.add(noAdminsLabel);
        } else {
            for (Admin admin : admins) {
                JPanel adminRow = createAdminListRow(admin);
                listPanel.add(adminRow);
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(listPanel);
        container.add(scrollPane, BorderLayout.CENTER);
        container.revalidate();
        container.repaint();
    }
    
    private JPanel createAdminListRow(Admin admin) {
        JPanel row = new JPanel(new BorderLayout(10, 10));
        row.setBackground(Theme.CARD_WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_GRAY),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        
        JLabel infoLabel = new JLabel("<html><b>" + admin.getAdminId() + "</b> - " + 
                                     admin.getName() + "<br><small>Role: " + 
                                     AdminRole.getDisplayName(admin.getRole()) + "</small></html>");
        infoLabel.setFont(Theme.BODY_FONT);
        row.add(infoLabel, BorderLayout.WEST);
        
        // Add status indicators
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusPanel.setBackground(Theme.CARD_WHITE);
        
        // Check if active
        JLabel statusLabel = new JLabel("Active");
        statusLabel.setForeground(Theme.SUCCESS_GREEN);
        statusLabel.setFont(Theme.SMALL_FONT);
        statusPanel.add(statusLabel);
        
        row.add(statusPanel, BorderLayout.EAST);
        
        return row;
    }
    
    private void showAddAdminDialog(JDialog parentDialog) {
        JDialog dialog = new JDialog(parentDialog, "Add New Admin", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(parentDialog);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.setBorder(Theme.getDialogBorder());
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel idLabel = new JLabel("Admin ID:");
        idLabel.setFont(Theme.BODY_FONT);
        panel.add(idLabel, gbc);
        gbc.gridx = 1;
        JTextField idField = new JTextField(20);
        idField.setFont(Theme.BODY_FONT);
        panel.add(idField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(Theme.BODY_FONT);
        panel.add(nameLabel, gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField(20);
        nameField.setFont(Theme.BODY_FONT);
        panel.add(nameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(Theme.BODY_FONT);
        panel.add(passwordLabel, gbc);
        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setFont(Theme.BODY_FONT);
        panel.add(passwordField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel roleLabel = new JLabel("Role:");
        roleLabel.setFont(Theme.BODY_FONT);
        panel.add(roleLabel, gbc);
        gbc.gridx = 1;
        String[] roles = AdminRole.getAllRoles();
        JComboBox<String> roleCombo = new JComboBox<>(roles);
        roleCombo.setFont(Theme.BODY_FONT);
        panel.add(roleCombo, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        
        JButton addButton = new JButton("Add Admin");
        addButton.setBackground(Theme.SUCCESS_GREEN);
        addButton.setForeground(Theme.TEXT_WHITE);
        addButton.setFont(Theme.BODY_BOLD_FONT);
        addButton.addActionListener(evt -> {
            String newAdminId = idField.getText().trim();
            String name = nameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String role = (String) roleCombo.getSelectedItem();
            
            if (newAdminId.isEmpty() || name.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "❌ Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (password.length() < 6) {
                JOptionPane.showMessageDialog(dialog, "❌ Password must be at least 6 characters", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (SqlAdminManager.addAdminBySuper(adminId, newAdminId, name, password, role)) {
                JOptionPane.showMessageDialog(dialog, "✅ Admin added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                // Refresh admin list in parent dialog
                refreshAdminList((JPanel) parentDialog.getContentPane());
            } else {
                JOptionPane.showMessageDialog(dialog, "❌ Failed to add admin. ID may already exist.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(Theme.BODY_FONT);
        cancelButton.addActionListener(evt -> dialog.dispose());
        
        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, gbc);
        
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }
    
    private void showDeleteAdminDialog(JDialog parentDialog) {
        java.util.List<Admin> admins = SqlAdminManager.getAllAdmins(adminId);
        
        // Filter out current admin and other superadmins
        java.util.List<String> deletableAdmins = new java.util.ArrayList<>();
        java.util.Map<String, String> adminMap = new java.util.HashMap<>();
        
        for (Admin admin : admins) {
            if (!admin.getAdminId().equals(adminId) && !AdminRole.isSuperAdmin(admin.getRole())) {
                String display = admin.getAdminId() + " - " + admin.getName() + " (" + admin.getRole() + ")";
                deletableAdmins.add(display);
                adminMap.put(display, admin.getAdminId());
            }
        }
        
        if (deletableAdmins.isEmpty()) {
            JOptionPane.showMessageDialog(parentDialog, "No admins available to delete", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        JDialog dialog = new JDialog(parentDialog, "Delete Admin", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(parentDialog);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(Theme.getDialogBorder());
        
        JLabel label = new JLabel("Select admin to delete:");
        label.setFont(Theme.BODY_FONT);
        JComboBox<String> adminCombo = new JComboBox<>(deletableAdmins.toArray(new String[0]));
        adminCombo.setFont(Theme.BODY_FONT);
        
        panel.add(label, BorderLayout.NORTH);
        panel.add(adminCombo, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setBackground(Theme.ERROR_RED);
        deleteBtn.setForeground(Theme.TEXT_WHITE);
        deleteBtn.setFont(Theme.BODY_BOLD_FONT);
        deleteBtn.addActionListener(evt -> {
            String selectedItem = (String) adminCombo.getSelectedItem();
            if (selectedItem != null) {
                String targetAdminId = adminMap.get(selectedItem);
                int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Are you sure you want to delete admin: " + targetAdminId + "?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                
                if (confirm == JOptionPane.YES_OPTION) {
                    if (SqlAdminManager.deleteAdminBySuper(adminId, targetAdminId)) {
                        JOptionPane.showMessageDialog(dialog, "✅ Admin deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        refreshAdminList((JPanel) parentDialog.getContentPane());
                    } else {
                        JOptionPane.showMessageDialog(dialog, "❌ Failed to delete admin", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        buttonPanel.add(deleteBtn);
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(Theme.BODY_FONT);
        cancelBtn.addActionListener(evt -> dialog.dispose());
        buttonPanel.add(cancelBtn);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }
    
    private void showChangeRoleDialog(JDialog parentDialog) {
        java.util.List<Admin> admins = SqlAdminManager.getAllAdmins(adminId);
        
        // Filter out current admin and superadmins
        java.util.List<String> changeableAdmins = new java.util.ArrayList<>();
        java.util.Map<String, String> adminMap = new java.util.HashMap<>();
        
        for (Admin admin : admins) {
            if (!admin.getAdminId().equals(adminId) && !AdminRole.isSuperAdmin(admin.getRole())) {
                String display = admin.getAdminId() + " - " + admin.getName() + " (" + admin.getRole() + ")";
                changeableAdmins.add(display);
                adminMap.put(display, admin.getAdminId());
            }
        }
        
        if (changeableAdmins.isEmpty()) {
            JOptionPane.showMessageDialog(parentDialog, "No admins available to change role", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        JDialog dialog = new JDialog(parentDialog, "Change Admin Role", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(parentDialog);
        
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(Theme.getDialogBorder());
        
        JLabel adminLabel = new JLabel("Select Admin:");
        adminLabel.setFont(Theme.BODY_FONT);
        JComboBox<String> adminCombo = new JComboBox<>(changeableAdmins.toArray(new String[0]));
        adminCombo.setFont(Theme.BODY_FONT);
        panel.add(adminLabel);
        panel.add(adminCombo);
        
        JLabel roleLabel = new JLabel("New Role:");
        roleLabel.setFont(Theme.BODY_FONT);
        String[] roles = AdminRole.getAllRoles();
        // Remove SuperAdmin from list (can't assign to others)
        java.util.List<String> assignableRoles = new java.util.ArrayList<>();
        for (String role : roles) {
            if (!AdminRole.isSuperAdmin(role)) {
                assignableRoles.add(role);
            }
        }
        JComboBox<String> roleCombo = new JComboBox<>(assignableRoles.toArray(new String[0]));
        roleCombo.setFont(Theme.BODY_FONT);
        panel.add(roleLabel);
        panel.add(roleCombo);
        
        JButton changeBtn = new JButton("Change Role");
        changeBtn.setBackground(Theme.PRIMARY_BLUE);
        changeBtn.setForeground(Theme.TEXT_WHITE);
        changeBtn.setFont(Theme.BODY_BOLD_FONT);
        changeBtn.addActionListener(evt -> {
            String selectedAdmin = (String) adminCombo.getSelectedItem();
            String newRole = (String) roleCombo.getSelectedItem();
            
            if (selectedAdmin != null && newRole != null) {
                String targetAdminId = adminMap.get(selectedAdmin);
                int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Change role for " + targetAdminId + " to " + AdminRole.getDisplayName(newRole) + "?",
                    "Confirm Change", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                
                if (confirm == JOptionPane.YES_OPTION) {
                    if (SqlAdminManager.reassignAdminRoleBySuper(adminId, targetAdminId, newRole)) {
                        JOptionPane.showMessageDialog(dialog, "✅ Role changed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        refreshAdminList((JPanel) parentDialog.getContentPane());
                    } else {
                        JOptionPane.showMessageDialog(dialog, "❌ Failed to change role", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        panel.add(changeBtn);
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(Theme.BODY_FONT);
        cancelBtn.addActionListener(evt -> dialog.dispose());
        panel.add(cancelBtn);
        
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    // ==================== VOTER MANAGEMENT ====================
    
    private void showVoterManagement() {
        // Create a voter management interface with full functionality
        JDialog dialog = new JDialog(this, "Voter Management", true);
        dialog.setSize(700, 500);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Theme.BACKGROUND_WHITE);
        panel.setBorder(Theme.getDialogBorder());
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        buttonPanel.setBackground(Theme.BACKGROUND_WHITE);
        
        JButton addBtn = new JButton("➕ Add Voter");
        addBtn.setBackground(Theme.SUCCESS_GREEN);
        addBtn.setForeground(Theme.TEXT_WHITE);
        addBtn.setFont(Theme.BODY_FONT);
        addBtn.setFocusPainted(false);
        addBtn.addActionListener(evt -> showVoterAddDialog(dialog));
        buttonPanel.add(addBtn);
        
        JButton viewBtn = new JButton("👁️ View Voters");
        viewBtn.setBackground(Theme.INFO_BLUE);
        viewBtn.setForeground(Theme.TEXT_WHITE);
        viewBtn.setFont(Theme.BODY_FONT);
        viewBtn.setFocusPainted(false);
        viewBtn.addActionListener(evt -> showVoterListDialog());
        buttonPanel.add(viewBtn);
        
        JButton deleteBtn = new JButton("❌ Delete Voter");
        deleteBtn.setBackground(Theme.ERROR_RED);
        deleteBtn.setForeground(Theme.TEXT_WHITE);
        deleteBtn.setFont(Theme.BODY_FONT);
        deleteBtn.setFocusPainted(false);
        deleteBtn.addActionListener(evt -> showDeleteVoterDialog(dialog));
        buttonPanel.add(deleteBtn);
        
        JButton exportBtn = new JButton("📥 Export Voters");
        exportBtn.setBackground(Theme.WARNING_ORANGE);
        exportBtn.setForeground(Theme.TEXT_WHITE);
        exportBtn.setFont(Theme.BODY_FONT);
        exportBtn.setFocusPainted(false);
        exportBtn.addActionListener(evt -> exportVoterList());
        buttonPanel.add(exportBtn);
        
        panel.add(buttonPanel, BorderLayout.NORTH);
        
        // Information panel
        JTextArea infoArea = new JTextArea(
            "Voter Management System\n" +
            "══════════════════════════════════════════\n\n" +
            "Available Actions:\n\n" +
            "➕ Add Voter\n" +
            "   Register a new voter in the system\n\n" +
            "👁️ View Voters\n" +
            "   Display all registered voters\n\n" +
            "❌ Delete Voter\n" +
            "   Remove a voter from the system\n\n" +
            "📥 Export Voters\n" +
            "   Export voter list to CSV file");
        
        infoArea.setEditable(false);
        infoArea.setFont(Theme.MONOSPACE_FONT);
        infoArea.setBackground(Theme.BACKGROUND_LIGHT);
        infoArea.setForeground(Theme.TEXT_DARK);
        infoArea.setMargin(new Insets(10, 10, 10, 10));
        
        panel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        
        JButton closeBtn = new JButton("Close");
        closeBtn.setBackground(Theme.PRIMARY_BLUE);
        closeBtn.setForeground(Theme.TEXT_WHITE);
        closeBtn.setFont(Theme.BODY_FONT);
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(evt -> dialog.dispose());
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(Theme.BACKGROUND_WHITE);
        bottomPanel.add(closeBtn);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }
    
    private void showVoterAddDialog(JDialog parentDialog) {
        JDialog dialog = new JDialog(parentDialog, "Add New Voter", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(parentDialog);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.setBorder(Theme.getDialogBorder());
        panel.setBackground(Theme.BACKGROUND_WHITE);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel idLabel = new JLabel("Voter ID:");
        idLabel.setFont(Theme.BODY_FONT);
        panel.add(idLabel, gbc);
        gbc.gridx = 1;
        JTextField idField = new JTextField(20);
        idField.setFont(Theme.BODY_FONT);
        panel.add(idField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel nameLabel = new JLabel("Voter Name:");
        nameLabel.setFont(Theme.BODY_FONT);
        panel.add(nameLabel, gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField(20);
        nameField.setFont(Theme.BODY_FONT);
        panel.add(nameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        
        JButton addBtn = new JButton("Add Voter");
        addBtn.setBackground(Theme.SUCCESS_GREEN);
        addBtn.setForeground(Theme.TEXT_WHITE);
        addBtn.setFont(Theme.BODY_BOLD_FONT);
        addBtn.addActionListener(evt -> {
            String voterId = idField.getText().trim();
            String voterName = nameField.getText().trim();
            
            if (voterId.isEmpty() || voterName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "❌ Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Try to add voter using SqlElectionManager if available
            try {
                if (SqlElectionDataManager.addVoter(voterId, voterName)) {
                    JOptionPane.showMessageDialog(dialog, "✅ Voter added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    SqlAdminManager.logAdminAction(adminId, "ADD_VOTER", "Added voter: " + voterId);
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "❌ Failed to add voter", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(dialog, 
                    "⚠️ SqlElectionManager.addVoter() not implemented\n" +
                    "Voter would be added: " + voterId + " - " + voterName,
                    "Info", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            }
        });
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(Theme.BODY_FONT);
        cancelBtn.addActionListener(evt -> dialog.dispose());
        
        buttonPanel.add(addBtn);
        buttonPanel.add(cancelBtn);
        panel.add(buttonPanel, gbc);
        
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }
    
    private void showVoterListDialog() {
        JDialog dialog = new JDialog(this, "Voter List", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Theme.BACKGROUND_WHITE);
        panel.setBorder(Theme.getDialogBorder());
        
        JTextArea voterList = new JTextArea();
        voterList.setEditable(false);
        voterList.setFont(Theme.MONOSPACE_FONT);
        voterList.setBackground(Theme.BACKGROUND_LIGHT);
        
        try {
            // Try to get voters from SqlElectionManager
            java.util.List<Voter> voters = SqlElectionDataManager.getAllVoters();
            if (voters != null && !voters.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Registered Voters:\n");
                sb.append("══════════════════════════════════════════\n\n");
                for (Voter voter : voters) {
                    sb.append(String.format("ID: %-10s | Name: %-20s\n", 
                        voter.getId(), voter.getName()));
                }
                voterList.setText(sb.toString());
            } else {
                voterList.setText("No voters found in database.\n\nAdd voters using the 'Add Voter' button.");
            }
        } catch (Exception e) {
            voterList.setText("⚠️ SqlElectionManager.getAllVoters() not implemented\n\n" +
                            "This feature requires voter data access methods.");
        }
        
        panel.add(new JScrollPane(voterList), BorderLayout.CENTER);
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(evt -> dialog.dispose());
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(closeBtn);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void showDeleteVoterDialog(JDialog parentDialog) {
        JDialog dialog = new JDialog(parentDialog, "Delete Voter", true);
        dialog.setSize(420, 220);
        dialog.setLocationRelativeTo(parentDialog);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.setBorder(Theme.getDialogBorder());
        panel.setBackground(Theme.BACKGROUND_WHITE);

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel warningLabel = new JLabel("<html><font color='red'><b>⚠️ WARNING: This action cannot be undone!</b></font></html>");
        warningLabel.setFont(Theme.BODY_BOLD_FONT);
        panel.add(warningLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel idLabel = new JLabel("Enter Voter ID to delete:");
        idLabel.setFont(Theme.BODY_FONT);
        panel.add(idLabel, gbc);

        gbc.gridx = 1;
        JTextField idField = new JTextField(20);
        idField.setFont(Theme.BODY_FONT);
        panel.add(idField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        JButton deleteBtn = new JButton("Delete Voter");
        deleteBtn.setBackground(Theme.ERROR_RED);
        deleteBtn.setForeground(Theme.TEXT_WHITE);
        deleteBtn.setFont(Theme.BODY_BOLD_FONT);
        deleteBtn.addActionListener(evt -> {
            String voterId = idField.getText().trim();

            if (voterId.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "❌ Please enter a voter ID", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Confirm deletion
            int confirm = JOptionPane.showConfirmDialog(dialog,
                "<html><b>Are you absolutely sure?</b><br><br>" +
                "Voter ID: " + voterId + "<br>" +
                "This action will permanently delete this voter and cannot be undone.<br><br>" +
                "Continue?</html>",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    if (Data.ElectionData.deleteVoter(voterId)) {
                        JOptionPane.showMessageDialog(dialog, "✅ Voter deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        SqlAdminManager.logAdminAction(adminId, "DELETE_VOTER", "Deleted voter: " + voterId);
                        dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "❌ Failed to delete voter", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(dialog, "❌ Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        buttonPanel.add(deleteBtn);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setBackground(Theme.PRIMARY_BLUE);
        cancelBtn.setForeground(Theme.TEXT_WHITE);
        cancelBtn.setFont(Theme.BODY_FONT);
        cancelBtn.addActionListener(evt -> dialog.dispose());
        buttonPanel.add(cancelBtn);

        panel.add(buttonPanel, gbc);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void exportVoterList() {
        try {
            String[] voters = Data.ElectionData.getAllVoters();
            
            if (voters == null || voters.length == 0) {
                JOptionPane.showMessageDialog(this, "No voters to export", "Empty List", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            // Create CSV content
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("Voter ID,Voter Name,Status\n");
            
            for (String voter : voters) {
                csvContent.append(voter).append("\n");
            }
            
            // Save to file
            String fileName = "voter_export_" + System.currentTimeMillis() + ".csv";
            java.io.FileWriter writer = new java.io.FileWriter(fileName);
            writer.write(csvContent.toString());
            writer.close();
            
            JOptionPane.showMessageDialog(this,
                "✅ Voter list exported successfully!\n\n" +
                "File: " + fileName + "\n" +
                "Total voters: " + voters.length,
                "Export Successful",
                JOptionPane.INFORMATION_MESSAGE);
            
            SqlAdminManager.logAdminAction(adminId, "EXPORT_VOTERS", "Exported " + voters.length + " voters to " + fileName);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ Error exporting voters: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==================== NOMINEE MANAGEMENT ====================
    
    private void showNomineeManagement() {
        JDialog dialog = new JDialog(this, "Nominee Management", true);
        dialog.setSize(700, 600);
        dialog.setLocationRelativeTo(this);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Theme.BACKGROUND_WHITE);
        mainPanel.setBorder(Theme.getDialogBorder());
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBackground(Theme.CARD_WHITE);
        
        JButton addButton = new JButton("➕ Add Nominee");
        addButton.setBackground(Theme.SUCCESS_GREEN);
        addButton.setForeground(Theme.TEXT_WHITE);
        addButton.setFont(Theme.BODY_BOLD_FONT);
        addButton.setFocusPainted(false);
        addButton.addActionListener(e -> showAddNomineeDialog(dialog));
        buttonPanel.add(addButton);
        
        JButton viewButton = new JButton("👁️ View Nominees");
        viewButton.setBackground(Theme.PRIMARY_BLUE);
        viewButton.setForeground(Theme.TEXT_WHITE);
        viewButton.setFont(Theme.BODY_BOLD_FONT);
        viewButton.setFocusPainted(false);
        viewButton.addActionListener(e -> showNomineeListDialog(dialog));
        buttonPanel.add(viewButton);
        
        JButton deleteButton = new JButton("❌ Delete Nominee");
        deleteButton.setBackground(Theme.ERROR_RED);
        deleteButton.setForeground(Theme.TEXT_WHITE);
        deleteButton.setFont(Theme.BODY_BOLD_FONT);
        deleteButton.setFocusPainted(false);
        deleteButton.addActionListener(e -> showDeleteNomineeDialog(dialog));
        buttonPanel.add(deleteButton);
        
        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        
        // Info panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Theme.CARD_WHITE);
        infoPanel.setBorder(BorderFactory.createTitledBorder("Information"));
        
        String[] allNominees = Data.ElectionData.getAllNominees();
        JLabel infoLabel = new JLabel("<html>" +
            "<b>Total Nominees:</b> " + allNominees.length + "<br>" +
            "<b>Manage:</b> Add new candidates, view existing ones, or delete nominees<br>" +
            "<b>Permission:</b> PERM_MANAGE_NOMINEES required" +
            "</html>");
        infoLabel.setFont(Theme.BODY_FONT);
        infoLabel.setForeground(Theme.TEXT_DARK);
        infoPanel.add(infoLabel);
        infoPanel.add(Box.createVerticalGlue());
        
        mainPanel.add(infoPanel, BorderLayout.CENTER);
        
        // Close button
        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closePanel.setBackground(Theme.BACKGROUND_WHITE);
        JButton closeButton = new JButton("Close");
        closeButton.setBackground(Theme.INFO_CYAN);
        closeButton.setForeground(Theme.TEXT_WHITE);
        closeButton.addActionListener(e -> dialog.dispose());
        closePanel.add(closeButton);
        mainPanel.add(closePanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }
    
    private void showAddNomineeDialog(JDialog parentDialog) {
        JDialog dialog = new JDialog(parentDialog, "Add Nominee", true);
        dialog.setSize(550, 420);
        dialog.setLocationRelativeTo(parentDialog);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.CARD_WHITE);
        panel.setBorder(Theme.getDialogBorder());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel idLabel = new JLabel("Nominee ID:");
        idLabel.setFont(Theme.BODY_FONT);
        JTextField idField = new JTextField(15);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        panel.add(idLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        panel.add(idField, gbc);
        
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(Theme.BODY_FONT);
        JTextField nameField = new JTextField(15);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        panel.add(nameLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        panel.add(nameField, gbc);
        
        JLabel partyLabel = new JLabel("Party:");
        partyLabel.setFont(Theme.BODY_FONT);
        JTextField partyField = new JTextField(15);
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        panel.add(partyLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        panel.add(partyField, gbc);
        
        // Image upload section (only for SuperAdmin or ElectionManager)
        boolean canUploadImage = AdminRole.isSuperAdmin(adminRole) || 
                                AdminRole.hasPermission(adminRole, AdminRole.PERM_MANAGE_ELECTIONS);
        
        JLabel iconLabel = new JLabel("Icon Image:" + (canUploadImage ? " (Optional)" : " (No Access)"));
        iconLabel.setFont(Theme.BODY_FONT);
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        panel.add(iconLabel, gbc);
        
        JLabel selectedFileLabel = new JLabel("No image selected");
        selectedFileLabel.setFont(Theme.BODY_FONT);
        selectedFileLabel.setForeground(Theme.TEXT_MEDIUM);
        
        final String[] selectedImagePath = {null};
        
        JButton uploadButton = new JButton("📁 Browse");
        uploadButton.setBackground(Theme.INFO_CYAN);
        uploadButton.setForeground(Theme.TEXT_WHITE);
        uploadButton.setFont(Theme.BODY_FONT);
        uploadButton.setEnabled(canUploadImage);
        uploadButton.addActionListener(e -> {
            javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
            javax.swing.filechooser.FileNameExtensionFilter filter = 
                new javax.swing.filechooser.FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png", "gif");
            chooser.setFileFilter(filter);
            int result = chooser.showOpenDialog(dialog);
            if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                selectedImagePath[0] = chooser.getSelectedFile().getAbsolutePath();
                selectedFileLabel.setText(chooser.getSelectedFile().getName());
                selectedFileLabel.setForeground(Theme.SUCCESS_GREEN);
            }
        });
        gbc.gridx = 1; gbc.weightx = 0.7;
        panel.add(uploadButton, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.weightx = 1.0;
        panel.add(selectedFileLabel, gbc);
        gbc.gridwidth = 1;
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(Theme.BACKGROUND_WHITE);
        
        JButton addButton = new JButton("Add Nominee");
        addButton.setBackground(Theme.SUCCESS_GREEN);
        addButton.setForeground(Theme.TEXT_WHITE);
        addButton.addActionListener(e -> {
            String id = idField.getText().trim();
            String name = nameField.getText().trim();
            String party = partyField.getText().trim();
            
            if (id.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "ID and Name are required!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Nominee nominee = new Nominee(id, name, party.isEmpty() ? "Independent" : party);
            
            if (Data.ElectionData.addNominee(nominee)) {
                // If image was selected, copy it to nominee folder
                if (selectedImagePath[0] != null) {
                    copyNomineeImage(selectedImagePath[0], id);
                    SqlAdminManager.logAdminAction(adminId, "ADD_NOMINEE", "Added nominee: " + name + " (ID: " + id + ") with image");
                } else {
                    SqlAdminManager.logAdminAction(adminId, "ADD_NOMINEE", "Added nominee: " + name + " (ID: " + id + ")");
                }
                JOptionPane.showMessageDialog(dialog, "✅ Nominee added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "❌ Failed to add nominee", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(addButton);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBackground(Theme.ERROR_RED);
        cancelButton.setForeground(Theme.TEXT_WHITE);
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Theme.BACKGROUND_WHITE);
        mainPanel.add(panel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }
    
    private void copyNomineeImage(String sourcePath, String nomineeId) {
        try {
            java.io.File sourceFile = new java.io.File(sourcePath);
            java.io.File nomineesFolder = new java.io.File("nominee_images");
            if (!nomineesFolder.exists()) {
                nomineesFolder.mkdir();
            }
            
            String extension = sourceFile.getName().substring(sourceFile.getName().lastIndexOf('.'));
            java.io.File destFile = new java.io.File(nomineesFolder, nomineeId + extension);
            
            java.nio.file.Files.copy(sourceFile.toPath(), destFile.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            System.out.println("✅ [AdminDashboard] Nominee image saved: " + destFile.getAbsolutePath());
        } catch (Exception ex) {
            System.out.println("❌ [AdminDashboard] Error copying nominee image: " + ex.getMessage());
        }
    }
    
    private void showNomineeListDialog(JDialog parentDialog) {
        String[] nominees = Data.ElectionData.getAllNominees();
        
        if (nominees.length == 0) {
            JOptionPane.showMessageDialog(parentDialog, "No nominees found in the system.", "Nominee List", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        JDialog dialog = new JDialog(parentDialog, "Nominee List", true);
        dialog.setSize(600, 420);
        dialog.setLocationRelativeTo(parentDialog);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String s : nominees) listModel.addElement(s);

        JList<String> list = new JList<>(listModel);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> listComp, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(listComp, value, index, isSelected, cellHasFocus);
                lbl.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
                try {
                    String text = String.valueOf(value);
                    String id = text.contains(":") ? text.split(":")[0].trim() : (text.contains(" - ") ? text.split(" - ")[0].trim() : null);
                    if (id != null) {
                        java.io.File imgDir = new java.io.File("nominee_images");
                        if (imgDir.exists() && imgDir.isDirectory()) {
                            java.io.File[] files = imgDir.listFiles();
                            if (files != null) {
                                for (java.io.File f : files) {
                                    if (f.getName().startsWith(id + ".")) {
                                        ImageIcon icon = new ImageIcon(f.getAbsolutePath());
                                        Image img = icon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
                                        lbl.setIcon(new ImageIcon(img));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    lbl.setIcon(null);
                }
                return lbl;
            }
        });

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(Theme.getDialogBorder());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Theme.BACKGROUND_WHITE);
        JButton closeButton = new JButton("Close");
        closeButton.setBackground(Theme.INFO_CYAN);
        closeButton.setForeground(Theme.TEXT_WHITE);
        closeButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeButton);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Theme.BACKGROUND_WHITE);
        mainPanel.setBorder(Theme.getDialogBorder());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }
    
    private void showDeleteNomineeDialog(JDialog parentDialog) {
        String nomineeId = JOptionPane.showInputDialog(parentDialog, "Enter Nominee ID to delete:", "Delete Nominee", JOptionPane.PLAIN_MESSAGE);
        
        if (nomineeId == null || nomineeId.trim().isEmpty()) {
            return;
        }
        
        nomineeId = nomineeId.trim();
        int confirm = JOptionPane.showConfirmDialog(parentDialog,
            "❌ Are you sure you want to delete nominee: " + nomineeId + "?\n\nThis action cannot be undone.",
            "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (Data.ElectionData.deleteNominee(nomineeId)) {
                JOptionPane.showMessageDialog(parentDialog, "✅ Nominee deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                SqlAdminManager.logAdminAction(adminId, "DELETE_NOMINEE", "Deleted nominee: " + nomineeId);
            } else {
                JOptionPane.showMessageDialog(parentDialog, "❌ Failed to delete nominee", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ==================== ELECTION MANAGEMENT ====================
    
    private void showElectionManagement() {
        JDialog dialog = new JDialog(this, "Election Management", true);
        dialog.setSize(700, 600);
        dialog.setLocationRelativeTo(this);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Theme.BACKGROUND_WHITE);
        mainPanel.setBorder(Theme.getDialogBorder());
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBackground(Theme.CARD_WHITE);
        
        JButton createButton = new JButton("➕ Create Election");
        createButton.setBackground(Theme.SUCCESS_GREEN);
        createButton.setForeground(Theme.TEXT_WHITE);
        createButton.setFont(Theme.BODY_BOLD_FONT);
        createButton.setFocusPainted(false);
        createButton.addActionListener(e -> showCreateElectionDialog(dialog));
        buttonPanel.add(createButton);
        
        JButton viewButton = new JButton("👁️ View Elections");
        viewButton.setBackground(Theme.PRIMARY_BLUE);
        viewButton.setForeground(Theme.TEXT_WHITE);
        viewButton.setFont(Theme.BODY_BOLD_FONT);
        viewButton.setFocusPainted(false);
        viewButton.addActionListener(e -> showElectionListDialog(dialog));
        buttonPanel.add(viewButton);
        
        JButton statusButton = new JButton("📊 Election Status");
        statusButton.setBackground(Theme.INFO_CYAN);
        statusButton.setForeground(Theme.TEXT_WHITE);
        statusButton.setFont(Theme.BODY_BOLD_FONT);
        statusButton.setFocusPainted(false);
        statusButton.addActionListener(e -> showElectionStatus(dialog));
        buttonPanel.add(statusButton);
        
        JButton deleteButton = new JButton("❌ Delete Election");
        deleteButton.setBackground(Theme.ERROR_RED);
        deleteButton.setForeground(Theme.TEXT_WHITE);
        deleteButton.setFont(Theme.BODY_BOLD_FONT);
        deleteButton.setFocusPainted(false);
        deleteButton.addActionListener(e -> showDeleteElectionDialog(dialog));
        buttonPanel.add(deleteButton);
        
        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        
        // Info panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Theme.CARD_WHITE);
        infoPanel.setBorder(BorderFactory.createTitledBorder("Information"));
        
        JLabel infoLabel = new JLabel("<html>" +
            "<b>Manage Elections:</b><br>" +
            "• Create new elections<br>" +
            "• View election details and status<br>" +
            "• Activate/deactivate elections<br>" +
            "• Delete elections<br>" +
            "<b>Permission:</b> PERM_MANAGE_ELECTIONS required" +
            "</html>");
        infoLabel.setFont(Theme.BODY_FONT);
        infoLabel.setForeground(Theme.TEXT_DARK);
        infoPanel.add(infoLabel);
        infoPanel.add(Box.createVerticalGlue());
        
        mainPanel.add(infoPanel, BorderLayout.CENTER);
        
        // Close button
        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closePanel.setBackground(Theme.BACKGROUND_WHITE);
        JButton closeButton = new JButton("Close");
        closeButton.setBackground(Theme.INFO_CYAN);
        closeButton.setForeground(Theme.TEXT_WHITE);
        closeButton.addActionListener(e -> dialog.dispose());
        closePanel.add(closeButton);
        mainPanel.add(closePanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }
    
    private void showCreateElectionDialog(JDialog parentDialog) {
        JDialog dialog = new JDialog(parentDialog, "Create Election", true);
        dialog.setSize(420, 240);
        dialog.setLocationRelativeTo(parentDialog);
        
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 2, 8, 8));
        panel.setBackground(Theme.CARD_WHITE);
        panel.setBorder(Theme.getDialogBorder());
        
        JLabel nameLabel = new JLabel("Election Name:");
        nameLabel.setFont(Theme.BODY_FONT);
        JTextField nameField = new JTextField(15);
        
        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setFont(Theme.BODY_FONT);
        JComboBox<String> statusBox = new JComboBox<>(new String[]{"ACTIVE", "INACTIVE"});

        // Start / End date-time pickers
        JLabel startLabel = new JLabel("Start (YYYY-MM-DD HH:mm):");
        startLabel.setFont(Theme.BODY_FONT);
        Date startDefault = new Date();
        SpinnerDateModel startModel = new SpinnerDateModel(startDefault, null, null, java.util.Calendar.MINUTE);
        JSpinner startSpinner = new JSpinner(startModel);
        JSpinner.DateEditor startEditor = new JSpinner.DateEditor(startSpinner, "yyyy-MM-dd HH:mm");
        startSpinner.setEditor(startEditor);

        JLabel endLabel = new JLabel("End (YYYY-MM-DD HH:mm):");
        endLabel.setFont(Theme.BODY_FONT);
        Date endDefault = new Date(startDefault.getTime() + (7L * 24 * 60 * 60 * 1000));
        SpinnerDateModel endModel = new SpinnerDateModel(endDefault, null, null, java.util.Calendar.MINUTE);
        JSpinner endSpinner = new JSpinner(endModel);
        JSpinner.DateEditor endEditor = new JSpinner.DateEditor(endSpinner, "yyyy-MM-dd HH:mm");
        endSpinner.setEditor(endEditor);
        
        panel.add(nameLabel);
        panel.add(nameField);
        panel.add(statusLabel);
        panel.add(statusBox);
        panel.add(startLabel);
        panel.add(startSpinner);
        panel.add(endLabel);
        panel.add(endSpinner);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(Theme.BACKGROUND_WHITE);
        
        JButton createButton = new JButton("Create Election");
        createButton.setBackground(Theme.SUCCESS_GREEN);
        createButton.setForeground(Theme.TEXT_WHITE);
        createButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String status = (String) statusBox.getSelectedItem();
            
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Election name is required!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Read selected dates
            Date startDate = (Date) startSpinner.getValue();
            Date endDate = (Date) endSpinner.getValue();
            
            if (startDate.after(endDate)) {
                JOptionPane.showMessageDialog(dialog, "Start date/time must be before end date/time.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean success = Data.ElectionScheduler.setElectionSchedule(name, startDate, endDate, status.equals("ACTIVE"));
            if (success) {
                JOptionPane.showMessageDialog(dialog, "✅ Election created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                SqlAdminManager.logAdminAction(adminId, "CREATE_ELECTION", "Created election: " + name + " with status: " + status);
                dialog.dispose();
            } else {
                // Provide more detailed diagnostics to the admin
                // Sanitize name for filename checks (remove unsafe chars and spaces -> _)
                String safe = name.replaceAll("[^a-zA-Z0-9\\s-]", "").trim().replaceAll("[\\s-]+", "_");
                if (safe.isEmpty()) safe = "election";
                String fileName = "election_schedule_" + safe + ".txt";
                java.io.File f = new java.io.File(fileName);
                String detail;
                if (f.exists()) {
                    detail = "A schedule file was created at: " + f.getAbsolutePath();
                } else {
                    detail = "No schedule file found. Possible causes:\n" +
                             "- Database write failed (check DB connection)\n" +
                             "- File write permission denied in application folder\n" +
                             "- Election name already exists with invalid characters\n" +
                             "Try checking logs or the application console for more details.";
                }
                JOptionPane.showMessageDialog(dialog, "❌ Failed to create election\n\n" + detail, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(createButton);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBackground(Theme.ERROR_RED);
        cancelButton.setForeground(Theme.TEXT_WHITE);
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Theme.BACKGROUND_WHITE);
        mainPanel.add(panel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }
    
    private void showElectionListDialog(JDialog parentDialog) {
        JDialog dialog = new JDialog(parentDialog, "Elections", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(parentDialog);
        
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(Theme.BODY_FONT);
        textArea.setText("╔═══════════════════════════════════════════════════════╗\n");
        textArea.append("║               ELECTIONS                              ║\n");
        textArea.append("╚═══════════════════════════════════════════════════════╝\n\n");
        textArea.append("Current Active Election: " + Data.ElectionScheduler.getCurrentActiveElection() + "\n");
        textArea.append("Status: " + Data.ElectionScheduler.getElectionStatus() + "\n");
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(Theme.getDialogBorder());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Theme.BACKGROUND_WHITE);
        JButton closeButton = new JButton("Close");
        closeButton.setBackground(Theme.INFO_CYAN);
        closeButton.setForeground(Theme.TEXT_WHITE);
        closeButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeButton);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Theme.BACKGROUND_WHITE);
        mainPanel.setBorder(Theme.getDialogBorder());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }
    
    private void showElectionStatus(JDialog parentDialog) {
        String status = Data.ElectionScheduler.getElectionStatus();
        JOptionPane.showMessageDialog(parentDialog, "Election Status:\n\n" + status, "Election Status", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showDeleteElectionDialog(JDialog parentDialog) {
        String electionName = JOptionPane.showInputDialog(parentDialog, "Enter Election name to delete:", "Delete Election", JOptionPane.PLAIN_MESSAGE);
        
        if (electionName == null || electionName.trim().isEmpty()) {
            return;
        }
        
        electionName = electionName.trim();
        int confirm = JOptionPane.showConfirmDialog(parentDialog,
            "❌ Are you sure you want to delete election: " + electionName + "?\n\nThis action cannot be undone.",
            "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (Data.ElectionScheduler.deleteElection(electionName)) {
                JOptionPane.showMessageDialog(parentDialog, "✅ Election deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                SqlAdminManager.logAdminAction(adminId, "DELETE_ELECTION", "Deleted election: " + electionName);
            } else {
                JOptionPane.showMessageDialog(parentDialog, "❌ Failed to delete election", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ==================== VOTING POLICY MANAGEMENT ====================
    
    private void showVotingPolicyManagement() {
        JDialog dialog = new JDialog(this, "Voting Policy Management", true);
        dialog.setSize(700, 500);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Theme.BACKGROUND_WHITE);
        panel.setBorder(Theme.getDialogBorder());
        
        // Policy section
        JPanel policyPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        policyPanel.setBackground(Theme.CARD_WHITE);
        policyPanel.setBorder(BorderFactory.createTitledBorder("Multi-Election Voting Policy"));
        
        // Current policy info
        boolean allowMulti = Data.ElectionData.isMultiElectionVotingAllowed();
        String policyStatus = allowMulti ? 
            "ALLOW: Voters can vote in multiple concurrent elections" :
            "RESTRICT: Voters can only vote in one election total";
        
        JLabel policyLabel = new JLabel("<html><b>Current Policy:</b> " + policyStatus + "</html>");
        policyLabel.setFont(Theme.BODY_BOLD_FONT);
        policyLabel.setForeground(allowMulti ? Theme.SUCCESS_GREEN : Theme.ERROR_RED);
        policyPanel.add(policyLabel);
        
        // Toggle buttons
        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        togglePanel.setBackground(Theme.CARD_WHITE);
        
        JButton allowButton = new JButton("✅ Allow Multi-Election Voting");
        allowButton.setBackground(Theme.SUCCESS_GREEN);
        allowButton.setForeground(Theme.TEXT_WHITE);
        allowButton.setFont(Theme.BODY_FONT);
        allowButton.setFocusPainted(false);
        allowButton.addActionListener(e -> {
            Data.ElectionData.setMultiElectionVotingPolicy(true);
            JOptionPane.showMessageDialog(dialog, 
                "✅ Multi-election voting policy updated!\n\n" +
                "Voters can now vote in multiple concurrent elections.",
                "Policy Updated", JOptionPane.INFORMATION_MESSAGE);
            showVotingPolicyManagement(); // Refresh
            dialog.dispose();
        });
        togglePanel.add(allowButton);
        
        JButton restrictButton = new JButton("🛑 Restrict to Single Election");
        restrictButton.setBackground(Theme.ERROR_RED);
        restrictButton.setForeground(Theme.TEXT_WHITE);
        restrictButton.setFont(Theme.BODY_FONT);
        restrictButton.setFocusPainted(false);
        restrictButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(dialog,
                "<html><b>⚠️ Warning!</b><br><br>" +
                "Enabling single-election restriction will prevent voters who have already voted<br>" +
                "in one election from voting in other concurrent elections.<br><br>" +
                "Are you sure you want to proceed?</html>",
                "Confirm Restriction", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                Data.ElectionData.setMultiElectionVotingPolicy(false);
                JOptionPane.showMessageDialog(dialog,
                    "✅ Single-election voting policy enabled!\n\n" +
                    "Voters can now vote in only one election.",
                    "Policy Updated", JOptionPane.INFORMATION_MESSAGE);
                showVotingPolicyManagement(); // Refresh
                dialog.dispose();
            }
        });
        togglePanel.add(restrictButton);
        policyPanel.add(togglePanel);
        
        // Description
        JLabel descLabel = new JLabel(
            "<html><b>Policy Description:</b><br>" +
            "<b>Allow:</b> A voter can vote in multiple different elections if they're running concurrently.<br><br>" +
            "<b>Restrict:</b> A voter who votes in one election cannot vote in any other election.</html>");
        descLabel.setFont(Theme.BODY_FONT);
        descLabel.setForeground(Theme.TEXT_DARK);
        policyPanel.add(descLabel);
        
        panel.add(policyPanel, BorderLayout.NORTH);
        
        // Voter history section
        JPanel historyPanel = new JPanel(new BorderLayout(5, 5));
        historyPanel.setBackground(Theme.CARD_WHITE);
        historyPanel.setBorder(BorderFactory.createTitledBorder("Voter Voting History"));
        
        JLabel searchLabel = new JLabel("Search Voter ID:");
        searchLabel.setFont(Theme.BODY_BOLD_FONT);
        
        JTextField voterIdField = new JTextField(20);
        voterIdField.setFont(Theme.BODY_FONT);
        
        JButton searchButton = new JButton("Search");
        searchButton.setBackground(Theme.PRIMARY_BLUE);
        searchButton.setForeground(Theme.TEXT_WHITE);
        searchButton.setFont(Theme.BODY_FONT);
        
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        searchPanel.setBackground(Theme.CARD_WHITE);
        searchPanel.add(searchLabel);
        searchPanel.add(voterIdField);
        searchPanel.add(searchButton);
        
        JTextArea historyArea = new JTextArea();
        historyArea.setFont(Theme.MONOSPACE_FONT);
        historyArea.setEditable(false);
        historyArea.setText("Enter a voter ID and click Search to view their voting history");
        historyArea.setLineWrap(true);
        historyArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(historyArea);
        
        searchButton.addActionListener(e -> {
            String voterId = voterIdField.getText().trim();
            if (voterId.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a voter ID", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            java.util.List<String> elections = Data.ElectionData.getVoterElectionHistory(voterId);
            StringBuilder history = new StringBuilder();
            history.append("Voter ID: ").append(voterId).append("\n");
            history.append("═══════════════════════════════════════════\n\n");
            
            if (elections.isEmpty()) {
                history.append("This voter has NOT voted in any election yet.\n");
            } else {
                history.append("Elections voted in: ").append(elections.size()).append("\n\n");
                for (int i = 0; i < elections.size(); i++) {
                    history.append((i+1)).append(". ").append(elections.get(i)).append("\n");
                }
            }
            
            historyArea.setText(history.toString());
        });
        
        historyPanel.add(searchPanel, BorderLayout.NORTH);
        historyPanel.add(scrollPane, BorderLayout.CENTER);
        
        panel.add(historyPanel, BorderLayout.CENTER);
        
        // Close button
        JButton closeBtn = new JButton("Close");
        closeBtn.setBackground(Theme.PRIMARY_BLUE);
        closeBtn.setForeground(Theme.TEXT_WHITE);
        closeBtn.setFont(Theme.BODY_FONT);
        closeBtn.addActionListener(e -> dialog.dispose());
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(Theme.BACKGROUND_WHITE);
        bottomPanel.add(closeBtn);
        
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    // ==================== LIVE RESULTS ====================
    
    private void showLiveResults() {
        JDialog dialog = new JDialog(this, "Live Election Results", true);
        dialog.setSize(500, 300);
        dialog.setLocationRelativeTo(this);
        
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(Theme.MONOSPACE_FONT);
        textArea.setText("📊 LIVE ELECTION RESULTS\n" +
                        "═════════════════════════════════════════\n\n" +
                        "Feature Preview:\n" +
                        "• Real-time vote counts\n" +
                        "• Candidate standings\n" +
                        "• Turnout statistics\n" +
                        "• Visual charts\n\n" +
                        "Requires: Report viewing permission\n\n" +
                        "This feature would connect to SqlElectionManager\n" +
                        "to fetch live vote data from the database.");
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        dialog.add(scrollPane);
        dialog.setVisible(true);
    }

    // ==================== AUDIT LOGS ====================
    
    private void showAuditLogs() {
        // Get audit logs from SqlAdminManager
        java.util.List<String> auditLogs = SqlAdminManager.getAdminAuditTrail(adminId);
        
        JDialog dialog = new JDialog(this, "Audit Logs", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(Theme.MONOSPACE_FONT);
        
        if (auditLogs.isEmpty()) {
            textArea.setText("📋 AUDIT LOGS\n" +
                           "═════════════════════════════════════════\n\n" +
                           "No audit logs found.");
        } else {
            StringBuilder logs = new StringBuilder();
            logs.append("📋 AUDIT LOGS (Last 50 entries)\n");
            logs.append("═════════════════════════════════════════\n\n");
            for (String log : auditLogs) {
                logs.append(log).append("\n");
            }
            textArea.setText(logs.toString());
        }
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        dialog.add(scrollPane);
        dialog.setVisible(true);
    }

    // ==================== CHANGE PASSWORD ====================
    
    private void showChangePasswordDialog() {
        JDialog dialog = new JDialog(this, "Change Password", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.setBorder(Theme.getDialogBorder());
        panel.setBackground(Theme.BACKGROUND_WHITE);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel currentLabel = new JLabel("Current Password:");
        currentLabel.setFont(Theme.BODY_FONT);
        panel.add(currentLabel, gbc);
        gbc.gridx = 1;
        JPasswordField currentPass = new JPasswordField(20);
        currentPass.setFont(Theme.BODY_FONT);
        panel.add(currentPass, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel newLabel = new JLabel("New Password:");
        newLabel.setFont(Theme.BODY_FONT);
        panel.add(newLabel, gbc);
        gbc.gridx = 1;
        JPasswordField newPass = new JPasswordField(20);
        newPass.setFont(Theme.BODY_FONT);
        panel.add(newPass, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel confirmLabel = new JLabel("Confirm New:");
        confirmLabel.setFont(Theme.BODY_FONT);
        panel.add(confirmLabel, gbc);
        gbc.gridx = 1;
        JPasswordField confirmPass = new JPasswordField(20);
        confirmPass.setFont(Theme.BODY_FONT);
        panel.add(confirmPass, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        
        JButton changeBtn = new JButton("Change Password");
        changeBtn.setBackground(Theme.SUCCESS_GREEN);
        changeBtn.setForeground(Theme.TEXT_WHITE);
        changeBtn.setFont(Theme.BODY_BOLD_FONT);
        changeBtn.addActionListener(evt -> {
            String current = new String(currentPass.getPassword());
            String newPassword = new String(newPass.getPassword());
            String confirm = new String(confirmPass.getPassword());
            
            if (current.isEmpty() || newPassword.isEmpty() || confirm.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "❌ Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!newPassword.equals(confirm)) {
                JOptionPane.showMessageDialog(dialog, "❌ New passwords don't match", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (newPassword.length() < 6) {
                JOptionPane.showMessageDialog(dialog, "❌ Password must be at least 6 characters", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (SqlAdminManager.updateAdminPassword(adminId, current, newPassword)) {
                JOptionPane.showMessageDialog(dialog, "✅ Password changed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                SqlAdminManager.logAdminAction(adminId, "PASSWORD_CHANGED", "Admin changed their password");
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "❌ Failed to change password. Check current password.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(Theme.BODY_FONT);
        cancelBtn.addActionListener(evt -> dialog.dispose());
        
        buttonPanel.add(changeBtn);
        buttonPanel.add(cancelBtn);
        panel.add(buttonPanel, gbc);
        
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    /**
     * Refresh the main panel to reload content
     */
    public void resetMainPanel() {
        getContentPane().removeAll();
        initTopBar();
        initMainPanel();
        revalidate();
        repaint();
    }
}