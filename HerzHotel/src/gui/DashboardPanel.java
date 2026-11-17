package gui;

import service.ReportService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;

public class DashboardPanel extends JPanel {

    // ---------- Colors used across the dashboard ----------
    private static final Color PAGE_BG      = new Color(247, 245, 241); // cream background
    private static final Color NAVY         = new Color(22, 40, 64);    // numbers
    private static final Color BURGUNDY_HEX = new Color(74, 0, 0);      // hero text
    private static final Color MUTED        = new Color(120, 126, 136); // small gray text
    private static final Color CARD_WHITE   = Color.WHITE;
    private static final Color CARD_BLUSH   = new Color(239, 222, 222); // pink cards
    private static final Color WHITE_BORDER = new Color(232, 228, 222); // outline of white cards
    private static final Color BLUSH_BORDER = new Color(233, 213, 213); // outline of pink cards

    // ---------- Sizes (change these to resize things) ----------
    private static final int CARD_WIDTH      = 210;  // width of each stat card
    private static final int CARD_HEIGHT     = 128;  // height of each stat card
    private static final int CARD_GAP_X      = 100;  // space between cards (left/right)
    private static final int CARD_GAP_Y      = 50;   // space between cards (top/bottom)
    private static final int TITLE_SIZE            = 40;   // "Herz Hotel" heading
    private static final int DESCRIPTION_SIZE      = 20;   // paragraph under the heading (smaller than the title)
    private static final int DESCRIPTION_MAX_WIDTH = 640;  // widest the paragraph gets before wrapping

    private final ReportService reportService;

    // Labels that get updated when the dashboard refreshes
    private JLabel roomsValue;
    private JLabel availableValue;
    private JLabel occupiedValue;
    private JLabel reservedValue;
    private JLabel occupancyNote; // the "60% occupancy" text
    private JLabel floorsNote;    // the "Across 4 floors" text

    public DashboardPanel(ReportService reportService) {

        if (reportService == null) {
            throw new IllegalArgumentException("Report service is required.");
        }
        this.reportService = reportService;

        // Cream page with margins around the content
        setLayout(new BorderLayout(0, 20));
        setBackground(PAGE_BG);
        setBorder(new EmptyBorder(24, 24, 24, 40)); // top, left, bottom, right

        add(createHeroSection(), BorderLayout.NORTH);   // big heading
        add(createCardsSection(), BorderLayout.CENTER); // the 4 cards

        refreshDashboard(); // fill in the numbers
    }

