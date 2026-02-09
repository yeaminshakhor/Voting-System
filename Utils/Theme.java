package Utils;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.border.Border;

/**
 * Centralized theme configuration for the Election Management System.
 * All UI colors, fonts, and styles are defined here for consistency.
 */
public class Theme {
    
    // ==================== COLORS ====================
    
    // Primary Colors
    public static final Color PRIMARY_BLUE = Color.decode("#2563EB");
    public static final Color PRIMARY_BLUE_DARK = Color.decode("#1E40AF");
    public static final Color PRIMARY_BLUE_LIGHT = Color.decode("#60A5FA");
    
    // Background Colors
    public static final Color BACKGROUND_WHITE = Color.WHITE;
    public static final Color BACKGROUND_LIGHT = Color.decode("#F8FAFC");
    public static final Color BACKGROUND_DARK = Color.decode("#1E293B");
    
    // Text Colors
    public static final Color TEXT_DARK = Color.decode("#1F2937");
    public static final Color TEXT_MEDIUM = Color.decode("#4B5563");
    public static final Color TEXT_LIGHT = Color.decode("#9CA3AF");
    public static final Color TEXT_WHITE = Color.WHITE;
    
    // Semantic Colors
    public static final Color SUCCESS_GREEN = Color.decode("#22C55E");
    public static final Color SUCCESS_GREEN_DARK = Color.decode("#16A34A");
    public static final Color WARNING_ORANGE = Color.decode("#FF9800");
    public static final Color WARNING_ORANGE_DARK = Color.decode("#F97316");
    public static final Color ERROR_RED = Color.decode("#F44336");
    public static final Color ERROR_RED_DARK = Color.decode("#DC2626");
    public static final Color INFO_BLUE = Color.decode("#3B82F6");
    public static final Color INFO_CYAN = Color.decode("#06B6D4");
    public static final Color MAGENTA = Color.decode("#E91E63");
    public static final Color MAGENTA_DARK = Color.decode("#BE185D");
    public static final Color NAVY_BLUE = Color.decode("#001F3F");
    public static final Color BORDER_GRAY = Color.decode("#D1D5DB");
    
    // Card Colors
    public static final Color CARD_WHITE = Color.WHITE;
    public static final Color CARD_SHADOW = Color.decode("#00000010");
    
    // ==================== FONTS ====================
    
    // Font Families
    public static final String FONT_FAMILY_PRIMARY = "SansSerif";
    public static final String FONT_FAMILY_SECONDARY = "Monospaced";
    
    // Font Sizes
    public static final Font TITLE_FONT = new Font(FONT_FAMILY_PRIMARY, Font.BOLD, 24);
    public static final Font HEADER_FONT = new Font(FONT_FAMILY_PRIMARY, Font.BOLD, 18);
    public static final Font SUBHEADER_FONT = new Font(FONT_FAMILY_PRIMARY, Font.BOLD, 16);
    public static final Font SUBTITLE_FONT = new Font(FONT_FAMILY_PRIMARY, Font.BOLD, 16);
    public static final Font BODY_BOLD_FONT = new Font(FONT_FAMILY_PRIMARY, Font.BOLD, 14);
    public static final Font BUTTON_FONT = new Font(FONT_FAMILY_PRIMARY, Font.BOLD, 14);
    public static final Font BODY_FONT = new Font(FONT_FAMILY_PRIMARY, Font.PLAIN, 14);
    public static final Font SMALL_FONT = new Font(FONT_FAMILY_PRIMARY, Font.PLAIN, 12);
    public static final Font MONOSPACE_FONT = new Font(FONT_FAMILY_SECONDARY, Font.PLAIN, 12);
    
    // ==================== DIMENSIONS ====================
    
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 800;
    public static final int CARD_PADDING = 15;
    public static final int BUTTON_HEIGHT = 40;
    public static final int FIELD_HEIGHT = 35;
    public static final int TOPBAR_HEIGHT = 50;
    
    // ==================== BORDERS ====================
    
    public static Border getCardBorder(Color accentColor) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_SHADOW, 1),
                BorderFactory.createLineBorder(accentColor, 2)
            ),
            BorderFactory.createEmptyBorder(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING)
        );
    }
    
    public static Border getTopBarBorder() {
        return BorderFactory.createEmptyBorder(10, 20, 10, 20);
    }
    
    public static Border getMainPanelBorder() {
        return BorderFactory.createEmptyBorder(20, 20, 20, 20);
    }
    
    public static Border getDialogBorder() {
        return BorderFactory.createEmptyBorder(15, 15, 15, 15);
    }
    
    // ==================== UTILITY METHODS ====================
    
    public static Color getDarkerColor(Color color) {
        return color.darker();
    }
    
    public static Color getBrighterColor(Color color) {
        return color.brighter();
    }
    
    public static Font deriveFontSize(Font font, int size) {
        return font.deriveFont((float) size);
    }
}