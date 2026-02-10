package Framesg;

import Data.ElectionData;
import Data.PortalIntegration;
import javax.swing.*;
import java.awt.*;
import java.io.*;

/**
 * PortalImportDialog - Allows admins to import voters from institute portals.
 * Supports multiple portal types and CSV file imports.
 */
public class PortalImportDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    
    public PortalImportDialog(JFrame parent) {
        super(parent, "Import Voters from Portal", true);
        setSize(600, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));
        
        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(25, 50, 100));
        JLabel headerLabel = new JLabel("Import Voters from Institute Portal");
        headerLabel.setFont(new Font("Serif", Font.BOLD, 18));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel);
        add(headerPanel, BorderLayout.NORTH);
        
        // Main content
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridwidth = 2;
        
        // Tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Tab 1: Import from CSV
        JPanel csvTab = createCSVTab();
        tabbedPane.addTab("CSV File Import", csvTab);
        
        // Tab 2: Portal Configuration
        JPanel configTab = createConfigTab();
        tabbedPane.addTab("Portal Configuration", configTab);
        
        // Tab 3: Direct Portal Fetch
        JPanel directTab = createDirectPortalTab();
        tabbedPane.addTab("Direct Portal Fetch", directTab);
        
        gbc.gridy = 0;
        mainPanel.add(tabbedPane, gbc);
        
        // Add/Close buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton closeButton = new JButton("Close");
        closeButton.setPreferredSize(new Dimension(100, 35));
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);
        
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Create CSV import tab
     */
    private JPanel createCSVTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(Color.WHITE);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Instructions
        JLabel instructions = new JLabel(
            "<html>" +
            "<b>CSV File Format:</b><br><br>" +
            "Each line should contain: <code>student_id,full_name,email</code><br><br>" +
            "Example:<br>" +
            "<code>22-47797-2,Miskat Jahan,miskat@institute.edu</code><br>" +
            "<code>24-59145-3,Yeamin Shakhor,yeamin@institute.edu</code><br><br>" +
            "The email field is optional.<br>" +
            "Voters will be created without passwords (must self-register)." +
            "</html>"
        );
        instructions.setFont(new Font("Serif", Font.PLAIN, 11));
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(instructions, gbc);
        
        // File selector
        JLabel fileLabel = new JLabel("CSV File:");
        fileLabel.setFont(new Font("Serif", Font.BOLD, 12));
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(fileLabel, gbc);
        
        JTextField fileField = new JTextField(30);
        fileField.setFont(new Font("Serif", Font.PLAIN, 11));
        fileField.setEditable(false);
        gbc.gridx = 1;
        panel.add(fileField, gbc);
        
        JButton browseBtn = new JButton("Browse");
        browseBtn.setFont(new Font("Serif", Font.PLAIN, 11));
        gbc.gridx = 2;
        gbc.gridwidth = 1;
        panel.add(browseBtn, gbc);
        
        // Result area
        JTextArea resultArea = new JTextArea(10, 40);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        resultArea.setEditable(false);
        resultArea.setBackground(new Color(240, 240, 240));
        JScrollPane scrollPane = new JScrollPane(resultArea);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        panel.add(scrollPane, gbc);
        
        // Import button
        JButton importBtn = new JButton("Import Voters from CSV");
        importBtn.setBackground(new Color(34, 139, 34));
        importBtn.setForeground(Color.WHITE);
        importBtn.setFont(new Font("Serif", Font.BOLD, 12));
        importBtn.setPreferredSize(new Dimension(200, 35));
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        panel.add(importBtn, gbc);
        
        // Browse button action
        browseBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files (*.csv)", "csv"));
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                fileField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });
        
        // Import button action
        importBtn.addActionListener(e -> {
            String filePath = fileField.getText().trim();
            if (filePath.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a CSV file!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            resultArea.setText("Importing voters from: " + filePath + "\n\n");
            int imported = ElectionData.importVotersFromCSV(filePath);
            
            resultArea.append("✓ Import completed!\n");
            resultArea.append(imported + " voters successfully imported.\n\n");
            resultArea.append("Next steps:\n");
            resultArea.append("1. Voters can now access the registration page\n");
            resultArea.append("2. They must set a password to complete registration\n");
            resultArea.append("3. After registration, they can login and vote\n");
        });
        
        return panel;
    }
    
    /**
     * Create portal configuration tab
     */
    private JPanel createConfigTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(Color.WHITE);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        
        JLabel configLabel = new JLabel(
            "<html>" +
            "<b>Portal Configuration:</b><br><br>" +
            "Configure your institute's portals in <code>portal_config.txt</code><br><br>" +
            "<b>Format:</b><br>" +
            "<code>moodle_url=https://moodle.institute.edu/api/student</code><br>" +
            "<code>moodle_token=your_api_token</code><br>" +
            "<code>ldap_url=ldap://ldap.institute.edu:389</code><br><br>" +
            "<b>Supported Portals:</b><br>" +
            "• Moodle LMS<br>" +
            "• LDAP Directory<br>" +
            "• Custom REST API<br><br>" +
            "Once configured, you can fetch voter data directly from the portal." +
            "</html>"
        );
        configLabel.setFont(new Font("Serif", Font.PLAIN, 11));
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        panel.add(configLabel, gbc);
        
        JButton editConfigBtn = new JButton("Edit Configuration File");
        editConfigBtn.setFont(new Font("Serif", Font.PLAIN, 11));
        gbc.gridy = 1;
        gbc.weighty = 0;
        panel.add(editConfigBtn, gbc);
        
        editConfigBtn.addActionListener(e -> {
            try {
                Desktop.getDesktop().edit(new File("portal_config.txt"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not open editor: " + ex.getMessage());
            }
        });
        
        return panel;
    }
    
    /**
     * Create direct portal fetch tab
     */
    private JPanel createDirectPortalTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(Color.WHITE);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        
        JLabel portallabel = new JLabel("Select Portal:");
        portallabel.setFont(new Font("Serif", Font.BOLD, 12));
        gbc.gridy = 0;
        panel.add(portallabel, gbc);
        
        JComboBox<String> portalSelector = new JComboBox<>(new String[]{
            "-- Select Portal Type --",
            "Moodle LMS",
            "LDAP Directory",
            "Custom Portal"
        });
        gbc.gridy = 1;
        panel.add(portalSelector, gbc);
        
        JLabel idLabel = new JLabel("Student ID to Fetch:");
        idLabel.setFont(new Font("Serif", Font.BOLD, 12));
        gbc.gridy = 2;
        panel.add(idLabel, gbc);
        
        JTextField idField = new JTextField(30);
        idField.setFont(new Font("Serif", Font.PLAIN, 11));
        gbc.gridy = 3;
        panel.add(idField, gbc);
        
        // Result area
        JTextArea resultArea = new JTextArea(8, 40);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        resultArea.setEditable(false);
        resultArea.setBackground(new Color(240, 240, 240));
        JScrollPane scrollPane = new JScrollPane(resultArea);
        gbc.gridy = 4;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(scrollPane, gbc);
        
        JButton fetchBtn = new JButton("Fetch from Portal");
        fetchBtn.setBackground(new Color(65, 105, 225));
        fetchBtn.setForeground(Color.WHITE);
        fetchBtn.setFont(new Font("Serif", Font.BOLD, 12));
        fetchBtn.setPreferredSize(new Dimension(150, 35));
        gbc.gridy = 5;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(fetchBtn, gbc);
        
        fetchBtn.addActionListener(e -> {
            String portal = portalSelector.getSelectedItem().toString().toLowerCase();
            String studentId = idField.getText().trim();
            
            if (studentId.isEmpty() || portal.equals("-- select portal type --")) {
                JOptionPane.showMessageDialog(this, "Please select portal and enter student ID!");
                return;
            }
            
            resultArea.setText("Fetching from " + portal + " for student: " + studentId + "\n\n");
            
            PortalIntegration.StudentRecord student = PortalIntegration.fetchStudentData(portal, studentId);
            if (student != null) {
                resultArea.append("✓ Student found!\n\n");
                resultArea.append("ID: " + student.id + "\n");
                resultArea.append("Name: " + student.name + "\n");
                resultArea.append("Email: " + student.email + "\n");
                if (student.department != null && !student.department.isEmpty()) {
                    resultArea.append("Department: " + student.department + "\n");
                }
                if (student.batch != null && !student.batch.isEmpty()) {
                    resultArea.append("Batch: " + student.batch + "\n");
                }
                resultArea.append("Status: " + student.status + "\n\n");
                
                int response = JOptionPane.showConfirmDialog(this, 
                    "Add this voter to the system?",
                    "Confirm", JOptionPane.YES_NO_OPTION);
                
                if (response == JOptionPane.YES_OPTION) {
                    if (PortalIntegration.registerVoterFromPortal(portal, studentId)) {
                        resultArea.append("✓ Voter added successfully!\n");
                        resultArea.append("They can now register with a password.");
                    }
                }
            } else {
                resultArea.append("✗ Student not found or portal not configured.\n");
                resultArea.append("Please check:\n");
                resultArea.append("1. Portal is configured in portal_config.txt\n");
                resultArea.append("2. Student ID is correct\n");
                resultArea.append("3. Portal is accessible\n");
            }
        });
        
        return panel;
    }
}
