package gui;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Shared look-and-feel helpers (colors, rounded buttons and fields, confirm popup)
 * used by the Rooms and Bookings pages.
 */
final class UiKit {

    private UiKit() { }

    // ---- Palette taken from the design ----
    static final Color PAGE_BG      = new Color(0xF6, 0xF4, 0xF0);
    static final Color CARD_BORDER  = new Color(0xE3, 0xE1, 0xDD);
    static final Color FIELD_BG     = new Color(0xF4, 0xF5, 0xF5);
    static final Color FIELD_BORDER = new Color(0xDD, 0xE0, 0xE2);
    static final Color INK          = new Color(0x1B, 0x2A, 0x3A);
    static final Color MUTED        = new Color(0x6B, 0x77, 0x83);
    static final Color HINT         = new Color(0x9A, 0xA3, 0xAB);
    static final Color BURGUNDY     = new Color(0x4A, 0x04, 0x04);
    static final Color GOLD         = new Color(0x9A, 0x6B, 0x2F);
    static final Color ROW_SELECTED = new Color(0xF3, 0xE4, 0xE4);
    static final Color ROW_LINE     = new Color(0xE6, 0xE6, 0xE6);
    static final Color HEADER_BG    = new Color(0xF1, 0xF2, 0xF2);
    static final Color DANGER       = new Color(0xA5, 0x3B, 0x33);
    static final Color DANGER_BG    = new Color(0xF8, 0xE9, 0xE6);
    static final Color DANGER_LINE  = new Color(0xEB, 0xCF, 0xC9);

    enum ButtonKind { PRIMARY, OUTLINE, DANGER }

    // =====================================================================
    //  Buttons
    // =====================================================================

    static JButton button(String text, ButtonKind kind) {

        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);

                ButtonKind k = (ButtonKind) getClientProperty("kind");

                Color fill = Color.WHITE;
                Color line = CARD_BORDER;
                Color txt = INK;

                if (k == ButtonKind.PRIMARY) {
                    fill = BURGUNDY; line = BURGUNDY; txt = Color.WHITE;
                } else if (k == ButtonKind.DANGER) {
                    line = DANGER_LINE; txt = DANGER;
                }

                if (!isEnabled()) {
                    if (k == ButtonKind.PRIMARY) {
                        fill = new Color(0xB9, 0xA0, 0xA0);
                        line = fill;
                    }
                    txt = k == ButtonKind.PRIMARY ? Color.WHITE : HINT;
                } else if (getModel().isRollover() && k != ButtonKind.PRIMARY) {
                    fill = new Color(0xFA, 0xF8, 0xF6);
                }
                if (getModel().isPressed()) {
                    fill = fill.darker();
                }

                g2.setColor(fill);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.setColor(line);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();

                setForeground(txt);
                super.paintComponent(g);
            }
        };

        button.putClientProperty("kind", kind);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFont(new Font("SansSerif", Font.PLAIN, 12));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    static void setKind(JButton button, ButtonKind kind) {
        button.putClientProperty("kind", kind);
        button.repaint();
    }

    // =====================================================================
    //  Fields and form layout
    // =====================================================================

    static JTextField formField(String placeholder) {
        JTextField f = new PlaceholderField(placeholder);
        f.setFont(new Font("SansSerif", Font.PLAIN, 12));
        f.setForeground(INK);
        f.setBackground(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(FIELD_BORDER, 6, 1),
            BorderFactory.createEmptyBorder(0, 12, 0, 12)
        ));
        f.setPreferredSize(new Dimension(200, 40));
        return f;
    }

    static void styleCombo(JComboBox<?> combo) {
        combo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        combo.setBackground(Color.WHITE);
        combo.setForeground(INK);
        combo.setPreferredSize(new Dimension(200, 40));
    }

    static JPanel labeled(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setForeground(MUTED);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    /** Left-aligns a component inside a vertical BoxLayout and lets it stretch sideways. */
    static <T extends JComponent> T left(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (c instanceof JPanel || c instanceof JLabel) {
            Dimension pref = c.getPreferredSize();
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
        }
        return c;
    }

    static void onChange(JTextComponent field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { action.run(); }
            @Override public void removeUpdate(DocumentEvent e) { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        });
    }

    // =====================================================================
    //  "CONFIRM DELETION" style popup
    // =====================================================================

    static boolean confirm(Component parent, String tagText, String title,
                           String body, String warning, String confirmLabel) {

        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Confirm", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);

        boolean[] confirmed = {false};

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER),
            BorderFactory.createEmptyBorder(24, 26, 22, 26)
        ));

        JLabel tag = new JLabel(tagText);
        tag.setFont(new Font("SansSerif", Font.BOLD, 10));
        tag.setForeground(DANGER);
        tag.setOpaque(true);
        tag.setBackground(DANGER_BG);
        tag.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setForeground(INK);

        JLabel bodyLabel = new JLabel(body);
        bodyLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        bodyLabel.setForeground(MUTED);

        JLabel warningLabel = new JLabel(warning);
        warningLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        warningLabel.setForeground(DANGER);

        root.add(left(tag));
        root.add(Box.createVerticalStrut(18));
        root.add(left(titleLabel));
        root.add(Box.createVerticalStrut(16));
        root.add(left(bodyLabel));
        root.add(Box.createVerticalStrut(14));
        root.add(left(warningLabel));
        root.add(Box.createVerticalStrut(24));

        JButton cancel = button("Cancel", ButtonKind.OUTLINE);
        cancel.setPreferredSize(new Dimension(110, 40));
        cancel.addActionListener(e -> dialog.dispose());

        JButton ok = button(confirmLabel, ButtonKind.PRIMARY);
        ok.setPreferredSize(new Dimension(150, 40));
        ok.addActionListener(e -> {
            confirmed[0] = true;
            dialog.dispose();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(ok);
        root.add(left(buttons));

        dialog.setContentPane(root);
        dialog.setMinimumSize(new Dimension(440, 0));
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        return confirmed[0];
    }

    // =====================================================================
    //  Small components
    // =====================================================================

    /** Text field that paints a gray hint while empty. */
    static class PlaceholderField extends JTextField {

        private final String placeholder;

        PlaceholderField(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(HINT);
                g2.setFont(getFont());
                Insets in = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, in.left, y);
                g2.dispose();
            }
        }
    }

    /** Rounded 1px border. */
    static class RoundedBorder extends AbstractBorder {

        private final Color color;
        private final int radius;
        private final int thickness;

        RoundedBorder(Color color, int radius, int thickness) {
            this.color = color;
            this.radius = radius;
            this.thickness = thickness;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.draw(new RoundRectangle2D.Float(x, y, w - 1, h - 1, radius, radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness + 1, thickness + 1, thickness + 1, thickness + 1);
        }
    }
}