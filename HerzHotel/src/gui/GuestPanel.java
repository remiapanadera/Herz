package gui;

import exception.DuplicateRecordException;
import exception.InvalidBookingException;
import model.Booking;
import model.Guest;
import model.Payment;
import service.BookingService;
import service.GuestService;
import service.PaymentService;
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

/**
 * Screen 3: Guest Management.
 * - Displays guest profiles paired with their active bookings (Room column first).
 * - Cancelling a booking releases the room, locks the PHP 500 cash cancellation fee,
 *   records it in PaymentService, and hides the cancelled row from Guest Management.
 */
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

    private static final double CANCELLATION_FEE = 500.0;
    private static final String PAYMENT_METHOD_CASH = "Cash";
    private static final int PAGE_SIZE = 8;

    private static final DateTimeFormatter DAY_FMT  = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    // High-contrast HTML badge colors for check-in / check-out statuses
    private static final String STATUS_PENDING_HEX = "#B45309";
    private static final String STATUS_DONE_HEX    = "#1F6E43";

    private final GuestService guestService;
    private final BookingService bookingService;
    private final PaymentService paymentService;

    private JTable guestTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JLabel countLabel;
    private JLabel showingLabel;
    private JButton checkInButton;
    private JButton checkOutButton;
    private JButton cancelBookingButton;
    private JButton prevButton;
    private JButton nextButton;
    private JLabel pageLabel;

    private final List<GuestRow> filtered = new ArrayList<>();
    private int currentPage = 0;

    private static class GuestRow {
        final Guest guest;
        final Booking booking;

        GuestRow(Guest guest, Booking booking) {
            this.guest = guest;
            this.booking = booking;
        }
    }

    public GuestPanel(
            GuestService guestService,
            BookingService bookingService,
            PaymentService paymentService) {

        if (guestService == null || bookingService == null || paymentService == null) {
            throw new IllegalArgumentException(
                "GuestService, BookingService, and PaymentService are required."
            );
        }

        this.guestService = guestService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;

        setLayout(new BorderLayout(0, 14));
        setBackground(PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(26, 26, 26, 26));

        add(createHeader(), BorderLayout.NORTH);
        add(createCard(), BorderLayout.CENTER);

        refreshTable();

        new javax.swing.Timer(30_000, e -> updateActionButtons()).start();
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

        // --- Search + action buttons row ---
        JPanel searchRow = new JPanel(new BorderLayout(12, 0));
        searchRow.setOpaque(false);

        searchField = new PlaceholderField("Search by room, name, phone, or address...");
        searchField.setPreferredSize(new Dimension(260, 36));
        searchField.setBackground(FIELD_BG);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(FIELD_BG, 6, 1),
            BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        searchField.addActionListener(e -> resetAndRefresh());
        searchField.getDocument().addDocumentListener(new SimpleDocListener(this::resetAndRefresh));

        JButton editButton = styledButton("Edit", ButtonKind.OUTLINE);
        editButton.setPreferredSize(new Dimension(68, 36));
        editButton.addActionListener(e -> editGuest());

        checkInButton = styledButton("Check in", ButtonKind.PRIMARY);
        checkInButton.setPreferredSize(new Dimension(92, 36));
        checkInButton.addActionListener(e -> checkInSelected());

        checkOutButton = styledButton("Check out", ButtonKind.PRIMARY);
        checkOutButton.setPreferredSize(new Dimension(98, 36));
        checkOutButton.addActionListener(e -> checkOutSelected());

        cancelBookingButton = styledButton("Cancel booking", ButtonKind.DANGER);
        cancelBookingButton.setPreferredSize(new Dimension(122, 36));
        cancelBookingButton.addActionListener(e -> cancelSelectedBooking());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(editButton);
        right.add(checkInButton);
        right.add(checkOutButton);
        right.add(cancelBookingButton);

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
            "Room", "Guest name", "Contact number",
            "Address", "Booking type", "Check-in", "Check-out"
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
        guestTable.setRowHeight(52);
        guestTable.setShowVerticalLines(false);
        guestTable.setShowHorizontalLines(true);
        guestTable.setGridColor(ROW_LINE);
        guestTable.setIntercellSpacing(new Dimension(0, 1));
        guestTable.setSelectionBackground(ROW_SELECTED);
        guestTable.setSelectionForeground(INK);
        guestTable.setFillsViewportHeight(true);
        guestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        guestTable.setToolTipText("Double-click a guest to edit");

        guestTable.setAutoCreateRowSorter(false);
        guestTable.setRowSorter(null);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < guestTable.getColumnCount(); i++) {
            guestTable.getColumnModel().getColumn(i).setCellRenderer(center);
        }

        DefaultTableCellRenderer roomRenderer = new DefaultTableCellRenderer();
        roomRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        roomRenderer.setFont(new Font("SansSerif", Font.BOLD, 12));
        roomRenderer.setForeground(BURGUNDY);
        guestTable.getColumnModel().getColumn(0).setCellRenderer(roomRenderer);

        int[] widths = {58, 128, 122, 138, 102, 148, 148};
        for (int i = 0; i < widths.length; i++) {
            guestTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        guestTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateActionButtons();
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
    //  Check-in, Check-out & Cancellation Logic
    // =====================================================================

    private void checkInSelected() {

        GuestRow row = getSelectedRow();

        if (row == null || row.booking == null) {
            showError("Please select a guest with an active booking to check in.");
            return;
        }

        try {
            bookingService.checkInBooking(row.booking.getBookingId());
            refreshTable();

            JOptionPane.showMessageDialog(this,
                row.guest.getFullName() + " checked in to Room "
                + row.booking.getRoomNumber() + ".");

        } catch (InvalidBookingException e) {
            showError(e.getMessage());
            refreshTable();
        }
    }

    private void checkOutSelected() {

        GuestRow row = getSelectedRow();

        if (row == null || row.booking == null) {
            showError("Please select a checked-in guest to check out.");
            return;
        }

        try {
            bookingService.checkOutBooking(row.booking.getBookingId());
            refreshTable();

            JOptionPane.showMessageDialog(this,
                row.guest.getFullName() + " checked out from Room "
                + row.booking.getRoomNumber() + ".");

        } catch (InvalidBookingException e) {
            showError(e.getMessage());
            refreshTable();
        }
    }

    /**
     * Cancels a Reserved booking, frees the room, records the non-editable PHP 500
     * cash cancellation fee in PaymentService, and hides the cancelled row from Guest Management.
     */
    private void cancelSelectedBooking() {

        GuestRow row = getSelectedRow();

        if (row == null || row.booking == null) {
            showError("Please select a guest with a reserved booking to cancel.");
            return;
        }

        if (!"Reserved".equalsIgnoreCase(row.booking.getBookingStatus())) {
            showError("Only reserved (not yet checked-in) bookings can be cancelled.");
            return;
        }

        CancellationFeeDialog dialog = new CancellationFeeDialog(row.guest, row.booking);
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            return;
        }

        String bookingId = row.booking.getBookingId();
        String guestName = row.guest.getFullName();
        String roomNumber = row.booking.getRoomNumber();

        try {
            // 1. Mark booking cancelled & release the room
            bookingService.cancelBooking(bookingId);

            // 2. Record the mandatory PHP 500 cash cancellation fee
            Payment feePayment = new Payment(
                nextPaymentId(),
                bookingId,
                CANCELLATION_FEE,
                PAYMENT_METHOD_CASH,
                LocalDate.now(),
                "Paid"
            );
            paymentService.addPayment(feePayment);

            refreshTable();

            JOptionPane.showMessageDialog(
                this,
                "Booking for " + guestName + " (Room " + roomNumber + ") has been cancelled.\n"
                    + "Cancellation fee of PHP 500 paid in Cash.",
                "Booking Cancelled",
                JOptionPane.INFORMATION_MESSAGE
            );

        } catch (InvalidBookingException
                | IllegalArgumentException e) {
            showError(e.getMessage());
            refreshTable();
        }
    }

    private String nextPaymentId() {
        int max = 0;
        for (Payment p : paymentService.getAllPayments()) {
            String id = p.getPaymentId();
            if (id != null) {
                int dash = id.lastIndexOf('-');
                String digits = (dash >= 0 ? id.substring(dash + 1) : id).replaceAll("\\D", "");
                if (!digits.isEmpty() && digits.length() <= 9) {
                    max = Math.max(max, Integer.parseInt(digits));
                }
            }
        }
        return String.format("PAY-%04d", max + 1);
    }

    private void updateActionButtons() {

        if (checkInButton == null || checkOutButton == null || cancelBookingButton == null) {
            return;
        }

        GuestRow row = getSelectedRow();

        // --- Check-in button state ---
        boolean canCheckIn = false;
        String checkInTip;

        if (row == null || row.booking == null) {
            checkInTip = "Select a guest with a booking.";
        } else if ("Checked In".equalsIgnoreCase(row.booking.getBookingStatus())) {
            checkInTip = "This guest is already checked in.";
        } else if ("Checked Out".equalsIgnoreCase(row.booking.getBookingStatus())) {
            checkInTip = "This guest has already checked out.";
        } else if (bookingService.isEligibleForCheckIn(row.booking)) {
            canCheckIn = true;
            checkInTip = "Check this guest in now.";
        } else if (LocalDate.now().equals(row.booking.getCheckInDate())) {
            checkInTip = "Check-in opens at " + Booking.STANDARD_CHECK_IN_TIME.format(TIME_FMT) + ".";
        } else {
            checkInTip = "Check-in opens on " + row.booking.getCheckInDate().format(DAY_FMT)
                + " at " + Booking.STANDARD_CHECK_IN_TIME.format(TIME_FMT) + ".";
        }

        checkInButton.setEnabled(canCheckIn);
        checkInButton.setToolTipText(checkInTip);

        // --- Check-out button state ---
        boolean canCheckOut = false;
        String checkOutTip;

        if (row == null || row.booking == null) {
            checkOutTip = "Select a guest with a booking.";
        } else if ("Checked Out".equalsIgnoreCase(row.booking.getBookingStatus())) {
            checkOutTip = "This guest has already checked out.";
        } else if ("Checked In".equalsIgnoreCase(row.booking.getBookingStatus())) {
            canCheckOut = true;
            checkOutTip = "Check this guest out now.";
        } else {
            checkOutTip = "Guest must be checked in before checking out.";
        }

        checkOutButton.setEnabled(canCheckOut);
        checkOutButton.setToolTipText(checkOutTip);

        // --- Cancel booking button state ---
        boolean canCancel = false;
        String cancelTip;

        if (row == null || row.booking == null) {
            cancelTip = "Select a guest with a reserved booking to cancel.";
        } else if ("Reserved".equalsIgnoreCase(row.booking.getBookingStatus())) {
            canCancel = true;
            cancelTip = "Cancel this reservation (PHP 500 cash cancellation fee applies).";
        } else if ("Checked In".equalsIgnoreCase(row.booking.getBookingStatus())) {
            cancelTip = "A checked-in guest cannot be cancelled.";
        } else {
            cancelTip = "This booking can no longer be cancelled.";
        }

        cancelBookingButton.setEnabled(canCancel);
        cancelBookingButton.setToolTipText(cancelTip);
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
        String search = keyword.toLowerCase();

        filtered.clear();

        List<Booking> allBookings = bookingService.getAllBookings();

        // Join guest records with their active (non-cancelled) bookings
        for (Guest g : guestService.getAllGuests()) {

            List<Booking> activeStays = new ArrayList<>();
            boolean hasAnyBooking = false;

            for (Booking b : allBookings) {
                if (b.getGuestId().equalsIgnoreCase(g.getGuestId())) {
                    hasAnyBooking = true;
                    if (!"Cancelled".equalsIgnoreCase(b.getBookingStatus())) {
                        activeStays.add(b);
                    }
                }
            }

            // Hide guests whose bookings were all cancelled
            if (activeStays.isEmpty()) {
                if (!hasAnyBooking && matches(g, null, search)) {
                    filtered.add(new GuestRow(g, null));
                }
                continue;
            }

            for (Booking b : activeStays) {
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
                b == null ? "\u2014" : b.getRoomNumber(),
                g.getFullName(),
                g.getPhoneNumber(),
                g.getAddress() == null || g.getAddress().isBlank() ? "\u2014" : g.getAddress(),
                b == null ? "\u2014" : b.getBookingType(),
                b == null ? "\u2014" : checkInText(b),
                b == null ? "\u2014" : checkOutText(b)
            });
        }

        int total = filtered.size();

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

        updateActionButtons();
    }

    private boolean matches(Guest g, Booking b, String search) {
        if (search.isEmpty()) {
            return true;
        }
        return g.getGuestId().toLowerCase().contains(search)
            || g.getFullName().toLowerCase().contains(search)
            || g.getPhoneNumber().toLowerCase().contains(search)
            || (g.getAddress() != null && g.getAddress().toLowerCase().contains(search))
            || (b != null && b.getRoomNumber().toLowerCase().contains(search))
            || (b != null && b.getBookingType().toLowerCase().contains(search));
    }

    private String checkInText(Booking b) {

        LocalDateTime actual = b.getActualCheckIn();

        if (actual != null) {
            String mainLine = actual.toLocalDate().format(DAY_FMT)
                + " \u00B7 " + actual.toLocalTime().format(TIME_FMT);
            return twoLines(mainLine, "Checked in", STATUS_DONE_HEX);
        }

        if ("Checked In".equalsIgnoreCase(b.getBookingStatus())
                || "Checked Out".equalsIgnoreCase(b.getBookingStatus())) {
            String mainLine = b.getCheckInDate().format(DAY_FMT);
            return twoLines(mainLine, "Checked in", STATUS_DONE_HEX);
        }

        String mainLine = b.getCheckInDate().format(DAY_FMT);
        return twoLines(mainLine, "Not yet checked in", STATUS_PENDING_HEX);
    }

    private String checkOutText(Booking b) {

        LocalDateTime actual = b.getActualCheckOut();

        if (actual != null) {
            String mainLine = actual.toLocalDate().format(DAY_FMT)
                + " \u00B7 " + actual.toLocalTime().format(TIME_FMT);
            return twoLines(mainLine, "Checked out", STATUS_DONE_HEX);
        }

        if ("Checked Out".equalsIgnoreCase(b.getBookingStatus())) {
            String mainLine = b.getCheckOutDate().format(DAY_FMT);
            return twoLines(mainLine, "Checked out", STATUS_DONE_HEX);
        }

        String mainLine = b.getCheckOutDate().format(DAY_FMT);
        return twoLines(mainLine, "Not yet checked out", STATUS_PENDING_HEX);
    }

    private static String twoLines(String main, String badgeText, String badgeHex) {
        return "<html><div style='text-align:center'>"
            + "<span style='color:#1B2A3A; font-size:11px;'>" + main + "</span><br>"
            + "<b style='color:" + badgeHex + "; font-size:10.5px;'>" + badgeText + "</b>"
            + "</div></html>";
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Herz Hotel", JOptionPane.ERROR_MESSAGE);
    }

    // =====================================================================
    //  Dialogs
    // =====================================================================

    private class CancellationFeeDialog extends JDialog {

        private final JTextField amountField = formField("500");
        private boolean confirmed = false;

        CancellationFeeDialog(Guest guest, Booking booking) {
            super(SwingUtilities.getWindowAncestor(GuestPanel.this),
                  "Cancel booking",
                  ModalityType.APPLICATION_MODAL);

            setUndecorated(true);

            // Lock the PHP 500 cancellation fee so it cannot be changed
            amountField.setText("500");
            amountField.setEditable(false);
            amountField.setFocusable(false);
            amountField.setBackground(FIELD_BG);
            amountField.setFont(new Font("SansSerif", Font.BOLD, 13));

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBackground(Color.WHITE);
            root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(22, 22, 20, 22)
            ));

            JLabel title = new JLabel("Cancel booking");
            title.setFont(new Font("SansSerif", Font.BOLD, 20));
            title.setForeground(INK);

            JLabel subtitle = new JLabel(
                guest.getFullName() + " · Room " + booking.getRoomNumber()
                + " (" + booking.getCheckInDate().format(DAY_FMT) + ")"
            );
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
            root.add(Box.createVerticalStrut(14));

            JPanel feeBanner = new JPanel(new BorderLayout());
            feeBanner.setBackground(new Color(0xFB, 0xF3, 0xE4));
            feeBanner.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(0xE2, 0xC6, 0x99), 6, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));
            JLabel feeText = new JLabel(
                "<html><b>Cancellation Fee Due: PHP 500 (Cash Only)</b><br>"
                + "<span style='color:#6B7783; font-size:10px;'>"
                + "A fixed cash fee of PHP 500 is required to process cancellation.</span></html>"
            );
            feeText.setFont(new Font("SansSerif", Font.PLAIN, 12));
            feeText.setForeground(BURGUNDY);
            feeBanner.add(feeText, BorderLayout.CENTER);
            root.add(left(feeBanner));
            root.add(Box.createVerticalStrut(14));

            root.add(left(labeled("Cancellation Fee (PHP · Fixed Cash)", amountField)));
            root.add(Box.createVerticalStrut(18));

            JButton back = styledButton("Keep booking", ButtonKind.OUTLINE);
            back.setPreferredSize(new Dimension(120, 40));
            back.addActionListener(e -> dispose());

            JButton proceed = styledButton("Pay PHP 500 & Cancel", ButtonKind.PRIMARY);
            proceed.setPreferredSize(new Dimension(210, 40));
            proceed.addActionListener(e -> {
                confirmed = true;
                dispose();
            });

            JPanel buttons = new JPanel(new BorderLayout(12, 0));
            buttons.add(back, BorderLayout.WEST);
            buttons.add(proceed, BorderLayout.CENTER);
            buttons.setOpaque(false);
            root.add(left(buttons));

            getRootPane().setDefaultButton(proceed);
            setContentPane(root);
            setMinimumSize(new Dimension(410, 0));
            pack();
            setLocationRelativeTo(GuestPanel.this);
        }

        boolean isConfirmed() { return confirmed; }
    }

    private class GuestDialog extends JDialog {

        private final String guestId;
        private final String existingEmail;

        private final JTextField firstField = formField("Enter first name");
        private final JTextField lastField = formField("Enter last name");
        private final JTextField phoneField = formField("Include country code");
        private final JTextField addressField = formField("Enter Address");
        private final JLabel errorLabel = new JLabel(" ");

        private Guest result;

        GuestDialog(Guest existing, String guestId) {
            super(SwingUtilities.getWindowAncestor(GuestPanel.this),
                  "Edit guest",
                  ModalityType.APPLICATION_MODAL);

            this.guestId = guestId;
            this.existingEmail = existing.getEmail() == null ? "" : existing.getEmail();

            setUndecorated(true);

            ((javax.swing.text.AbstractDocument) phoneField.getDocument())
                .setDocumentFilter(new PhoneFilter());

            String full = existing.getFullName() == null ? "" : existing.getFullName().trim();
            int split = full.lastIndexOf(' ');
            firstField.setText(split < 0 ? full : full.substring(0, split));
            lastField.setText(split < 0 ? "" : full.substring(split + 1));
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
                    existingEmail,
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
                Color txt;

                switch (kind) {
                    case PRIMARY -> { fill = BURGUNDY; line = BURGUNDY; txt = Color.WHITE; }
                    case DANGER  -> { fill = Color.WHITE; line = DANGER_LINE; txt = DANGER; }
                    default      -> { fill = Color.WHITE; line = CARD_BORDER; txt = INK; }
                }

                if (!isEnabled()) {
                    if (kind == ButtonKind.PRIMARY || kind == ButtonKind.DANGER) {
                        fill = new Color(0xEF, 0xDE, 0xDE);
                        line = new Color(0xD8, 0xB4, 0xB4);
                        txt  = new Color(0x6E, 0x24, 0x24);
                    } else {
                        fill = new Color(0xF5, 0xF5, 0xF5);
                        line = CARD_BORDER;
                        txt  = HINT;
                    }
                } else if (getModel().isRollover() && kind == ButtonKind.DANGER) {
                    fill = new Color(0xFA, 0xF0, 0xEE);
                } else if (getModel().isRollover() && kind != ButtonKind.PRIMARY) {
                    fill = new Color(0xFA, 0xF8, 0xF6);
                } else if (getModel().isRollover() && kind == ButtonKind.PRIMARY) {
                    fill = new Color(0x64, 0x0A, 0x0A);
                }
                if (getModel().isPressed() && isEnabled()) {
                    fill = fill.darker();
                }

                g2.setColor(fill);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.setColor(line);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();

                Graphics2D tg = (Graphics2D) g.create();
                tg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                tg.setFont(getFont());
                tg.setColor(txt);
                FontMetrics fm = tg.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                tg.drawString(getText(), tx, ty);
                tg.dispose();
            }
        };

        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
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
    //  Input Filters & Validation Helpers
    // =====================================================================

    private static final int PH_DIGITS = 12;

    private static String digitsOnly(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

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

    private static String formatPhone(String raw) {
        String digits = digitsOnly(raw);
        if (digits.length() != PH_DIGITS) {
            return raw == null ? "" : raw.trim();
        }
        return "+63 " + digits.substring(2, 5) + " "
                + digits.substring(5, 8) + " " + digits.substring(8);
    }

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