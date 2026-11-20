package gui;

import service.ReportService;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * Home page (the screen shown right after logging in):
 * a welcome banner, a "Room Overview" row of stat cards, and a room status section.
 *
 * Note: "Available" is NOT a stat card in the overview row on purpose. It is already
 * shown in the Room status panel and in the "Ready to welcome" card below it.
 */
public class DashboardPanel extends JPanel {

    // ---------- Colors ----------
    private static final Color PAGE_BG       = new Color(247, 245, 241); // cream background
    private static final Color BURGUNDY      = new Color(76, 18, 27);    // banner + dark cards
    private static final Color BURGUNDY_TEXT = new Color(74, 14, 26);    // big numbers
    private static final Color GOLD_TEXT     = new Color(232, 200, 130); // small gold labels on burgundy
    private static final Color GREEN         = new Color(80, 120, 104);  // available
    private static final Color GOLD          = new Color(184, 150, 90);  // reserved
    private static final Color GOLD_DARK     = new Color(150, 112, 62);  // reserved number
    private static final Color TEXT_DARK     = new Color(40, 38, 38);
    private static final Color MUTED         = new Color(120, 126, 136);
    private static final Color CARD_WHITE    = Color.WHITE;
    private static final Color CARD_TOTAL    = new Color(229, 231, 230); // grey highlight for "Total rooms"
    private static final Color CARD_BORDER   = new Color(232, 228, 222);
    private static final Color TRACK         = new Color(235, 232, 228); // empty part of the status bar

    // ---------- Sizes ----------
    private static final int HERO_HEIGHT   = 225;
    private static final int CARD_HEIGHT   = 136;
    private static final int BOTTOM_HEIGHT = 144;
    private static final int GAP           = 16;
    private static final String HERO_IMAGE = "/gui/mayon.jpg";

    private final ReportService reportService;
    private final BufferedImage heroImage = loadImage(HERO_IMAGE);

    // Labels that get updated when the dashboard refreshes
    private JLabel summaryLabel;      // "30 rooms · Across 4 floors"
    private JLabel roomsValue;
    private JLabel floorsNote;
    private JLabel occupiedValue;
    private JLabel reservedValue;
    private JLabel statusTotalLabel;  // "30 total rooms"
    private JLabel readyHeadline;     // "15 rooms available"
    private StatusBar statusBar;
    private JLabel availableCount, occupiedCount, reservedCount; // legend lines

    public DashboardPanel(ReportService reportService) {

        if (reportService == null) {
            throw new IllegalArgumentException("Report service is required.");
        }
        this.reportService = reportService;

        setLayout(new GridBagLayout());
        setBackground(PAGE_BG);
        setBorder(new EmptyBorder(26, 26, 14, 26)); // top, left, bottom, right

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;

        // Banner: the only row that is allowed to shrink on small windows
        c.gridy = 0; c.weighty = 1;
        add(createHeroSection(), c);

        c.gridy = 1; c.weighty = 0;
        add(createOverviewHeader(), c);

        c.gridy = 2;
        add(createCardsRow(), c);

        c.gridy = 3;
        add(createBottomRow(), c);

        refreshDashboard();
    }

