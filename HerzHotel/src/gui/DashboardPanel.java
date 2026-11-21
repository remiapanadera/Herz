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
 * a welcome banner, a "Room Overview" row (Ready to welcome / Occupied / Reserved),
 * and a "Room Types & Rates" section.
 */
public class DashboardPanel extends JPanel {

    // ---------- Colors ----------
    private static final Color PAGE_BG       = new Color(247, 245, 241); // cream background
    private static final Color BURGUNDY      = new Color(76, 18, 27);    // banner + dark cards
    private static final Color BURGUNDY_TEXT = new Color(74, 14, 26);    // big numbers
    private static final Color GOLD_TEXT     = new Color(232, 200, 130); // small gold labels on burgundy
    private static final Color TEXT_DARK     = new Color(40, 38, 38);
    private static final Color MUTED         = new Color(120, 126, 136);
    private static final Color CARD_WHITE    = Color.WHITE;
    private static final Color CARD_BORDER   = new Color(232, 228, 222);

    // ---------- Sizes ----------
    private static final int HERO_HEIGHT   = 170;
    private static final int CARD_HEIGHT   = 112;
    private static final int RATE_HEIGHT   = 196;
    private static final int GAP           = 16;

    // ---------- Room Types & Rates (edit the text and prices here) ----------
    // Each row: { type name, price per night, then the list of features }
    private static final String PESO = "\u20B1";
    private static final String[][] ROOM_TYPES = {
        { "Standard", "1,500", "2 persons", "1 Queen bed",     "Air conditioning", "Private bathroom", "Wi-Fi" },
        { "Deluxe",   "2,500", "3 persons", "Queen + Single",  "Air conditioning", "Private bathroom", "Television" },
        { "Suite",    "4,000", "4 persons", "2 Queen beds",    "Air conditioning", "Private bathroom", "Television" }
    };
    private static final String HERO_IMAGE = "/gui/mayon.jpg";

    private final ReportService reportService;
    private final BufferedImage heroImage = loadImage(HERO_IMAGE);

    // Labels that get updated when the dashboard refreshes
    private JLabel summaryLabel;      // "30 rooms · Across 4 floors"
    private JLabel readyHeadline;     // "15 rooms available"
    private JLabel occupiedValue;
    private JLabel reservedValue;

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
        add(createRatesHeader(), c);

        c.gridy = 4;
        add(createRatesRow(), c);

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
        hero.setBorder(new EmptyBorder(22, 26, 18, 26));
        hero.setPreferredSize(new Dimension(100, HERO_HEIGHT));
        hero.setMinimumSize(new Dimension(100, 120));

        JLabel eyebrow = new JLabel("HERZ Hotel - Legazpi City");
        eyebrow.setFont(font("SansSerif", Font.PLAIN, 11, "Inter"));
        eyebrow.setForeground(GOLD_TEXT);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel greeting = new JLabel("<html>Good day,<br>Administrator!</html>");
        greeting.setFont(font("SansSerif", Font.BOLD, 34, "Inter"));
        greeting.setForeground(Color.WHITE);
        greeting.setAlignmentX(Component.LEFT_ALIGNMENT);

