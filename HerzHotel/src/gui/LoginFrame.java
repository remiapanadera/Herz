package gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class LoginFrame extends JFrame {

    // ---------- Colors used across the screen ----------
    private static final Color PAGE_BG      = new Color(247, 245, 241); // cream background
    private static final Color NAVY         = new Color(22, 40, 64);    // main text
    private static final Color GOLD         = new Color(140, 110, 60);  // small accents
    private static final Color MUTED        = new Color(120, 120, 120); // secondary text
    private static final Color FIELD_BORDER = new Color(232, 228, 222); // input outlines
    private static final Color BTN_COLOR    = new Color(74, 0, 0);      // burgundy button
    private static final Color BTN_HOVER    = new Color(100, 10, 10);   // button on mouse hover

    // ---------- Photo location ----------
    private static final String IMAGE_RESOURCE = "/gui/mayon.jpg";
    private static final String IMAGE_FILE     = "src/gui/mayon.jpg";

    // ---------- Logo image ----------
    private static final String LOGO_RESOURCE = "/gui/logo.jpg";
    private static final int LOGO_WIDTH       = 80; // logo width in pixels

    // ---------- Size of the photo panel (change this to resize it) ----------
    private static final int PHOTO_WIDTH = 580; // window is 1100 wide, so the form gets 460

    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        // Basic window settings
        setTitle("Herz Hotel | Admin Login");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // center on screen
        setResizable(false);

        // Photo on the left (fixed width), login form fills the rest
        JPanel brandPanel = createBrandPanel();
        brandPanel.setPreferredSize(new Dimension(PHOTO_WIDTH, 700));

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(brandPanel, BorderLayout.WEST);
        mainPanel.add(createLoginPanel(), BorderLayout.CENTER);
        add(mainPanel);
    }

    // =====================================================================
    //  LEFT SIDE: photo with logo and tagline
    // =====================================================================
    private JPanel createBrandPanel() {
        BackgroundPanel panel = new BackgroundPanel(loadImage(IMAGE_RESOURCE, IMAGE_FILE));
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(28, 30, 28, 38));

        // --- Logo at the top-left ---
        JPanel logo = new JPanel();
        logo.setOpaque(false);
        logo.setLayout(new BoxLayout(logo, BoxLayout.Y_AXIS));

        JLabel logoMain = new JLabel("Herz");
        logoMain.setFont(font("Serif", Font.ITALIC, 34, "Playfair Display"));
        logoMain.setForeground(Color.WHITE);
        logoMain.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel logoSub = new JLabel("HOTEL");
        logoSub.setFont(tracked(font("SansSerif", Font.PLAIN, 9, "Inter"), 0.3f)); // spaced letters
        logoSub.setForeground(Color.WHITE);
        logoSub.setBorder(new EmptyBorder(0, 14, 0, 0));
        logoSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        logo.add(logoMain);
        logo.add(logoSub);

        // If a logo image exists: [logo image] + "erz" side by side, with HOTEL below
        BufferedImage logoImage = loadLogoImage();
        if (logoImage != null) {
            int logoHeight = LOGO_WIDTH * logoImage.getHeight() / logoImage.getWidth(); // keep proportions
            JLabel logoLabel = new JLabel(new ImageIcon(scaleImage(logoImage, LOGO_WIDTH, logoHeight)));
            logoLabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);

            // The "erz" text that sits right next to the logo image
            JLabel erz = new JLabel("erz");
            erz.setFont(font("Serif", Font.PLAIN, 32, "Playfair Display"));
            erz.setForeground(Color.WHITE);
            erz.setBorder(new EmptyBorder(0, -2, 15, 0)); // 3rd number moves "erz" up (bigger) or down (smaller)
            erz.setAlignmentY(Component.BOTTOM_ALIGNMENT);

            // Put the image and "erz" in one row
            JPanel row = new JPanel();
            row.setOpaque(false);
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.add(logoLabel);
            row.add(erz);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);

            logoSub.setBorder(new EmptyBorder(0, 15, 0, 0)); // 2nd number moves HOTEL left/right
            logo.removeAll();   // remove the text logo
            logo.add(row);      // image + erz
            logo.add(logoSub);  // HOTEL below
        }

        // --- Tagline at the bottom-left ---
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

        // Small gold line above the tagline
        JPanel accent = new JPanel();
        accent.setBackground(new Color(200, 170, 110));
        accent.setMaximumSize(new Dimension(38, 2));
        accent.setPreferredSize(new Dimension(38, 2));
        accent.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tagline = new JLabel("Stay where the heart is");
        tagline.setFont(font("Serif", Font.PLAIN, 38, "Playfair Display"));
        tagline.setForeground(Color.WHITE);
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tagSub = new JLabel("A thoughtful space for the people behind every great stay.");
        tagSub.setFont(font("SansSerif", Font.PLAIN, 13, "Inter"));
        tagSub.setForeground(new Color(245, 240, 240));
        tagSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel city = new JLabel("Legazpi City");
        city.setFont(font("SansSerif", Font.PLAIN, 10, "Inter"));
        city.setForeground(Color.WHITE);
        city.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Stack them from top to bottom with spacing in between
        bottom.add(accent);
        bottom.add(Box.createVerticalStrut(22));
        bottom.add(tagline);
        bottom.add(Box.createVerticalStrut(4));
        bottom.add(tagSub);
        bottom.add(Box.createVerticalStrut(60));
        bottom.add(city);

        panel.add(logo, BorderLayout.NORTH);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    // =====================================================================
    //  RIGHT SIDE: login form
    // =====================================================================
    private JPanel createLoginPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(PAGE_BG);
        outer.setBorder(new EmptyBorder(0, 60, 0, 66)); // top, left, bottom, right margins

        // The form stacks everything vertically
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        // --- Title area ---
        JLabel subtitle = new JLabel("ADMINISTRATOR ACCESS");
        subtitle.setFont(tracked(font("SansSerif", Font.PLAIN, 9, "Inter"), 0.04f));
        subtitle.setForeground(GOLD);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel heading = new JLabel("Welcome back.");
        heading.setFont(font("Serif", Font.PLAIN, 40, "Playfair Display"));
        heading.setForeground(NAVY);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- Username field ---
        JLabel usernameLabel = new JLabel("Username");
        styleLabel(usernameLabel);
        usernameField = new JTextField();
        RoundedPanel usernameBox = fieldBox(usernameField, null);

        // --- Password field with a Show/Hide button inside ---
        JLabel passwordLabel = new JLabel("Password");
        styleLabel(passwordLabel);
        passwordField = new JPasswordField();
        char defaultEcho = passwordField.getEchoChar(); // the dot character

        JButton showButton = new JButton("Show");
        showButton.setFont(font("SansSerif", Font.PLAIN, 10, "Inter"));
        showButton.setForeground(MUTED);
        showButton.setContentAreaFilled(false);
        showButton.setBorderPainted(false);
        showButton.setFocusPainted(false);
        showButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showButton.addActionListener(e -> {
            // echo char 0 means "show the real text"
            boolean hidden = passwordField.getEchoChar() != 0;
            passwordField.setEchoChar(hidden ? (char) 0 : defaultEcho);
            showButton.setText(hidden ? "Hide" : "Show");
        });
        RoundedPanel passwordBox = fieldBox(passwordField, showButton);

        // --- Row: "Forgot password?" on the right ---
        JLabel forgot = new JLabel("Forgot password?");
        forgot.setFont(font("SansSerif", Font.PLAIN, 11, "Inter"));
        forgot.setForeground(GOLD);
        forgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgot.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(LoginFrame.this,
                    "Please contact your hotel administrator to reset your password.",
                    "Forgot Password", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JPanel optionsRow = new JPanel(new BorderLayout());
        optionsRow.setOpaque(false);
        optionsRow.add(forgot, BorderLayout.EAST);
        optionsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        optionsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- Sign in button ---
        JButton signIn = createSignInButton();
        signIn.addActionListener(e -> handleLogin());

        // --- Small note under the button (centered) ---
        JLabel secure = new JLabel("Secure access for authorized hotel staff only.");
        secure.setFont(font("SansSerif", Font.PLAIN, 10, "Inter"));
        secure.setForeground(MUTED);
        JPanel secureRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        secureRow.setOpaque(false);
        secureRow.add(secure);
        secureRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        secureRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- Add everything to the form in order (glue = flexible space to center it) ---
        form.add(Box.createVerticalStrut(145)); // space above the form (bigger = form sits lower)
        form.add(subtitle);
        form.add(Box.createVerticalStrut(4));
        form.add(heading);
        form.add(Box.createVerticalStrut(36));
        form.add(usernameLabel);
        form.add(Box.createVerticalStrut(8));
        form.add(usernameBox);
        form.add(Box.createVerticalStrut(20));
        form.add(passwordLabel);
        form.add(Box.createVerticalStrut(8));
        form.add(passwordBox);
        form.add(Box.createVerticalStrut(24));
        form.add(optionsRow);
        form.add(Box.createVerticalStrut(22));
        form.add(signIn);
        form.add(Box.createVerticalStrut(18));
        form.add(secureRow);
        form.add(Box.createVerticalGlue());

        // --- Footer at the bottom: divider line + help text + copyright ---
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(new EmptyBorder(0, 0, 28, 0));

        JSeparator divider = new JSeparator();
        divider.setForeground(FIELD_BORDER);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel needAccess = new JLabel("Need access? Contact your hotel administrator.");
        needAccess.setFont(font("SansSerif", Font.PLAIN, 11, "Inter"));
        needAccess.setForeground(MUTED);
        needAccess.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel copyright = new JLabel("\u00A9 2026 Herz");
        copyright.setFont(font("SansSerif", Font.PLAIN, 9, "Inter"));
        copyright.setForeground(MUTED);
        copyright.setAlignmentX(Component.LEFT_ALIGNMENT);

        footer.add(divider);
        footer.add(Box.createVerticalStrut(18));
        footer.add(needAccess);
        footer.add(Box.createVerticalStrut(22));
        footer.add(copyright);

        outer.add(form, BorderLayout.CENTER);
        outer.add(footer, BorderLayout.SOUTH);
        return outer;
    }

    // =====================================================================
    //  HELPERS: small methods that build repeated pieces of the UI
    // =====================================================================

    // Styles the small "Username" / "Password" labels
    private void styleLabel(JLabel label) {
        label.setFont(font("SansSerif", Font.PLAIN, 11, "Inter"));
        label.setForeground(NAVY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    // Wraps a text field in a white rounded box (optionally with a component on the right)
    private RoundedPanel fieldBox(JTextField field, JComponent east) {
        field.setBorder(new EmptyBorder(0, 0, 0, 0)); // remove default border, the box draws its own
        field.setOpaque(false);
        field.setForeground(NAVY);
        field.setFont(font("SansSerif", Font.PLAIN, 13, "Inter"));
        field.setCaretColor(NAVY);

        RoundedPanel box = new RoundedPanel(10, Color.WHITE, FIELD_BORDER);
        box.setLayout(new BorderLayout());
        box.setBorder(new EmptyBorder(0, 12, 0, 12));
        box.add(field, BorderLayout.CENTER);
        if (east != null) box.add(east, BorderLayout.EAST);
        box.setPreferredSize(new Dimension(100, 39));
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 39)); // full width, 39px tall
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        return box;
    }

    // Burgundy rounded "Sign in →" button that gets lighter on hover
    private JButton createSignInButton() {
        JButton button = new JButton("Sign in  \u2192") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? BTN_HOVER : BTN_COLOR);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g); // draws the text on top
            }
        };
        button.setFont(font("SansSerif", Font.BOLD, 12, "Inter"));
        button.setForeground(Color.WHITE);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(100, 36));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        getRootPane().setDefaultButton(button); // pressing Enter clicks this button
        return button;
    }

    // Uses the nice font if it's installed, otherwise a built-in one
    private static Font font(String fallback, int style, int size, String preferred) {
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (name.equalsIgnoreCase(preferred)) return new Font(preferred, style, size);
        }
        return new Font(fallback, style, size);
    }

    // Adds extra spacing between letters (used for "HOTEL" and "ADMINISTRATOR ACCESS")
    private static Font tracked(Font base, float tracking) {
        Map<TextAttribute, Object> attrs = new HashMap<>();
        attrs.put(TextAttribute.TRACKING, tracking);
        return base.deriveFont(attrs);
    }

    // Resizes an image smoothly. Shrinking in small steps keeps it sharp instead of blurry.
    private static BufferedImage scaleImage(BufferedImage src, int w, int h) {
        BufferedImage result = src;
        int cw = src.getWidth(), ch = src.getHeight();
        while (cw > w * 2) {          // halve the size until it is close to the target
            cw /= 2;
            ch /= 2;
            result = drawScaled(result, cw, ch);
        }
        return drawScaled(result, w, h); // final step to the exact size
    }

    // Draws an image at a new size using high-quality settings (keeps transparency)
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
                URL url = LoginFrame.class.getResource(resource);
                if (url != null) return ImageIO.read(url);
            }
            if (file != null) {
                File f = new File(file);
                if (f.exists()) return ImageIO.read(f);
            }
        } catch (Exception ignored) { }
        return null;
    }

    // =====================================================================
    //  CUSTOM PANELS
    // =====================================================================

    // A panel with rounded corners, a fill color, and a thin outline
    private static class RoundedPanel extends JPanel {
        private final int arc;
        private final Color fill, stroke;

        RoundedPanel(int arc, Color fill, Color stroke) {
            this.arc = arc; this.fill = fill; this.stroke = stroke;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(stroke);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // A panel that draws the photo as its background, scaled to fill the whole area.
    // It also adds dark fades so the white text stays readable.
    private static class BackgroundPanel extends JPanel {
        private final BufferedImage image;

        BackgroundPanel(BufferedImage image) { this.image = image; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int w = getWidth(), h = getHeight();

            if (image != null) {
                // Scale the photo so it covers the panel, then center it (extra edges get cropped)
                double scale = Math.max((double) w / image.getWidth(), (double) h / image.getHeight());
                int iw = (int) (image.getWidth() * scale), ih = (int) (image.getHeight() * scale);
                g2.drawImage(image, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
            } else {
                // No photo found: draw a simple sky-to-lake gradient instead
                g2.setPaint(new LinearGradientPaint(0, 0, 0, h,
                    new float[]{0f, 0.45f, 0.62f, 1f},
                    new Color[]{new Color(40, 90, 170), new Color(150, 170, 200),
                                new Color(150, 120, 100), new Color(90, 70, 110)}));
                g2.fillRect(0, 0, w, h);
            }

            // Dark fade at the bottom (behind the tagline)
            g2.setPaint(new GradientPaint(0, h * 0.5f, new Color(30, 10, 40, 0), 0, h, new Color(30, 10, 40, 170)));
            g2.fillRect(0, (int) (h * 0.5), w, h - (int) (h * 0.5));
            // Light fade at the top (behind the logo)
            g2.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 70), 0, h * 0.2f, new Color(0, 0, 0, 0)));
            g2.fillRect(0, 0, w, (int) (h * 0.2));
            g2.dispose();
        }
    }

    // =====================================================================
    //  ACCOUNTS: each row is { username, password }.
    //  To add or change an account, edit or copy one of these lines.
    // =====================================================================
    private static final String[][] ACCOUNTS = {
        { "Trisha Dajac",  "admin123" },
        { "Heini Llaneta",   "admin12" },   
        { "Remia Panadera",    "admin1" }
    };

    // True if the typed username + password match one of the accounts above
    private static boolean isValidLogin(String username, String password) {
        for (String[] account : ACCOUNTS) {
            if (account[0].equals(username) && account[1].equals(password)) {
                return true;
            }
        }
        return false;
    }

    // =====================================================================
    //  LOGIN LOGIC
    // =====================================================================
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (isValidLogin(username, password)) {
            JOptionPane.showMessageDialog(this, "Welcome to Herz Hotel!",
                "Login Successful", JOptionPane.INFORMATION_MESSAGE);

            MainFrame mainFrame = new MainFrame(username); // open the main window
            mainFrame.setVisible(true);
            dispose();                              // close the login window
        } else {
            JOptionPane.showMessageDialog(this, "Invalid username or password.",
                "Login Failed", JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");  // clear the wrong password
            passwordField.requestFocus();
        }
    }
}