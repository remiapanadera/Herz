package gui;

import exception.DuplicateRecordException;
import exception.InvalidBookingException;
import model.Booking;
import model.Guest;
import service.BookingService;
import service.GuestService;
import util.HerzTheme;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuestPanel extends JPanel {

    // ---- Palette taken from the design ----
    private static final Color PAGE_BG      = new Color(0xF6, 0xF4, 0xF0);
    private static final Color CARD_BORDER  = new Color(0xE3, 0xE1, 0xDD);
    private static final Color FIELD_BG     = new Color(0xF4, 0xF5, 0xF5);
    private static final Color FIELD_BORDER = new Color(0xDD, 0xE0, 0xE2);
    private static final Color INK          = new Color(0x1B, 0x2A, 0x3A);
    private static final Color MUTED        = new Color(0x6B, 0x77, 0x83);
    private static final Color HINT         = new Color(0x9A, 0xA3, 0xAB);
    private static final Color BURGUNDY     = new Color(0x4A, 0x04, 0x04);
    private static final Color GOLD         = new Color(0x9A, 0x6B, 0x2F);
    private static final Color ROW_SELECTED = new Color(0xF3, 0xE4, 0xE4);
    private static final Color ROW_LINE     = new Color(0xE6, 0xE6, 0xE6);
    private static final Color HEADER_BG    = new Color(0xF1, 0xF2, 0xF2);
    private static final Color DANGER       = new Color(0xA5, 0x3B, 0x33);
    private static final Color DANGER_LINE  = new Color(0xEB, 0xCF, 0xC9);

    private static final int PAGE_SIZE = 8;

    private static final DateTimeFormatter DAY_FMT  = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter STAMP_FMT = DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH);
    private static final String MUTED_HEX = "#6B7783";

    private final GuestService guestService;
    private final BookingService bookingService;

    private JTable guestTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> filterBox;
    private JLabel countLabel;
    private JLabel showingLabel;
    private JButton checkInButton;
    private JButton prevButton;
    private JButton nextButton;
    private JLabel pageLabel;

    // One row per guest + booking (a guest with no active booking gets a single row)
    private final List<GuestRow> filtered = new ArrayList<>();
    private int currentPage = 0;

    private static class GuestRow {
        final Guest guest;
        final Booking booking; // null when the guest has no active booking

        GuestRow(Guest guest, Booking booking) {
            this.guest = guest;
            this.booking = booking;
        }
    }

    public GuestPanel(GuestService guestService, BookingService bookingService) {

        if (guestService == null || bookingService == null) {
            throw new IllegalArgumentException("GuestService and BookingService are required.");
        }

        this.guestService = guestService;
        this.bookingService = bookingService;

        setLayout(new BorderLayout(0, 14));
        setBackground(PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(26, 26, 26, 26));

        add(createHeader(), BorderLayout.NORTH);
        add(createCard(), BorderLayout.CENTER);

        refreshTable();

        // The 2:00 PM check-in time can arrive while this page is open,
        // so the Check in button is re-evaluated every 30 seconds.
        new javax.swing.Timer(30_000, e -> updateCheckInButton()).start();
    }

    // =====================================================================
    //  Page layout
    // =====================================================================

    private JPanel createHeader() {

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel crumb = new JLabel("HOTEL OPERATIONS");
        crumb.setFont(new Font("SansSerif", Font.PLAIN, 10));
        crumb.setForeground(GOLD);
        crumb.setAlignmentX(LEFT_ALIGNMENT);

        JLabel title = new JLabel("Guest Management");
        title.setFont(new Font("Serif", Font.PLAIN, 34));
        title.setForeground(INK);
        title.setAlignmentX(LEFT_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        header.add(crumb);
        header.add(Box.createVerticalStrut(18));
        header.add(title);
        header.add(Box.createVerticalStrut(6));

        return header;
    }

    private JPanel createCard() {

        JPanel card = new JPanel(new BorderLayout(0, 14));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(CARD_BORDER, 10, 1),
            BorderFactory.createEmptyBorder(18, 16, 14, 16)
        ));

        // --- Card title row ---
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel cardTitle = new JLabel("Guest Directory");
        cardTitle.setFont(HerzTheme.HEADING_FONT.deriveFont(Font.BOLD, 15f));
        cardTitle.setForeground(INK);

        countLabel = new JLabel("0 guest profiles");
        countLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        countLabel.setForeground(MUTED);

        titleRow.add(cardTitle, BorderLayout.WEST);
        titleRow.add(countLabel, BorderLayout.EAST);

        // --- Search row ---
        JPanel searchRow = new JPanel(new BorderLayout(12, 0));
        searchRow.setOpaque(false);

        searchField = new PlaceholderField("Search by name, email, phone or room...");
        searchField.setPreferredSize(new Dimension(300, 36));
        searchField.setBackground(FIELD_BG);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(FIELD_BG, 6, 1),
            BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        searchField.addActionListener(e -> resetAndRefresh());
        searchField.getDocument().addDocumentListener(new SimpleDocListener(this::resetAndRefresh));

        filterBox = new JComboBox<>(new String[] {
            "All guests", "Missing email", "Missing phone"
        });
        filterBox.setFont(new Font("SansSerif", Font.BOLD, 11));
        filterBox.setBackground(Color.WHITE);
        filterBox.setPreferredSize(new Dimension(190, 36));
        filterBox.addActionListener(e -> resetAndRefresh());

        JButton editButton = styledButton("Edit", ButtonKind.OUTLINE);
        editButton.setPreferredSize(new Dimension(70, 36));
        editButton.addActionListener(e -> editGuest());

        // Only advance bookings use this button; walk-ins are checked in automatically
        checkInButton = styledButton("Check in", ButtonKind.PRIMARY);
        checkInButton.setPreferredSize(new Dimension(96, 36));
        checkInButton.addActionListener(e -> checkInSelected());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(filterBox);
        right.add(editButton);
        right.add(checkInButton);

        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(right, BorderLayout.EAST);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.add(titleRow);
        top.add(Box.createVerticalStrut(14));
        top.add(searchRow);

        card.add(top, BorderLayout.NORTH);
        card.add(createTable(), BorderLayout.CENTER);
        card.add(createFooter(), BorderLayout.SOUTH);

        return card;
    }

    private JScrollPane createTable() {

        String[] columns = {
            "Guest name", "Contact number", "Email", "Address",
            "Room", "Booking type", "Check-in", "Check-out"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        guestTable = new JTable(tableModel);
        guestTable.setFont(new Font("SansSerif", Font.PLAIN, 11));
        guestTable.setForeground(INK);
        guestTable.setRowHeight(48);
        guestTable.setShowVerticalLines(false);
        guestTable.setShowHorizontalLines(true);
        guestTable.setGridColor(ROW_LINE);
        guestTable.setIntercellSpacing(new Dimension(0, 1));
        guestTable.setSelectionBackground(ROW_SELECTED);
        guestTable.setSelectionForeground(INK);
        guestTable.setFillsViewportHeight(true);
        guestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        guestTable.setToolTipText("Double-click a guest to edit");

        // The list keeps the order of the records: no sorting by column
        guestTable.setAutoCreateRowSorter(false);
        guestTable.setRowSorter(null);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < guestTable.getColumnCount(); i++) {
            guestTable.getColumnModel().getColumn(i).setCellRenderer(center);
        }

        // Column widths (the date columns hold two lines: standard time + actual time)
        int[] widths = {100, 112, 118, 88, 42, 106, 126, 134};
        for (int i = 0; i < widths.length; i++) {
            guestTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Re-check the Check in button whenever another row is selected
        guestTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateCheckInButton();
            }
        });

        JTableHeader th = guestTable.getTableHeader();
        th.setReorderingAllowed(false);
        th.setPreferredSize(new Dimension(0, 44));
        th.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(new Font("SansSerif", Font.PLAIN, 10));
                l.setForeground(MUTED);
                l.setBackground(HEADER_BG);
                l.setBorder(BorderFactory.createEmptyBorder());
                return l;
            }
        });

        guestTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && guestTable.getSelectedRow() != -1) {
                    editGuest();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(guestTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private JPanel createFooter() {

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        showingLabel = new JLabel("Showing 0 of 0 guests");
        showingLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        showingLabel.setForeground(MUTED);

        prevButton = pagerButton("\u2039");
        prevButton.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                refreshTable();
            }
        });

        nextButton = pagerButton("\u203A");
        nextButton.addActionListener(e -> {
            if (currentPage < pageCount() - 1) {
                currentPage++;
                refreshTable();
            }
        });

        pageLabel = new JLabel("1", SwingConstants.CENTER);
        pageLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        pageLabel.setForeground(INK);
        pageLabel.setPreferredSize(new Dimension(44, 32));
        pageLabel.setBorder(new RoundedBorder(CARD_BORDER, 6, 1));

        JPanel pager = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        pager.setOpaque(false);
        pager.add(prevButton);
        pager.add(pageLabel);
        pager.add(nextButton);

        footer.add(showingLabel, BorderLayout.WEST);
        footer.add(pager, BorderLayout.EAST);

        return footer;
    }

    private JButton pagerButton(String text) {
        JButton b = styledButton(text, ButtonKind.OUTLINE);
        b.setPreferredSize(new Dimension(32, 32));
        return b;
    }

    // =====================================================================
    //  Actions
    // =====================================================================

    private void editGuest() {

        Guest selected = getSelectedGuest();

        if (selected == null) {
            showError("Please select a guest to edit.");
            return;
        }

        GuestDialog dialog = new GuestDialog(selected, selected.getGuestId());
        dialog.setVisible(true);

        Guest updated = dialog.getResult();

        if (updated == null) {
            return;
        }

        try {
            guestService.updateGuest(selected.getGuestId(), updated);
            refreshTable();

            JOptionPane.showMessageDialog(this, "Guest updated successfully.");

        } catch (DuplicateRecordException | IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private GuestRow getSelectedRow() {

        int row = guestTable.getSelectedRow();

        if (row == -1) {
            return null;
        }

        int index = currentPage * PAGE_SIZE + guestTable.convertRowIndexToModel(row);

        return index < filtered.size() ? filtered.get(index) : null;
    }

    private Guest getSelectedGuest() {
        GuestRow row = getSelectedRow();
        return row == null ? null : row.guest;
    }

    // =====================================================================
    //  Check-in (advance bookings only)
    // =====================================================================

    /** Checks in the selected advance booking, recording the actual check-in time. */
    private void checkInSelected() {

        GuestRow row = getSelectedRow();

        if (row == null || row.booking == null) {
            showError("Please select a guest with a booking to check in.");
            return;
        }

        try {
            bookingService.checkInBooking(row.booking.getBookingId());
            refreshTable();

            JOptionPane.showMessageDialog(this,
                row.guest.getFullName() + " checked in to room "
                + row.booking.getRoomNumber() + ".");

        } catch (InvalidBookingException e) {
            showError(e.getMessage());
            refreshTable();
        }
    }

    /** Enables the Check in button only when the selected guest is eligible, and says why not otherwise. */
    private void updateCheckInButton() {

        if (checkInButton == null) {
            return;
        }

        GuestRow row = getSelectedRow();
        boolean eligible = false;
        String tip;

        if (row == null || row.booking == null) {
            tip = "Select a guest with a booking.";
        } else if (row.booking.isWalkIn()) {
            tip = "Walk-in guests are checked in automatically.";
        } else if ("Checked In".equalsIgnoreCase(row.booking.getBookingStatus())) {
            tip = "This guest is already checked in.";
        } else if ("Checked Out".equalsIgnoreCase(row.booking.getBookingStatus())) {
            tip = "This guest has already checked out.";
        } else if (bookingService.isEligibleForCheckIn(row.booking)) {
            eligible = true;
            tip = "Check this guest in now.";
        } else if (LocalDate.now().equals(row.booking.getCheckInDate())) {
            tip = "Check-in opens at " + Booking.STANDARD_CHECK_IN_TIME.format(TIME_FMT) + ".";
        } else {
            tip = "Check-in opens on " + row.booking.getCheckInDate().format(DAY_FMT)
                + " at " + Booking.STANDARD_CHECK_IN_TIME.format(TIME_FMT) + ".";
        }

        checkInButton.setEnabled(eligible);
        checkInButton.setToolTipText(tip);
    }

    // =====================================================================
    //  Table data
    // =====================================================================

    private void resetAndRefresh() {
        currentPage = 0;
        refreshTable();
    }

    private int pageCount() {
        return Math.max(1, (int) Math.ceil(filtered.size() / (double) PAGE_SIZE));
    }

    public void refreshTable() {

        if (tableModel == null) {
            return;
        }

        String keyword = searchField == null ? "" : searchField.getText().trim();
        String filter = filterBox == null ? "All guests" : filterBox.getSelectedItem().toString();

        filtered.clear();

        String search = keyword.toLowerCase();
        List<Booking> allBookings = bookingService.getAllBookings();

        // Guests come from the guest records; their stays come from confirmed bookings.
        for (Guest g : guestService.getAllGuests()) {

            boolean noEmail = g.getEmail() == null || g.getEmail().isBlank();
            boolean noPhone = g.getPhoneNumber() == null || g.getPhoneNumber().isBlank();

            if (filter.equals("Missing email") && !noEmail) continue;
            if (filter.equals("Missing phone") && !noPhone) continue;

            List<Booking> stays = new ArrayList<>();
            for (Booking b : allBookings) {
                boolean cancelled = "Cancelled".equalsIgnoreCase(b.getBookingStatus());
                if (!cancelled && b.getGuestId().equalsIgnoreCase(g.getGuestId())) {
                    stays.add(b);
                }
            }

            if (stays.isEmpty()) {
                if (matches(g, null, search)) {
                    filtered.add(new GuestRow(g, null));
                }
                continue;
            }

            for (Booking b : stays) {
                if (matches(g, b, search)) {
                    filtered.add(new GuestRow(g, b));
                }
            }
        }

        if (currentPage >= pageCount()) {
            currentPage = pageCount() - 1;
        }

        tableModel.setRowCount(0);

        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filtered.size());

        for (int i = start; i < end; i++) {
            GuestRow row = filtered.get(i);
            Guest g = row.guest;
            Booking b = row.booking;

            tableModel.addRow(new Object[] {
                g.getFullName(),
                g.getPhoneNumber(),
                g.getEmail(),
                g.getAddress(),
                b == null ? "\u2014" : b.getRoomNumber(),
                b == null ? "\u2014" : b.getBookingType(),
                b == null ? "\u2014" : checkInText(b),
                b == null ? "\u2014" : checkOutText(b)
            });
        }

        int total = filtered.size();

        // Count guests (a guest with two stays has two rows but is one profile)
        java.util.Set<String> guestIds = new java.util.HashSet<>();
        for (GuestRow row : filtered) {
            guestIds.add(row.guest.getGuestId());
        }
        int profiles = guestIds.size();

        countLabel.setText(profiles + (profiles == 1 ? " guest profile" : " guest profiles"));
        showingLabel.setText(total == 0
            ? "No guests found"
            : "Showing " + (start + 1) + "\u2013" + end + " of " + total
              + (total == 1 ? " guest record" : " guest records"));
        pageLabel.setText(String.valueOf(currentPage + 1));
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage < pageCount() - 1);

        updateCheckInButton();
    }

    /** Search by name, email, phone, room number or booking type. */
    private boolean matches(Guest g, Booking b, String search) {

        if (search.isEmpty()) {
            return true;
        }

        return g.getGuestId().toLowerCase().contains(search)
            || g.getFullName().toLowerCase().contains(search)
            || g.getEmail().toLowerCase().contains(search)
            || g.getPhoneNumber().toLowerCase().contains(search)
            || (b != null && b.getRoomNumber().toLowerCase().contains(search))
            || (b != null && b.getBookingType().toLowerCase().contains(search));
    }

    // Check-in cell: scheduled date + standard time (2:00 PM), then the actual check-in.
    private String checkInText(Booking b) {

        String standard = b.getCheckInDate().format(DAY_FMT)
            + " \u00B7 " + Booking.STANDARD_CHECK_IN_TIME.format(TIME_FMT);

        LocalDateTime actual = b.getActualCheckIn();
        String status;

        if (actual != null) {
            status = "Actual: " + actual.format(STAMP_FMT);
        } else if ("Checked In".equalsIgnoreCase(b.getBookingStatus())
                || "Checked Out".equalsIgnoreCase(b.getBookingStatus())) {
            status = "Checked in"; // saved before times were tracked, so no clock time to show
        } else {
            status = "Not yet checked in";
        }

        return twoLines(standard, status);
    }

    // Check-out cell: scheduled date + standard time (11:00 AM), then the actual check-out.
    private String checkOutText(Booking b) {

        String standard = b.getCheckOutDate().format(DAY_FMT)
            + " \u00B7 " + Booking.STANDARD_CHECK_OUT_TIME.format(TIME_FMT);

        LocalDateTime actual = b.getActualCheckOut();
        String status;

        if (actual != null) {
            status = "Actual: " + actual.format(STAMP_FMT);
        } else if ("Checked Out".equalsIgnoreCase(b.getBookingStatus())) {
            status = "Checked out"; // saved before times were tracked
        } else {
            status = "Not yet checked out";
        }

        return twoLines(standard, status);
    }

    private static String twoLines(String main, String note) {
        return "<html><div style='text-align:center'>" + main
            + "<br><font color='" + MUTED_HEX + "' size='2'>" + note + "</font></div></html>";
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Herz Hotel", JOptionPane.ERROR_MESSAGE);
    }

    // =====================================================================
    //  Dialogs
    // =====================================================================

    /** Edit guest form (first + last name, email, contact, address). */
    private class GuestDialog extends JDialog {

        private final String guestId;

        private final JTextField firstField = formField("Enter first name");
        private final JTextField lastField = formField("Enter last name");
        private final JTextField emailField = formField("guest@example.com");
        private final JTextField phoneField = formField("Include country code");
        private final JTextField addressField = formField("Enter Address");
        private final JLabel errorLabel = new JLabel(" ");

        private Guest result;

        GuestDialog(Guest existing, String guestId) {
            super(SwingUtilities.getWindowAncestor(GuestPanel.this),
                  "Edit guest",
                  ModalityType.APPLICATION_MODAL);

            this.guestId = guestId;

            setUndecorated(true);

            ((javax.swing.text.AbstractDocument) phoneField.getDocument())
                .setDocumentFilter(new PhoneFilter());

            String full = existing.getFullName() == null ? "" : existing.getFullName().trim();
            int split = full.lastIndexOf(' ');
            firstField.setText(split < 0 ? full : full.substring(0, split));
            lastField.setText(split < 0 ? "" : full.substring(split + 1));
            emailField.setText(existing.getEmail());
            phoneField.setText(existing.getPhoneNumber());
            addressField.setText(existing.getAddress());

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBackground(Color.WHITE);
            root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(22, 22, 20, 22)
            ));

            JLabel title = new JLabel("Edit guest");
            title.setFont(new Font("SansSerif", Font.BOLD, 20));
            title.setForeground(INK);

            JLabel subtitle = new JLabel("Update this guest's profile.");
            subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
            subtitle.setForeground(MUTED);

            root.add(left(title));
            root.add(Box.createVerticalStrut(6));
            root.add(left(subtitle));
            root.add(Box.createVerticalStrut(14));

            JSeparator sep = new JSeparator();
            sep.setForeground(ROW_LINE);
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            root.add(sep);
            root.add(Box.createVerticalStrut(16));

            JPanel names = new JPanel(new GridLayout(1, 2, 18, 0));
            names.setOpaque(false);
            names.add(labeled("First name", firstField));
            names.add(labeled("Last name", lastField));
            root.add(left(names));
            root.add(Box.createVerticalStrut(14));

            root.add(left(labeled("Email address", emailField)));
            root.add(Box.createVerticalStrut(14));
            root.add(left(labeled("Contact number", phoneField)));
            root.add(Box.createVerticalStrut(14));
            root.add(left(labeled("Address", addressField)));
            root.add(Box.createVerticalStrut(12));

            errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
            errorLabel.setForeground(DANGER);
            root.add(left(errorLabel));

            JPanel note = new JPanel(new BorderLayout());
            note.setBackground(FIELD_BG);
            note.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
            JLabel noteText = new JLabel("Guest details are used only for hotel operations.");
            noteText.setFont(new Font("SansSerif", Font.PLAIN, 10));
            noteText.setForeground(MUTED);
            note.add(noteText, BorderLayout.WEST);
            root.add(left(note));
            root.add(Box.createVerticalStrut(18));

            JButton cancel = styledButton("Cancel", ButtonKind.OUTLINE);
            cancel.setPreferredSize(new Dimension(110, 40));
            cancel.addActionListener(e -> dispose());

            JButton save = styledButton("Save changes", ButtonKind.PRIMARY);
            save.setPreferredSize(new Dimension(220, 40));
            save.addActionListener(e -> submit());

            JPanel buttons = new JPanel(new BorderLayout(12, 0));
            buttons.add(cancel, BorderLayout.WEST);
            buttons.add(save, BorderLayout.CENTER);
            buttons.setOpaque(false);
            root.add(left(buttons));

            getRootPane().setDefaultButton(save);
            setContentPane(root);
            setMinimumSize(new Dimension(400, 0));
            pack();
            setLocationRelativeTo(GuestPanel.this);
        }

        private void submit() {
            String first = firstField.getText().trim();
            String last = lastField.getText().trim();

            if (first.isEmpty()) {
                errorLabel.setText("First name is required.");
                return;
            }

            String phoneError = validatePhone(phoneField.getText());

            if (phoneError != null) {
                errorLabel.setText(phoneError);
                phoneField.requestFocus();
                return;
            }

            try {
                result = new Guest(
                    guestId,
                    (first + " " + last).trim(),
                    emailField.getText().trim(),
                    formatPhone(phoneField.getText()),
                    addressField.getText().trim()
                );
                dispose();
            } catch (IllegalArgumentException ex) {
                result = null;
                errorLabel.setText(ex.getMessage());
                pack();
            }
        }

        Guest getResult() { return result; }
    }

    // =====================================================================
    //  Small UI helpers
    // =====================================================================

    private enum ButtonKind { PRIMARY, OUTLINE, DANGER }

    private static JButton styledButton(String text, ButtonKind kind) {

        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color fill;
                Color line;
                Color text;

                switch (kind) {
                    case PRIMARY -> { fill = BURGUNDY; line = BURGUNDY; text = Color.WHITE; }
                    case DANGER  -> { fill = Color.WHITE; line = DANGER_LINE; text = DANGER; }
                    default      -> { fill = Color.WHITE; line = CARD_BORDER; text = INK; }
                }

                if (!isEnabled()) {
                    fill = kind == ButtonKind.PRIMARY ? new Color(0xB9, 0xA0, 0xA0) : fill;
                    text = HINT;
                }
                if (getModel().isRollover() && isEnabled() && kind != ButtonKind.PRIMARY) {
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

                setForeground(text);
                super.paintComponent(g);
            }
        };

        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFont(new Font("SansSerif", Font.PLAIN, 12));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static JTextField formField(String placeholder) {
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

    private static JPanel labeled(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setForeground(MUTED);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        if (c instanceof JPanel || c instanceof JLabel) {
            Dimension pref = c.getPreferredSize();
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
        }
        return c;
    }

    // =====================================================================
    //  Philippine mobile number rules: +63 9xx xxx xxxx (12 digits total)
    // =====================================================================

    private static final int PH_DIGITS = 12;

    private static String digitsOnly(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    /** Returns an error message, or null when the number is valid (blank is allowed). */
    private static String validatePhone(String raw) {

        String text = raw == null ? "" : raw.trim();

        if (text.isEmpty()) {
            return null;
        }

        String digits = digitsOnly(text);

        if (!text.startsWith("+63") && !digits.startsWith("63")) {
            return "Contact number must start with the country code +63.";
        }
        if (digits.length() > PH_DIGITS) {
            return "Contact number cannot exceed 12 digits (+63 followed by 10 digits).";
        }
        if (digits.length() < PH_DIGITS) {
            return "Contact number is incomplete. Use +63 followed by 10 digits.";
        }
        if (digits.charAt(2) != '9') {
            return "Philippine mobile numbers start with 9 after +63.";
        }

        return null;
    }

    /** Formats a valid number as +63 912 345 6789. */
    private static String formatPhone(String raw) {

        String digits = digitsOnly(raw);

        if (digits.length() != PH_DIGITS) {
            return raw == null ? "" : raw.trim();
        }

        return "+63 " + digits.substring(2, 5) + " "
                + digits.substring(5, 8) + " " + digits.substring(8);
    }

    /** Only lets digits, spaces, dashes and a leading '+' in, capped at 12 digits. */
    private static class PhoneFilter extends javax.swing.text.DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String text,
                                 javax.swing.text.AttributeSet attr)
                throws javax.swing.text.BadLocationException {
            replace(fb, offset, 0, text, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text,
                            javax.swing.text.AttributeSet attrs)
                throws javax.swing.text.BadLocationException {

            if (text == null) {
                super.replace(fb, offset, length, text, attrs);
                return;
            }

            String current = fb.getDocument().getText(0, fb.getDocument().getLength());
            String next = current.substring(0, offset) + text
                    + current.substring(offset + length);

            if (!next.matches("\\+?[0-9 \\-]*")) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            if (digitsOnly(next).length() > PH_DIGITS) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }

            super.replace(fb, offset, length, text, attrs);
        }
    }

    /** Text field that paints a gray hint while empty. */
    private static class PlaceholderField extends JTextField {

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
    private static class RoundedBorder extends AbstractBorder {

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
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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

    /** Minimal DocumentListener that runs one callback. */
    private static class SimpleDocListener implements javax.swing.event.DocumentListener {

        private final Runnable action;

        SimpleDocListener(Runnable action) {
            this.action = action;
        }

        @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { action.run(); }
        @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { action.run(); }
        @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { action.run(); }
    }
}