        hero.add(eyebrow);
        hero.add(Box.createVerticalStrut(10));
        hero.add(greeting);
        hero.add(Box.createVerticalGlue());
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
    //  OVERVIEW CARDS: Ready to welcome, Occupied, Reserved
    // =====================================================================
    private JPanel createCardsRow() {
        occupiedValue = new JLabel("0");
        reservedValue = new JLabel("0");

        JPanel row = new JPanel(new GridLayout(1, 3, GAP, 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(100, CARD_HEIGHT));

        row.add(createReadyCard());
        row.add(createStatCard("Occupied", occupiedValue, BURGUNDY_TEXT));
        row.add(createStatCard("Reserved", reservedValue, BURGUNDY_TEXT));
        return row;
    }

    // White card: big centered number with its label under it
    private JPanel createStatCard(String title, JLabel valueLabel, Color numberColor) {

        RoundedCard card = new RoundedCard(CARD_WHITE, CARD_BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(12, 20, 12, 20));

        valueLabel.setFont(font("SansSerif", Font.BOLD, 40, "Inter"));
        valueLabel.setForeground(numberColor);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(font("SansSerif", Font.PLAIN, 13, "Inter"));
        titleLabel.setForeground(TEXT_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createVerticalGlue());
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(titleLabel);
        card.add(Box.createVerticalGlue());
        return card;
    }

    // Burgundy card that shows how many rooms are available
    private JPanel createReadyCard() {
        RoundedCard card = new RoundedCard(BURGUNDY, BURGUNDY);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(18, 22, 16, 22));

        JLabel eyebrow = new JLabel("READY TO WELCOME");
        eyebrow.setFont(font("SansSerif", Font.PLAIN, 10, "Inter"));
        eyebrow.setForeground(GOLD_TEXT);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        readyHeadline = new JLabel("0 rooms available");
        readyHeadline.setFont(font("SansSerif", Font.BOLD, 18, "Inter"));
        readyHeadline.setForeground(Color.WHITE);
        readyHeadline.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel welcome = new JLabel("A warm welcome starts here.");
        welcome.setFont(font("SansSerif", Font.PLAIN, 12, "Inter"));
        welcome.setForeground(Color.WHITE);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(eyebrow);
        card.add(Box.createVerticalStrut(10));
        card.add(readyHeadline);
        card.add(Box.createVerticalGlue());
        card.add(welcome);
        return card;
    }

    // =====================================================================
    //  ROOM TYPES & RATES: Standard / Deluxe / Suite
    // =====================================================================
    private JPanel createRatesHeader() {
        JLabel title = new JLabel("Room Types & Rates");
        title.setFont(font("SansSerif", Font.PLAIN, 17, "Inter"));
        title.setForeground(TEXT_DARK);

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(20, 0, 12, 0));
        row.add(title, BorderLayout.WEST);
        return row;
    }

    private JPanel createRatesRow() {
        JPanel row = new JPanel(new GridLayout(1, ROOM_TYPES.length, GAP, 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(100, RATE_HEIGHT));
        for (String[] type : ROOM_TYPES) {
            row.add(createRateCard(type));
        }
        return row;
    }

    private JPanel createRateCard(String[] type) {
        RoundedCard card = new RoundedCard(CARD_WHITE, CARD_BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(16, 22, 14, 22));

        JLabel name = new JLabel(type[0]);
        name.setFont(font("SansSerif", Font.PLAIN, 17, "Inter"));
        name.setForeground(TEXT_DARK);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel price = new JLabel(PESO + type[1]);
        price.setFont(font("SansSerif", Font.BOLD, 26, "Inter"));
        price.setForeground(BURGUNDY_TEXT);
        price.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel perNight = new JLabel("per night");
        perNight.setFont(font("SansSerif", Font.BOLD, 10, "Inter"));
        perNight.setForeground(TEXT_DARK);
        perNight.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(name);
        card.add(Box.createVerticalStrut(2));
        card.add(price);
        card.add(perNight);
        card.add(Box.createVerticalStrut(14));

        for (int i = 2; i < type.length; i++) {
            JLabel feature = new JLabel(type[i]);
            feature.setFont(font("SansSerif", Font.PLAIN, 12, "Inter"));
            feature.setForeground(TEXT_DARK);
            feature.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(feature);
            card.add(Box.createVerticalStrut(3));
        }
        card.add(Box.createVerticalGlue());
        return card;
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

        summaryLabel.setText(total == 0
            ? "No rooms yet"
            : roomsText(total) + " \u00B7 " + floorsText);

        readyHeadline.setText(roomsText(available) + " available");
        occupiedValue.setText(String.valueOf(occupied));
        reservedValue.setText(String.valueOf(reserved));
    }

    private static String roomsText(int n) {
        return n + (n == 1 ? " room" : " rooms");
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
}