    // =====================================================================
    //  BANNER: burgundy card, welcome text on the left, Mayon photo on the right
    // =====================================================================
    private JPanel createHeroSection() {

        JPanel hero = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                int w = getWidth(), h = getHeight();
                g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, 16, 16));
                g2.setColor(BURGUNDY);
                g2.fillRect(0, 0, w, h);

                if (heroImage != null) {
                    int imgX = (int) (w * 0.58);
                    int imgW = w - imgX;
                    drawCover(g2, heroImage, imgX, 0, imgW, h);

                    // fade the photo into the burgundy on its left edge
                    // A few solid pixels first, so the photo's hard left edge can never
                    // show as a thin line (happens on scaled / fractional-DPI displays).
                    int seam = 4;
                    g2.setColor(BURGUNDY);
                    g2.fillRect(imgX - seam, 0, seam * 2, h);

                    int fade = Math.min(160, imgW);
                    g2.setPaint(new GradientPaint(imgX + seam, 0, BURGUNDY,
                                                  imgX + seam + fade, 0, new Color(76, 18, 27, 0)));
                    g2.fillRect(imgX + seam, 0, fade, h);
                }
                g2.dispose();
            }
        };
        hero.setOpaque(false);
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setBorder(new EmptyBorder(24, 26, 22, 26));
        hero.setPreferredSize(new Dimension(100, HERO_HEIGHT));
        hero.setMinimumSize(new Dimension(100, 150));

        JLabel eyebrow = new JLabel("HERZ Hotel - Legazpi City");
        eyebrow.setFont(font("SansSerif", Font.PLAIN, 11, "Inter"));
        eyebrow.setForeground(GOLD_TEXT);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel greeting = new JLabel("<html>Good day,<br>Administrator!</html>");
        greeting.setFont(font("SansSerif", Font.BOLD, 38, "Inter"));
        greeting.setForeground(Color.WHITE);
        greeting.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel description = new JLabel(
            "<html>Your central workspace for managing hotel operations.<br>"
            + "Keep track of guests, monitor room occupancy, organize reservations,<br>"
            + "and ensure daily operations.</html>");
        description.setFont(font("SansSerif", Font.PLAIN, 12, "Inter"));
        description.setForeground(Color.WHITE);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        hero.add(eyebrow);
        hero.add(Box.createVerticalStrut(10));
        hero.add(greeting);
        hero.add(Box.createVerticalGlue());
        hero.add(description);
        return hero;
    }

    // =====================================================================
    //  "Room Overview" title (left) + "30 rooms · Across 4 floors" (right)
    // =====================================================================
    private JPanel createOverviewHeader() {
        JLabel title = new JLabel("Room Overview");
        title.setFont(font("SansSerif", Font.PLAIN, 17, "Inter"));
        title.setForeground(TEXT_DARK);

        summaryLabel = new JLabel(" ");
        summaryLabel.setFont(font("SansSerif", Font.PLAIN, 11, "Inter"));
        summaryLabel.setForeground(MUTED);

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(22, 0, 12, 0));
        row.add(title, BorderLayout.WEST);
        row.add(summaryLabel, BorderLayout.EAST);
        return row;
    }

    // =====================================================================
    //  STAT CARDS: Total rooms, Occupied, Reserved  (no "Available" card)
    // =====================================================================
    private JPanel createCardsRow() {
        roomsValue    = new JLabel("0");
        occupiedValue = new JLabel("0");
        reservedValue = new JLabel("0");
        floorsNote    = noteLabel("No rooms yet");

        JPanel row = new JPanel(new GridLayout(1, 3, GAP, 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(100, CARD_HEIGHT));

        row.add(createStatCard("Total rooms", roomsValue, floorsNote,
                               CARD_TOTAL, BURGUNDY_TEXT, BURGUNDY));
        row.add(createStatCard("Occupied", occupiedValue, noteLabel(" "),
                               CARD_WHITE, BURGUNDY_TEXT, BURGUNDY));
        row.add(createStatCard("Reserved", reservedValue, noteLabel("Upcoming arrivals"),
                               CARD_WHITE, GOLD_DARK, GOLD));
        return row;
    }

    // One stat card: big centered number, label under it, note + colored line at the bottom
    private JPanel createStatCard(String title, JLabel valueLabel, JLabel note,
                                  Color fill, Color numberColor, Color lineColor) {

        RoundedCard card = new RoundedCard(fill, CARD_BORDER);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 20, 16, 20));

        valueLabel.setFont(font("SansSerif", Font.BOLD, 40, "Inter"));
        valueLabel.setForeground(numberColor);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(font("SansSerif", Font.PLAIN, 13, "Inter"));
        titleLabel.setForeground(TEXT_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(Box.createVerticalGlue());
        center.add(valueLabel);
        center.add(Box.createVerticalStrut(2));
        center.add(titleLabel);
        center.add(Box.createVerticalGlue());

        JPanel line = new JPanel();
        line.setBackground(lineColor);
        line.setPreferredSize(new Dimension(1, 3));

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.add(note, BorderLayout.NORTH);
        bottom.add(line, BorderLayout.SOUTH);

        card.add(center, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    // =====================================================================
    //  BOTTOM ROW: Room status (left, wide) + Ready to welcome (right)
    // =====================================================================
    private JPanel createBottomRow() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(GAP, 0, 0, 0));
        row.setPreferredSize(new Dimension(100, BOTTOM_HEIGHT + GAP));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 0;

        c.gridx = 0; c.weightx = 2.0; c.insets = new Insets(0, 0, 0, GAP);
        row.add(createStatusPanel(), c);

        c.gridx = 1; c.weightx = 1.0; c.insets = new Insets(0, 0, 0, 0);
        row.add(createReadyCard(), c);
        return row;
    }

    private JPanel createStatusPanel() {
        RoundedCard card = new RoundedCard(CARD_WHITE, CARD_BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(18, 20, 16, 20));

        JLabel title = new JLabel("Room status");
        title.setFont(font("SansSerif", Font.PLAIN, 16, "Inter"));
        title.setForeground(TEXT_DARK);

        statusTotalLabel = new JLabel(" ");
        statusTotalLabel.setFont(font("SansSerif", Font.PLAIN, 11, "Inter"));
        statusTotalLabel.setForeground(MUTED);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);
        header.add(statusTotalLabel, BorderLayout.EAST);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        statusBar = new StatusBar();
        statusBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusBar.setPreferredSize(new Dimension(100, 14));
        statusBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));

        availableCount = legendCount();
        occupiedCount  = legendCount();
        reservedCount  = legendCount();

        JPanel legend = new JPanel(new GridLayout(1, 3, 12, 0));
        legend.setOpaque(false);
        legend.setAlignmentX(Component.LEFT_ALIGNMENT);
        legend.add(legendItem("Available", GREEN, availableCount));
        legend.add(legendItem("Occupied", BURGUNDY, occupiedCount));
        legend.add(legendItem("Reserved", GOLD, reservedCount));

        card.add(header);
        card.add(Box.createVerticalStrut(14));
        card.add(statusBar);
        card.add(Box.createVerticalStrut(16));
        card.add(legend);
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel legendItem(String name, Color dot, JLabel count) {
        JLabel dotLabel = new JLabel("●"); // filled circle
        dotLabel.setFont(font("SansSerif", Font.PLAIN, 9, "Inter"));
        dotLabel.setForeground(dot);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(font("SansSerif", Font.BOLD, 11, "Inter"));
        nameLabel.setForeground(TEXT_DARK);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        top.setOpaque(false);
        top.add(dotLabel);
        top.add(nameLabel);

        count.setBorder(new EmptyBorder(4, 14, 0, 0)); // lines up under the name

        JPanel item = new JPanel(new BorderLayout());
        item.setOpaque(false);
        item.add(top, BorderLayout.NORTH);
        item.add(count, BorderLayout.CENTER);
        return item;
    }

    private JLabel legendCount() {
        JLabel label = new JLabel(" ");
        label.setFont(font("SansSerif", Font.PLAIN, 11, "Inter"));
        label.setForeground(MUTED);
        return label;
    }

    // Burgundy card on the right that repeats the available count
    private JPanel createReadyCard() {
        RoundedCard card = new RoundedCard(BURGUNDY, BURGUNDY);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 22, 18, 22));

        JLabel eyebrow = new JLabel("READY TO WELCOME");
        eyebrow.setFont(font("SansSerif", Font.PLAIN, 10, "Inter"));
        eyebrow.setForeground(GOLD_TEXT);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        readyHeadline = new JLabel("0 rooms available");
        readyHeadline.setFont(font("SansSerif", Font.BOLD, 20, "Inter"));
        readyHeadline.setForeground(Color.WHITE);
        readyHeadline.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel welcome = new JLabel("A warm welcome starts here.");
        welcome.setFont(font("SansSerif", Font.PLAIN, 12, "Inter"));
        welcome.setForeground(Color.WHITE);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(eyebrow);
        card.add(Box.createVerticalStrut(12));
        card.add(readyHeadline);
        card.add(Box.createVerticalGlue());
        card.add(welcome);
        return card;
    }

    private JLabel noteLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(font("SansSerif", Font.PLAIN, 11, "Inter"));
        label.setForeground(MUTED);
        return label;
    }

    // =====================================================================
    //  DATA: call this to update every number on the page
    // =====================================================================
    public void refreshDashboard() {
        int total     = reportService.getTotalRooms();
        int available = reportService.getAvailableRooms();
        int occupied  = reportService.getOccupiedRooms();
        int reserved  = reportService.getReservedRooms();
        int floors    = reportService.getFloorCount();

        String floorsText = floors == 0
            ? "No rooms yet"
            : "Across " + floors + (floors == 1 ? " floor" : " floors");

        roomsValue.setText(String.valueOf(total));
        occupiedValue.setText(String.valueOf(occupied));
        reservedValue.setText(String.valueOf(reserved));
        floorsNote.setText(floorsText);

        summaryLabel.setText(total == 0
            ? "No rooms yet"
            : roomsText(total) + " · " + floorsText);
        statusTotalLabel.setText(total + " total rooms");

        availableCount.setText(roomsText(available) + " · " + percent(available, total) + "%");
        occupiedCount.setText(roomsText(occupied) + " · " + percent(occupied, total) + "%");
        reservedCount.setText(roomsText(reserved) + " · " + percent(reserved, total) + "%");

        readyHeadline.setText(roomsText(available) + " available");

        statusBar.setValues(total, available, occupied, reserved);
    }

    private static String roomsText(int n) {
        return n + (n == 1 ? " room" : " rooms");
    }

    private static int percent(int part, int total) {
        return total == 0 ? 0 : (int) Math.round(part * 100.0 / total);
    }

    // =====================================================================
    //  HELPERS
    // =====================================================================

    // Uses the nice font if it's installed, otherwise a built-in one
    private static Font font(String fallback, int style, int size, String preferred) {
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (name.equalsIgnoreCase(preferred)) return new Font(preferred, style, size);
        }
        return new Font(fallback, style, size);
    }

    private static BufferedImage loadImage(String resource) {
        try {
            URL url = DashboardPanel.class.getResource(resource);
            if (url != null) return ImageIO.read(url);
        } catch (Exception ignored) { }
        return null;
    }

    // Draws an image so it fills the box (like CSS "object-fit: cover")
    private static void drawCover(Graphics2D g2, BufferedImage img, int x, int y, int w, int h) {
        double scale = Math.max((double) w / img.getWidth(), (double) h / img.getHeight());
        int dw = (int) Math.ceil(img.getWidth() * scale);
        int dh = (int) Math.ceil(img.getHeight() * scale);
        int dx = x + (w - dw) / 2;
        int dy = y + (h - dh) / 2;
        Shape oldClip = g2.getClip();
        g2.clipRect(x, y, w, h);
        g2.drawImage(img, dx, dy, dw, dh, null);
        g2.setClip(oldClip);
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
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(stroke);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // The horizontal Available / Occupied / Reserved bar
    private static class StatusBar extends JComponent {
        private int total, available, occupied, reserved;

        void setValues(int total, int available, int occupied, int reserved) {
            this.total = total;
            this.available = available;
            this.occupied = occupied;
            this.reserved = reserved;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int gap = 3;

            // Everything is drawn inside a rounded pill, so only the outer ends are round
            g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, h, h));
            g2.setColor(TRACK);
            g2.fillRect(0, 0, w, h);

            if (total > 0) {
                int[] counts = { available, occupied, reserved };
                Color[] colors = { GREEN, BURGUNDY, GOLD };

                int shown = 0;
                for (int n : counts) if (n > 0) shown++;
                double usable = w - gap * Math.max(0, shown - 1);

                double x = 0;
                for (int i = 0; i < counts.length; i++) {
                    if (counts[i] == 0) continue;
                    double segW = usable * counts[i] / total;
                    g2.setColor(colors[i]);
                    g2.fill(new java.awt.geom.Rectangle2D.Double(x, 0, segW, h));
                    x += segW + gap;
                }
            }
            g2.dispose();
        }
    }
}