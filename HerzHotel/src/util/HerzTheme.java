
package util;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.border.Border;

public class HerzTheme {

    // ==========================================
    // HERZ HOTEL COLOR PALETTE
    // ==========================================

    public static final Color BURGUNDY =
            new Color(81, 11, 18);

    public static final Color DARK_BURGUNDY =
            new Color(60, 7, 13);

    public static final Color CREAM =
            new Color(247, 243, 238);

    public static final Color WHITE =
            new Color(255, 255, 255);

    public static final Color BLUSH =
            new Color(242, 227, 229);

    public static final Color DARK_TEXT =
            new Color(41, 35, 35);

    public static final Color SECONDARY_TEXT =
            new Color(125, 115, 112);

    public static final Color BORDER =
            new Color(233, 227, 221);

    public static final Color SUCCESS =
            new Color(48, 125, 85);

    public static final Color WARNING =
            new Color(190, 135, 45);

    public static final Color DANGER =
            new Color(170, 55, 55);


    // ==========================================
    // FONT STYLES
    // ==========================================

    public static final Font BRAND_FONT =
            new Font("Serif", Font.BOLD, 32);

    public static final Font TITLE_FONT =
            new Font("SansSerif", Font.BOLD, 25);

    public static final Font SUBTITLE_FONT =
            new Font("SansSerif", Font.PLAIN, 14);

    public static final Font HEADING_FONT =
            new Font("SansSerif", Font.BOLD, 17);

    public static final Font BODY_FONT =
            new Font("SansSerif", Font.PLAIN, 13);

    public static final Font BUTTON_FONT =
            new Font("SansSerif", Font.BOLD, 13);

    public static final Font SMALL_FONT =
            new Font("SansSerif", Font.PLAIN, 11);


    // ==========================================
    // WINDOW SETTINGS
    // ==========================================

    public static final int WINDOW_WIDTH = 1440;
    public static final int WINDOW_HEIGHT = 850;

    public static final int SIDEBAR_WIDTH = 245;


    // ==========================================
    // REUSABLE BORDERS
    // ==========================================

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        );
    }

    public static Border inputBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        );
    }


    // ==========================================
    // PRIMARY BUTTON
    // ==========================================

    public static JButton createPrimaryButton(String text) {

        JButton button = new JButton(text);

        button.setFont(BUTTON_FONT);
        button.setForeground(WHITE);
        button.setBackground(BURGUNDY);

        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);

        button.setBorder(
                BorderFactory.createEmptyBorder(12, 20, 12, 20)
        );

        button.setCursor(
                new java.awt.Cursor(
                        java.awt.Cursor.HAND_CURSOR
                )
        );

        return button;
    }


    // ==========================================
    // SECONDARY BUTTON
    // ==========================================

    public static JButton createSecondaryButton(String text) {

        JButton button = new JButton(text);

        button.setFont(BUTTON_FONT);
        button.setForeground(BURGUNDY);
        button.setBackground(WHITE);

        button.setFocusPainted(false);
        button.setOpaque(true);

        button.setBorder(
                BorderFactory.createLineBorder(BURGUNDY)
        );

        button.setCursor(
                new java.awt.Cursor(
                        java.awt.Cursor.HAND_CURSOR
                )
        );

        return button;
    }


    // ==========================================
    // TEXT FIELD
    // ==========================================

    public static JTextField createTextField(int columns) {

        JTextField field = new JTextField(columns);

        field.setFont(BODY_FONT);
        field.setForeground(DARK_TEXT);
        field.setBackground(WHITE);

        field.setBorder(inputBorder());

        return field;
    }


    // ==========================================
    // PASSWORD FIELD
    // ==========================================

    public static JPasswordField createPasswordField(int columns) {

        JPasswordField field = new JPasswordField(columns);

        field.setFont(BODY_FONT);
        field.setForeground(DARK_TEXT);
        field.setBackground(WHITE);

        field.setBorder(inputBorder());

        return field;
    }


    // ==========================================
    // PRIVATE CONSTRUCTOR
    // ==========================================

    private HerzTheme() {
        // Prevent unnecessary object creation.
    }
}
