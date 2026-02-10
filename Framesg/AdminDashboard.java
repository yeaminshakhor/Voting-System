package Framesg;

import Data.SqlAdminManager;
import Data.SqlElectionDataManager;
import Entities.Voter;
import Entities.Admin;
import Utils.Theme;
import Utils.AdminRole;
import java.awt.*;
import java.awt.event.*;
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
        // Create a simple voter management interface
        JDialog dialog = new JDialog(this, "Voter Management", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Theme.BACKGROUND_WHITE);
        panel.setBorder(Theme.getDialogBorder());
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBackground(Theme.BACKGROUND_WHITE);
        
        JButton addBtn = new JButton("➕ Add Voter");
        addBtn.setBackground(Theme.SUCCESS_GREEN);
        addBtn.setForeground(Theme.TEXT_WHITE);
        addBtn.setFont(Theme.BODY_FONT);
        addBtn.addActionListener(evt -> showVoterAddDialog(dialog));
        buttonPanel.add(addBtn);
        
        JButton viewBtn = new JButton("👁️ View Voters");
        viewBtn.setBackground(Theme.INFO_BLUE);
        viewBtn.setForeground(Theme.TEXT_WHITE);
        viewBtn.setFont(Theme.BODY_FONT);
        viewBtn.addActionListener(evt -> showVoterListDialog());
        buttonPanel.add(viewBtn);
        
        panel.add(buttonPanel, BorderLayout.NORTH);
        
        // Information panel
        JTextArea infoArea = new JTextArea(
            "Voter Management System\n" +
            "══════════════════════════════════════════\n\n" +
            "This feature allows you to manage voter accounts.\n\n" +
            "Available actions:\n" +
            "• Add new voters to the system\n" +
            "• View all registered voters\n" +
            "• Check voter registration status\n" +
            "• Monitor voting activity\n\n" +
            "Note: For full functionality, ensure SqlElectionManager\n" +
            "is properly implemented with voter management methods.");
        
        infoArea.setEditable(false);
        infoArea.setFont(Theme.MONOSPACE_FONT);
        infoArea.setBackground(Theme.BACKGROUND_LIGHT);
        infoArea.setForeground(Theme.TEXT_DARK);
        infoArea.setMargin(new Insets(10, 10, 10, 10));
        
        panel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        
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

    // ==================== NOMINEE MANAGEMENT ====================
    
    private void showNomineeManagement() {
        JOptionPane.showMessageDialog(this, 
            "<html><b>🗳️ Nominee Management</b><br><br>" +
            "Feature for <b>NomineeManager</b> role<br><br>" +
            "Allows managing election candidates:<br>" +
            "• Add new nominees<br>" +
            "• View nominee list<br>" +
            "• Delete nominees<br>" +
            "• Manage political parties</html>",
            "Nominee Management", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    // ==================== ELECTION MANAGEMENT ====================
    
    private void showElectionManagement() {
        JOptionPane.showMessageDialog(this, 
            "<html><b>💼 Election Management</b><br><br>" +
            "Feature for <b>ElectionManager</b> role<br><br>" +
            "Configure election settings:<br>" +
            "• Set start/end dates<br>" +
            "• Activate/deactivate voting<br>" +
            "• View election status<br>" +
            "• Monitor voting progress</html>",
            "Election Management", 
            JOptionPane.INFORMATION_MESSAGE);
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