    // =====================================================================
    //  TOP: hotel title, then a smaller description, then "Legazpi City"
    // =====================================================================
    private JPanel createHeroSection() {
        JPanel hero = new JPanel();
        hero.setOpaque(false);
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));

        String serif = fontFamily("Serif", "Playfair Display");

        // --- Title: "Herz Hotel" ---
        JLabel title = new JLabel("Herz Hotel");
        title.setFont(new Font(serif, Font.PLAIN, TITLE_SIZE));
        title.setForeground(BURGUNDY_HEX);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- Description: a text pane wraps the sentence to the available width.
        // It is capped at DESCRIPTION_MAX_WIDTH so the lines stay a comfortable length,
        // and its height is worked out from that width so no line gets cut off.
        JTextPane description = new JTextPane() {
            @Override public Dimension getPreferredSize() {
                int w = DESCRIPTION_MAX_WIDTH;
                Container parent = getParent();
                if (parent != null && parent.getWidth() > 0) {
                    w = Math.min(w, parent.getWidth());
                }
                setSize(w, Short.MAX_VALUE);
                return new Dimension(w, super.getPreferredSize().height);
            }
            @Override public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        description.setEditable(false);
        description.setFocusable(false);
        description.setOpaque(false);
        description.setBorder(null);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setFontFamily(style, serif);
        StyleConstants.setFontSize(style, DESCRIPTION_SIZE);
        StyleConstants.setForeground(style, BURGUNDY_HEX);
        StyleConstants.setAlignment(style, StyleConstants.ALIGN_LEFT);
        StyleConstants.setLineSpacing(style, 0.15f); // a little space between lines

        try {
            StyledDocument doc = description.getStyledDocument();
            doc.insertString(0,
                "offers comfortable and affordable accommodations for guests "
                + "looking for a relaxing and enjoyable stay.", style);
            doc.setParagraphAttributes(0, doc.getLength(), style, false);
        } catch (BadLocationException e) {
            // cannot happen: the text is inserted at a valid position
        }

        // --- Small gray "Legazpi City" under the description ---
        JLabel city = new JLabel("Legazpi City");
        city.setFont(font("SansSerif", Font.BOLD, 12, "Inter"));
        city.setForeground(MUTED);
        city.setAlignmentX(Component.LEFT_ALIGNMENT);

        hero.add(title);
        hero.add(Box.createVerticalStrut(8));   // space between title and description
        hero.add(description);
        hero.add(Box.createVerticalStrut(10));  // space between description and city
        hero.add(city);
        return hero;
    }

    // =====================================================================
    //  CENTER: 2 x 2 grid of stat cards
    // =====================================================================
    private JPanel createCardsSection() {
        roomsValue     = new JLabel("0");
        availableValue = new JLabel("0");
        occupiedValue  = new JLabel("0");
        reservedValue  = new JLabel("0");
        occupancyNote  = noteLabel("0% occupancy");
        floorsNote     = noteLabel("No rooms yet");

        JPanel grid = new JPanel(new GridLayout(2, 2, CARD_GAP_X, CARD_GAP_Y));
        grid.setOpaque(false);

        // Pink, white / white, pink (checkerboard like the design)
        grid.add(createStatCard("Total rooms", roomsValue, floorsNote,
                                true,  new Color(60, 90, 120)));
        grid.add(createStatCard("Available", availableValue, noteLabel("Ready to welcome"),
                                false, new Color(60, 100, 110)));
        grid.add(createStatCard("Occupied", occupiedValue, occupancyNote,
                                false, new Color(60, 90, 120)));
        grid.add(createStatCard("Reserved", reservedValue, noteLabel("Upcoming arrivals"),
                                true,  new Color(180, 140, 60)));

        // This wrapper keeps the grid centered in the leftover space
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(grid);
        return wrapper;
    }

    // One stat card: title + small dash on top, big number, small note below
    private JPanel createStatCard(String title, JLabel valueLabel, JLabel noteLabel,
                                  boolean blush, Color dashColor) {

        RoundedCard card = new RoundedCard(
            blush ? CARD_BLUSH : CARD_WHITE,
            blush ? BLUSH_BORDER : WHITE_BORDER
        );
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14, 16, 14, 16)); // inner padding
        card.setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));

        // Top row: title on the left, small dash on the right
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(font("SansSerif", Font.PLAIN, 11, "Inter"));
        titleLabel.setForeground(MUTED);

        JLabel dash = new JLabel("–");
        dash.setFont(font("SansSerif", Font.BOLD, 16, "Inter"));
        dash.setForeground(dashColor);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.add(titleLabel, BorderLayout.WEST);
        topRow.add(dash, BorderLayout.EAST);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Big number
        valueLabel.setFont(font("SansSerif", Font.BOLD, 34, "Inter"));
        valueLabel.setForeground(NAVY);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(topRow);
        card.add(Box.createVerticalStrut(10));
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(noteLabel);
        card.add(Box.createVerticalGlue());
        return card;
    }

    // Small gray text used under the numbers
    private JLabel noteLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(font("SansSerif", Font.PLAIN, 10, "Inter"));
        label.setForeground(MUTED);
        return label;
    }

    // =====================================================================
    //  DATA: call this to update the numbers (keeps working like before)
    // =====================================================================
    public void refreshDashboard() {
        roomsValue.setText(String.valueOf(reportService.getTotalRooms()));
        availableValue.setText(String.valueOf(reportService.getAvailableRooms()));
        occupiedValue.setText(String.valueOf(reportService.getOccupiedRooms()));
        reservedValue.setText(String.valueOf(reportService.getReservedRooms()));

        double rate = reportService.getOccupancyRate();
        occupancyNote.setText(String.format("%.0f%% occupancy", rate));

        // "Across 4 floors" comes from the real room numbers
        int floors = reportService.getFloorCount();
        if (floors == 0) {
            floorsNote.setText("No rooms yet");
        } else {
            floorsNote.setText("Across " + floors + (floors == 1 ? " floor" : " floors"));
        }
    }

    // =====================================================================
    //  HELPERS
    // =====================================================================

    // Uses the nice font if it's installed, otherwise a built-in one
    private static Font font(String fallback, int style, int size, String preferred) {
        return new Font(fontFamily(fallback, preferred), style, size);
    }

    // Returns the preferred font family name if installed, otherwise the fallback name
    private static String fontFamily(String fallback, String preferred) {
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (name.equalsIgnoreCase(preferred)) return preferred;
        }
        return fallback;
    }

    // A panel with rounded corners, a fill color, and a thin outline
    private static class RoundedCard extends JPanel {
        private final Color fill, stroke;

        RoundedCard(Color fill, Color stroke) {
            this.fill = fill;
            this.stroke = stroke;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.setColor(stroke);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}