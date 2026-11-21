package gui;

import model.Person;
import service.HotelSystem;
import util.HerzTheme;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainFrame extends JFrame {

    // ---------- Colors ----------
    private static final Color SIDEBAR_BG   = new Color(74, 0, 0);      // burgundy sidebar
    private static final Color SIDEBAR_LINE = new Color(120, 50, 50);   // thin divider lines in the sidebar
    private static final Color PAGE_BG      = new Color(247, 245, 241); // cream page
    private static final Color NAVY         = new Color(22, 40, 64);    // dark text
    private static final Color MUTED        = new Color(120, 126, 136); // gray text
    private static final Color GOLD         = new Color(196, 150, 70);  // active tab marker
    private static final Color LINE         = new Color(236, 232, 228); // light divider lines

    // ---------- Sizes ----------
    private static final int SIDEBAR_WIDTH = 180;
    private static final int TOPBAR_HEIGHT = 65;
    private static final int NAV_HEIGHT    = 38;  // height of each sidebar button
    private static final int NAV_GAP       = 14;  // space between sidebar buttons

    // ---------- Logo image (same file as the login screen) ----------
    private static final String LOGO_RESOURCE = "/gui/logo.png";
    private static final int LOGO_WIDTH       = 56;

    private final String username;          // the logged-in user's name
    private final String userRole;          // shown under the name (comes from the user's getRole())
    private final HotelSystem hotelSystem;

    private JPanel contentPanel;
    private CardLayout cardLayout;

    private NavButton dashboardButton;
    private NavButton guestsButton;
    private NavButton roomsButton;
    private NavButton bookingsButton;

    private DashboardPanel dashboardPanel;
    private GuestPanel guestPanel;
    private RoomPanel roomPanel;
    private BookingPanel bookingPanel;

    // The login screen passes in the logged-in user (an Admin, which is a Person)
    public MainFrame(Person user) {

        if (user == null) {
            throw new IllegalArgumentException("A logged-in user is required.");
        }

        this.username = user.getFullName();
        this.userRole = user.getRole();   // each kind of Person gives its own role

        hotelSystem = new HotelSystem();

        try {
            hotelSystem.loadData();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                null,
                "Could not load saved hotel data:\n" + e.getMessage(),
                "Herz Hotel Data Error",
                JOptionPane.ERROR_MESSAGE
            );
        }

        initializePanels();
        initializeFrame();
    }

    // =====================================================================
    //  WINDOW: sidebar on the left, top bar + pages + footer on the right
    // =====================================================================
    private void initializeFrame() {

        setTitle("Herz Hotel | Hotel Management System");
        setSize(
            HerzTheme.WINDOW_WIDTH,
            HerzTheme.WINDOW_HEIGHT
        );

        setMinimumSize(new Dimension(1100, 700));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        add(createSidebar(), BorderLayout.WEST);

        // The pages (Dashboard, Guests, ...) switch inside this card panel
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(PAGE_BG);

        contentPanel.add(dashboardPanel, "Dashboard");
        contentPanel.add(guestPanel, "Guests");
        contentPanel.add(roomPanel, "Rooms");
        contentPanel.add(bookingPanel, "Bookings");

        // Right side = top bar (north) + current page (center) + footer (south)
        JPanel rightSide = new JPanel(new BorderLayout());
        rightSide.setBackground(PAGE_BG);
        rightSide.add(createTopBar(), BorderLayout.NORTH);
        rightSide.add(contentPanel, BorderLayout.CENTER);
        rightSide.add(createFooter(), BorderLayout.SOUTH);

        add(rightSide, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                saveAndExit();
            }
        });

        showPage("Dashboard");
    }

    private void initializePanels() {

        dashboardPanel = new DashboardPanel(
            hotelSystem.getReportService()
        );

        guestPanel = new GuestPanel(
            hotelSystem.getGuestService(),
            hotelSystem.getBookingService(),
            hotelSystem.getPaymentService()
        );

        roomPanel = new RoomPanel(
            hotelSystem.getRoomService()
        );

        bookingPanel = new BookingPanel(
            hotelSystem.getBookingService(),
            hotelSystem.getGuestService(),
            hotelSystem.getRoomService(),
            hotelSystem.getPaymentService()
        );

        // The receipt's "Received by" line shows the logged-in user
        bookingPanel.setReceivedBy(username);
    }

    // =====================================================================
    //  SIDEBAR
    // =====================================================================
    private JPanel createSidebar() {

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, HerzTheme.WINDOW_HEIGHT));
        sidebar.setBorder(new EmptyBorder(18, 10, 16, 10)); // top, left, bottom, right

        // --- Logo + thin line + WORKSPACE label ---
        sidebar.add(createLogoBlock());
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(createSidebarLine());
        sidebar.add(Box.createVerticalStrut(22));

        JLabel workspace = new JLabel("WORKSPACE");
        workspace.setFont(tracked(font("SansSerif", Font.PLAIN, 9, "Inter"), 0.08f));
        workspace.setForeground(new Color(215, 190, 190));
        workspace.setBorder(new EmptyBorder(0, 10, 0, 0));
        workspace.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(workspace);
        sidebar.add(Box.createVerticalStrut(34));

        // --- Navigation buttons ---
        dashboardButton = new NavButton("Home", "home");
        guestsButton    = new NavButton("Guests", "guests");
        roomsButton     = new NavButton("Rooms", "rooms");
        bookingsButton  = new NavButton("Bookings", "bookings");

        addNavigation(sidebar, dashboardButton, "Dashboard");
        addNavigation(sidebar, guestsButton, "Guests");
        addNavigation(sidebar, roomsButton, "Rooms");
        addNavigation(sidebar, bookingsButton, "Bookings");

        sidebar.add(Box.createVerticalGlue());

        // --- Log out at the bottom ---
        NavButton logoutButton = new NavButton("Logout", "logout");
        logoutButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutButton.addActionListener(e -> logout());
        sidebar.add(logoutButton);

        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(createSidebarLine());
        sidebar.add(Box.createVerticalStrut(14));

        // --- Small text at the very bottom ---
        JLabel place = new JLabel("Herz · Legazpi");
        place.setFont(font("SansSerif", Font.PLAIN, 10, "Inter"));
        place.setForeground(new Color(235, 225, 225));
        place.setBorder(new EmptyBorder(0, 10, 0, 0));
        place.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel version = new JLabel("Management system v1.0");
        version.setFont(font("SansSerif", Font.PLAIN, 8, "Inter"));
        version.setForeground(new Color(170, 130, 130));
        version.setBorder(new EmptyBorder(0, 10, 0, 0));
        version.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(place);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(version);

        return sidebar;
    }

    // Logo image + "erz" + HOTEL (falls back to "Herz" text if the logo file is missing)
    private JPanel createLogoBlock() {
        JPanel logo = new JPanel();
        logo.setOpaque(false);
        logo.setLayout(new BoxLayout(logo, BoxLayout.Y_AXIS));
        logo.setBorder(new EmptyBorder(0, 10, 0, 0));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hotel = new JLabel("HOTEL");
        hotel.setFont(tracked(font("SansSerif", Font.PLAIN, 8, "Inter"), 0.3f));
        hotel.setForeground(Color.WHITE);
        hotel.setAlignmentX(Component.LEFT_ALIGNMENT);

        BufferedImage logoImage = loadLogoImage();
        if (logoImage != null) {
            int logoHeight = LOGO_WIDTH * logoImage.getHeight() / logoImage.getWidth();
            JLabel image = new JLabel(new ImageIcon(scaleImage(logoImage, LOGO_WIDTH, logoHeight)));
            image.setAlignmentY(Component.BOTTOM_ALIGNMENT);

            JLabel erz = new JLabel("erz");
            erz.setFont(font("Serif", Font.PLAIN, 22, "Playfair Display"));
            erz.setForeground(Color.WHITE);
            erz.setBorder(new EmptyBorder(0, 2, 6, 0)); // 3rd number moves "erz" up/down
            erz.setAlignmentY(Component.BOTTOM_ALIGNMENT);

            JPanel row = new JPanel();
            row.setOpaque(false);
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.add(image);
            row.add(erz);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);

            hotel.setBorder(new EmptyBorder(0, 6, 0, 0));
            logo.add(row);
            logo.add(hotel);
        } else {
            JLabel text = new JLabel("Herz");
            text.setFont(font("Serif", Font.ITALIC, 28, "Playfair Display"));
            text.setForeground(Color.WHITE);
            text.setAlignmentX(Component.LEFT_ALIGNMENT);

            hotel.setBorder(new EmptyBorder(0, 12, 0, 0));
            logo.add(text);
            logo.add(hotel);
        }
        return logo;
    }

    // A thin horizontal line for the sidebar
    private JPanel createSidebarLine() {
        JPanel line = new JPanel();
        line.setBackground(SIDEBAR_LINE);
        line.setPreferredSize(new Dimension(1, 1));
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        return line;
    }

    private void addNavigation(
            JPanel sidebar,
            NavButton button,
            String pageName) {

        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        button.addActionListener(e -> showPage(pageName));

        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(NAV_GAP));
    }

    // =====================================================================
    //  TOP BAR: "Hotel operations" on the left, date + user on the right
    // =====================================================================
    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setPreferredSize(new Dimension(100, TOPBAR_HEIGHT));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),   // thin line under the bar
            new EmptyBorder(0, 26, 0, 40)                        // left and right padding
        ));

        JLabel section = new JLabel("Hotel operations");
        section.setFont(font("SansSerif", Font.PLAIN, 12, "Inter"));
        section.setForeground(MUTED);

        // Today's date, written like "MONDAY, 18 NOVEMBER 2026"
        String today = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))
            .toUpperCase();
        JLabel date = new JLabel(today);
        date.setFont(tracked(font("SansSerif", Font.PLAIN, 9, "Inter"), 0.04f));
        date.setForeground(MUTED);

        // Thin vertical line between the date and the user
        JPanel divider = new JPanel();
        divider.setBackground(LINE);
        divider.setPreferredSize(new Dimension(1, 36));
        divider.setMaximumSize(new Dimension(1, 36));

        // User name + role (stacked)
        JLabel name = new JLabel(username); // the logged-in user's name
        name.setFont(font("SansSerif", Font.BOLD, 11, "Inter"));
        name.setForeground(NAVY);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel role = new JLabel(userRole);
        role.setFont(font("SansSerif", Font.PLAIN, 9, "Inter"));
        role.setForeground(MUTED);
        role.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel user = new JPanel();
        user.setOpaque(false);
        user.setLayout(new BoxLayout(user, BoxLayout.Y_AXIS));
        user.add(name);
        user.add(Box.createVerticalStrut(2));
        user.add(role);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(date);
        right.add(Box.createHorizontalStrut(22));
        right.add(divider);
        right.add(Box.createHorizontalStrut(22));
        right.add(user);

        // Wrapper centers the right group vertically inside the bar
        JPanel rightWrapper = new JPanel(new GridBagLayout());
        rightWrapper.setOpaque(false);
        rightWrapper.add(right);

        bar.add(section, BorderLayout.WEST);
        bar.add(rightWrapper, BorderLayout.EAST);
        return bar;
    }

    // =====================================================================
    //  FOOTER: thin line + "Herz / HOTEL ADMINISTRATION"
    // =====================================================================
    private JPanel createFooter() {
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(new EmptyBorder(0, 24, 14, 28));

        JPanel line = new JPanel();
        line.setBackground(LINE);
        line.setPreferredSize(new Dimension(1, 1));
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel text = new JLabel("Herz  /  HOTEL ADMINISTRATION");
        text.setFont(font("SansSerif", Font.PLAIN, 8, "Inter"));
        text.setForeground(MUTED);
        text.setAlignmentX(Component.LEFT_ALIGNMENT);

        footer.add(line);
        footer.add(Box.createVerticalStrut(12));
        footer.add(text);
        return footer;
    }

    // =====================================================================
    //  PAGE SWITCHING (same behavior as before)
    // =====================================================================
    private void showPage(String pageName) {

        cardLayout.show(contentPanel, pageName);

        // Only the current page's button gets the gold marker
        dashboardButton.setActive(false);
        guestsButton.setActive(false);
        roomsButton.setActive(false);
        bookingsButton.setActive(false);

        switch (pageName) {

            case "Dashboard":
                dashboardButton.setActive(true);
                dashboardPanel.refreshDashboard();
                break;

            case "Guests":
                guestsButton.setActive(true);
                guestPanel.refreshTable();
                break;

            case "Rooms":
                roomsButton.setActive(true);
                roomPanel.refreshTable();
                break;

            case "Bookings":
                bookingsButton.setActive(true);
                bookingPanel.refreshTable();
                break;

            default:
                break;
        }
    }

    // =====================================================================
    //  EXIT + LOGOUT
    // =====================================================================
    private void saveAndExit() {

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Save all hotel records before exiting?",
            "Exit Herz Hotel",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (choice == JOptionPane.CANCEL_OPTION
                || choice == JOptionPane.CLOSED_OPTION) {
            return;
        }

        if (choice == JOptionPane.YES_OPTION) {

            try {

                hotelSystem.saveData();

                JOptionPane.showMessageDialog(
                    this,
                    "Hotel records saved successfully."
                );

            } catch (IOException e) {

                JOptionPane.showMessageDialog(
                    this,
                    "Could not save hotel records:\n" + e.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE
                );

                return;
            }
        }

        dispose();
        System.exit(0);
    }

    private void logout() {

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Save your changes and log out?",
            "Log out",
            JOptionPane.YES_NO_CANCEL_OPTION
        );

        if (choice == JOptionPane.CANCEL_OPTION
                || choice == JOptionPane.CLOSED_OPTION) {
            return;
        }

        if (choice == JOptionPane.YES_OPTION) {

            try {

                hotelSystem.saveData();

            } catch (IOException e) {

                JOptionPane.showMessageDialog(
                    this,
                    "Could not save data:\n" + e.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE
                );

                return;
            }
        }

        LoginFrame loginFrame = new LoginFrame();
        loginFrame.setVisible(true);

        dispose();
    }

    // =====================================================================
    //  HELPERS (fonts and images)
    // =====================================================================

    // Uses the nice font if it's installed, otherwise a built-in one
    private static Font font(String fallback, int style, int size, String preferred) {
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (name.equalsIgnoreCase(preferred)) return new Font(preferred, style, size);
        }
        return new Font(fallback, style, size);
    }

    // Adds extra spacing between letters
    private static Font tracked(Font base, float tracking) {
        Map<TextAttribute, Object> attrs = new HashMap<>();
        attrs.put(TextAttribute.TRACKING, tracking);
        return base.deriveFont(attrs);
    }

    // Automatically locates logo.jpg (or any logo.* file) anywhere in the project folder
    private static BufferedImage loadLogoImage() {
        String[] resources = {
            LOGO_RESOURCE, "/logo.jpg", "/assets/logo.jpg", "/images/logo.jpg",
            "/util/logo.jpg", "/gui/logo.png", "/logo.png"
        };
        for (String res : resources) {
            BufferedImage img = loadImage(res, null);
            if (img != null) return img;
        }

        File found = findLogoFile(new File(System.getProperty("user.dir")), 4);
        if (found == null && new File("..").exists()) {
            found = findLogoFile(new File(".."), 4);
        }
        if (found != null) {
            BufferedImage img = loadImage(null, found.getPath());
            if (img != null) return img;

            Image raw = Toolkit.getDefaultToolkit().getImage(found.getAbsolutePath());
            MediaTracker tracker = new MediaTracker(new Canvas());
            tracker.addImage(raw, 0);
            try {
                tracker.waitForID(0);
                if (raw.getWidth(null) > 0 && raw.getHeight(null) > 0) {
                    BufferedImage buf = new BufferedImage(
                        raw.getWidth(null), raw.getHeight(null), BufferedImage.TYPE_INT_ARGB
                    );
                    Graphics2D g2 = buf.createGraphics();
                    g2.drawImage(raw, 0, 0, null);
                    g2.dispose();
                    return buf;
                }
            } catch (InterruptedException ignored) { }
        }
        return null;
    }

    private static File findLogoFile(File dir, int depth) {
        if (depth < 0 || dir == null || !dir.isDirectory()) return null;
        File[] children = dir.listFiles();
        if (children == null) return null;

        for (File f : children) {
            if (f.isFile()) {
                String lower = f.getName().toLowerCase();
                if (lower.startsWith("logo")
                        && (lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                            || lower.endsWith(".png") || lower.endsWith(".gif"))) {
                    return f;
                }
            }
        }
        for (File f : children) {
            if (f.isDirectory() && !f.getName().startsWith(".") && !f.getName().equals("bin")) {
                File match = findLogoFile(f, depth - 1);
                if (match != null) return match;
            }
        }
        return null;
    }

    // Loads an image from the classpath or project folder; returns null if not found
    private static BufferedImage loadImage(String resource, String file) {
        try {
            if (resource != null) {
                URL url = MainFrame.class.getResource(resource);
                if (url != null) return ImageIO.read(url);
            }
            if (file != null) {
                File f = new File(file);
                if (f.exists()) return ImageIO.read(f);
            }
        } catch (Exception ignored) { }
        return null;
    }

    // Resizes an image smoothly (in small steps so it stays sharp)
    private static BufferedImage scaleImage(BufferedImage src, int w, int h) {
        BufferedImage result = src;
        int cw = src.getWidth(), ch = src.getHeight();
        while (cw > w * 2) {
            cw /= 2;
            ch /= 2;
            result = drawScaled(result, cw, ch);
        }
        return drawScaled(result, w, h);
    }

    private static BufferedImage drawScaled(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();
        return out;
    }

    // =====================================================================
    //  CUSTOM SIDEBAR BUTTON: white rounded button with an icon
    // =====================================================================
    private static class NavButton extends JButton {

        private boolean active = false; // true for the page being shown

        NavButton(String text, String iconType) {
            super(text);
            setIcon(new NavIcon(iconType));
            setIconTextGap(12);
            setFont(font("SansSerif", Font.BOLD, 11, "Inter"));
            setForeground(SIDEBAR_BG);               // burgundy text
            setHorizontalAlignment(SwingConstants.LEFT);
            setContentAreaFilled(false);             // we paint the background ourselves
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setBorder(new EmptyBorder(0, 16, 0, 10)); // left padding before the icon
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(160, NAV_HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, NAV_HEIGHT));
        }

        void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // White rounded background (slightly gray when the mouse is over it)
            g2.setColor(getModel().isRollover() ? new Color(244, 242, 242) : Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

            // Gold marker on the left for the current page
            if (active) {
                g2.setColor(GOLD);
                g2.fillRoundRect(0, 9, 3, getHeight() - 18, 3, 3);
            }
            g2.dispose();

            super.paintComponent(g); // draws the icon and text on top
        }
    }

    // =====================================================================
    //  SIMPLE LINE ICONS (drawn with code, so no image files are needed)
    // =====================================================================
    private static class NavIcon implements Icon {

        private static final Color ICON_COLOR = new Color(125, 125, 135);
        private final String type;

        NavIcon(String type) { this.type = type; }

        @Override public int getIconWidth()  { return 18; }
        @Override public int getIconHeight() { return 18; }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.setColor(ICON_COLOR);
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            switch (type) {
                case "home": {            // house
                    Path2D house = new Path2D.Float();
                    house.moveTo(2, 9);   house.lineTo(9, 2);   house.lineTo(16, 9);
                    house.moveTo(4, 8);   house.lineTo(4, 16);  house.lineTo(14, 16);  house.lineTo(14, 8);
                    house.moveTo(8, 16);  house.lineTo(8, 11);  house.lineTo(10, 11);  house.lineTo(10, 16);
                    g2.draw(house);
                    break;
                }
                case "guests":            // two people
                    g2.drawOval(4, 3, 6, 6);
                    g2.drawArc(1, 11, 12, 10, 0, 180);
                    g2.drawOval(11, 5, 4, 4);
                    g2.drawArc(11, 12, 6, 8, 0, 180);
                    break;
                case "rooms":             // bed
                    g2.drawLine(2, 4, 2, 15);
                    g2.drawRoundRect(2, 9, 14, 5, 2, 2);
                    g2.drawRect(4, 6, 4, 3);
                    g2.drawLine(16, 14, 16, 16);
                    break;
                case "bookings":          // calendar
                    g2.drawRoundRect(2, 3, 14, 13, 3, 3);
                    g2.drawLine(2, 7, 16, 7);
                    g2.drawLine(6, 1, 6, 4);
                    g2.drawLine(12, 1, 12, 4);
                    g2.drawLine(5, 11, 7, 11);
                    g2.drawLine(9, 11, 11, 11);
                    break;
                case "payments":          // card
                    g2.drawRoundRect(1, 4, 16, 11, 3, 3);
                    g2.drawLine(1, 8, 17, 8);
                    g2.drawLine(4, 12, 8, 12);
                    break;
                case "logout": {          // door on the right + arrow pointing left (out of the door)
                    Path2D door = new Path2D.Float();
                    door.moveTo(11, 2); door.lineTo(15, 2); door.lineTo(15, 16); door.lineTo(11, 16);
                    door.moveTo(11, 9); door.lineTo(2, 9);
                    door.moveTo(5, 6);  door.lineTo(2, 9);  door.lineTo(5, 12);
                    g2.draw(door);
                    break;
                }
                default:
                    break;
            }
            g2.dispose();
        }
    }
}