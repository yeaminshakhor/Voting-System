package Utils;

import javax.swing.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonUtils {
    // Date formatting
    private static final SimpleDateFormat DATE_FORMAT = 
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public static String getCurrentDateTime() {
        return DATE_FORMAT.format(new Date());
    }
    
    // File operations
    public static boolean backupFile(String filePath) {
        try {
            File original = new File(filePath);
            if (!original.exists()) return false;
            
            String backupName = filePath + "_backup_" + 
                              new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File backup = new File(backupName);
            
            java.nio.file.Files.copy(original.toPath(), backup.toPath());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // GUI helpers
    public static void showSuccessDialog(JFrame parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Success", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    public static void showErrorDialog(JFrame parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", 
            JOptionPane.ERROR_MESSAGE);
    }
    
    public static boolean showConfirmationDialog(JFrame parent, String message) {
        int response = JOptionPane.showConfirmDialog(parent, message, "Confirm",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return response == JOptionPane.YES_OPTION;
    }
    
    // Validation
    public static boolean isStrongPassword(String password) {
        if (password.length() < 8) return false;
        if (!password.matches(".*[A-Z].*")) return false; // At least one uppercase
        if (!password.matches(".*[a-z].*")) return false; // At least one lowercase
        if (!password.matches(".*\\d.*")) return false;   // At least one digit
        return password.matches(".*[!@#$%^&*].*");       // At least one special char
    }
}