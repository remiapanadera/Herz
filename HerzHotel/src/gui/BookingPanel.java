package gui;

import exception.DuplicateRecordException;
import exception.InvalidBookingException;
import model.Booking;
import model.Guest;
import model.Payment;
import model.Room;
import service.BookingService;
import service.GuestService;
import service.PaymentService;
import service.RoomService;

import javax.swing.*;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static gui.UiKit.*;

/**
 * Bookings page: a reservation form on the left and the payment receipt of the
 * selected booking on the right. Payments are recorded from here, so there is no
 * separate Payments page.
 */
public class BookingPanel extends JPanel {

    private static final DateTimeFormatter DAY_FMT =
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final BookingService bookingService;
    private final GuestService guestService;
    private final RoomService roomService;
    private final PaymentService paymentService;

    private String receivedBy = "Front desk";
    private boolean walkIn = false;
    private boolean loading = false;

    // form
    private JButton advanceButton;
    private JButton walkInButton;
    private JComboBox<Guest> guestBox;
    private JTextField contactField;
    private JComboBox<Room> roomBox;
    private JComboBox<String> adultsBox;
    private JSpinner checkInSpinner;
    private JSpinner checkOutSpinner;
    private JLabel stayNote;

    // top bar + receipt
    private JComboBox<BookingItem> bookingBox;
    private JPanel receiptCard;
    private Booking currentBooking;

    public BookingPanel(
            BookingService bookingService,
            GuestService guestService,
            RoomService roomService,
            PaymentService paymentService) {

        if (bookingService == null
                || guestService == null
                || roomService == null
                || paymentService == null) {
            throw new IllegalArgumentException(
                "All booking-related services are required."
            );
        }

        this.bookingService = bookingService;
        this.guestService = guestService;
        this.roomService = roomService;
        this.paymentService = paymentService;

        setLayout(new BorderLayout(0, 16));
        setBackground(PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(26, 26, 26, 26));

        add(createTopBar(), BorderLayout.NORTH);
        add(createContent(), BorderLayout.CENTER);

        refreshTable();
    }

    /** Name shown in the receipt's "Received by" line (the logged-in user). */
    public void setReceivedBy(String name) {
        if (name != null && !name.trim().isEmpty()) {
            receivedBy = name.trim();
        }
    }

    // =====================================================================
    //  Top bar: record payment + booking actions + booking picker
    // =====================================================================

    private JPanel createTopBar() {

        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);

        JButton recordButton = button("+ Record payment", ButtonKind.PRIMARY);
        recordButton.setPreferredSize(new Dimension(150, 38));
        recordButton.addActionListener(e -> recordPayment());

        JButton checkInButton = button("Check in", ButtonKind.OUTLINE);
        checkInButton.setPreferredSize(new Dimension(86, 38));
        checkInButton.addActionListener(e -> checkIn());

        JButton checkOutButton = button("Check out", ButtonKind.OUTLINE);
        checkOutButton.setPreferredSize(new Dimension(92, 38));
        checkOutButton.addActionListener(e -> checkOut());

        JButton cancelButton = button("Cancel booking", ButtonKind.DANGER);
        cancelButton.setPreferredSize(new Dimension(120, 38));
        cancelButton.addActionListener(e -> cancelBooking());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(recordButton);
        left.add(checkInButton);
        left.add(checkOutButton);
        left.add(cancelButton);

        bookingBox = new JComboBox<>();
        styleCombo(bookingBox);
        bookingBox.setPreferredSize(new Dimension(360, 38));
        bookingBox.addActionListener(e -> {
            if (!loading) {
                BookingItem item = (BookingItem) bookingBox.getSelectedItem();
                currentBooking = item == null ? null : item.booking;
                renderReceipt();
            }
        });

        bar.add(left, BorderLayout.WEST);
        bar.add(bookingBox, BorderLayout.EAST);

        return bar;
    }

    // =====================================================================
    //  Content: reservation form (left) + receipt (right)
    // =====================================================================

    private JComponent createContent() {

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;
        gc.anchor = GridBagConstraints.NORTH;

        gc.gridx = 0;
        gc.weightx = 0.62;
        gc.insets = new Insets(0, 0, 0, 14);
        content.add(createReservationCard(), gc);

        receiptCard = new JPanel();
        receiptCard.setLayout(new BoxLayout(receiptCard, BoxLayout.Y_AXIS));
        receiptCard.setBackground(Color.WHITE);
        receiptCard.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(CARD_BORDER, 10, 1),
            BorderFactory.createEmptyBorder(16, 14, 14, 14)
        ));

        gc.gridx = 1;
        gc.weightx = 0.38;
        gc.insets = new Insets(0, 0, 0, 0);
        content.add(receiptCard, gc);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel createReservationCard() {

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(CARD_BORDER, 10, 1),
            BorderFactory.createEmptyBorder(24, 30, 20, 30)
        ));

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.anchor = GridBagConstraints.NORTHWEST;

        // --- title ---
        JLabel title = new JLabel("Reservation details");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(INK);

        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;
        gc.insets = new Insets(0, 0, 18, 0);
        card.add(title, gc);

        // --- booking type ---
        advanceButton = button("Advance booking", ButtonKind.PRIMARY);
        advanceButton.setPreferredSize(new Dimension(150, 38));
        advanceButton.addActionListener(e -> setWalkIn(false));

        walkInButton = button("Walk-in booking", ButtonKind.OUTLINE);
        walkInButton.setPreferredSize(new Dimension(150, 38));
        walkInButton.addActionListener(e -> setWalkIn(true));

        JPanel types = new JPanel(new GridLayout(1, 2, 20, 0));
        types.setOpaque(false);
        types.add(advanceButton);
        types.add(walkInButton);

        gc.gridy = 1;
        gc.insets = new Insets(0, 0, 22, 0);
        card.add(types, gc);

        // --- guest + contact ---
        guestBox = new JComboBox<>();
        styleCombo(guestBox);
        guestBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean sel, boolean focus) {
                String text = value instanceof Guest ? ((Guest) value).getFullName() : "";
                return super.getListCellRendererComponent(list, text, index, sel, focus);
            }
        });
        guestBox.addActionListener(e -> {
            Guest g = (Guest) guestBox.getSelectedItem();
            contactField.setText(g == null ? "" : g.getPhoneNumber());
        });

        contactField = formField("");
        contactField.setEditable(false);
        contactField.setBackground(FIELD_BG);

        addPair(card, 2, labeled("Guest name", guestBox), labeled("Contact number", contactField));

        // --- room + guests ---
        roomBox = new JComboBox<>();
        styleCombo(roomBox);
        roomBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean sel, boolean focus) {
                String text = "";
                if (value instanceof Room) {
                    Room r = (Room) value;
                    text = r.getRoomNumber() + " · " + r.getRoomType()
                        + " · PHP " + String.format("%,.0f", r.getPricePerNight())
                        + " / night";
                }
                return super.getListCellRendererComponent(list, text, index, sel, focus);
            }
        });
        roomBox.addActionListener(e -> reloadAdults());

        adultsBox = new JComboBox<>();
        styleCombo(adultsBox);

        addPair(card, 4, labeled("Room selection", roomBox), labeled("Guests", adultsBox));

        // --- dates ---
        checkInSpinner = dateSpinner(LocalDate.now().plusDays(1));
        checkOutSpinner = dateSpinner(LocalDate.now().plusDays(2));

        checkInSpinner.addChangeListener(e -> {
            LocalDate in = dateOf(checkInSpinner);
            if (!dateOf(checkOutSpinner).isAfter(in)) {
                checkOutSpinner.setValue(toDate(in.plusDays(1)));
            }
            updateStayNote();
        });
        checkOutSpinner.addChangeListener(e -> updateStayNote());

        addPair(card, 6, labeled("Check-in date", checkInSpinner), labeled("Check-out date", checkOutSpinner));

        // --- confirm + note ---
        JButton confirm = button("Confirm booking", ButtonKind.PRIMARY);
        confirm.setPreferredSize(new Dimension(170, 40));
        confirm.addActionListener(e -> confirmBooking());

        JPanel confirmRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        confirmRow.setOpaque(false);
        confirmRow.add(confirm);

        gc.gridy = 8; gc.gridwidth = 2;
        gc.insets = new Insets(8, 0, 14, 0);
        card.add(confirmRow, gc);

        stayNote = new JLabel("", SwingConstants.CENTER);
        stayNote.setFont(new Font("SansSerif", Font.PLAIN, 10));
        stayNote.setForeground(MUTED);

        gc.gridy = 9;
        gc.insets = new Insets(0, 0, 0, 0);
        card.add(stayNote, gc);

        // keeps the card from stretching its rows
        gc.gridy = 10; gc.weighty = 1; gc.fill = GridBagConstraints.BOTH;
        card.add(Box.createVerticalGlue(), gc);

        updateStayNote();
        return card;
    }

    private void addPair(JPanel card, int row, JComponent a, JComponent b) {

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 0.5;
        gc.gridy = row;

        gc.gridx = 0;
        gc.insets = new Insets(0, 0, 20, 12);
        card.add(a, gc);

        gc.gridx = 1;
        gc.insets = new Insets(0, 12, 20, 0);
        card.add(b, gc);
    }

    // =====================================================================
    //  Date helpers
    // =====================================================================

    private JSpinner dateSpinner(LocalDate initial) {

        SpinnerDateModel model = new SpinnerDateModel(
            toDate(initial), null, null, Calendar.DAY_OF_MONTH
        );

        JSpinner spinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "d MMM yyyy");
        spinner.setEditor(editor);

        editor.getTextField().setFont(new Font("SansSerif", Font.PLAIN, 12));
        editor.getTextField().setForeground(INK);
        editor.getTextField().setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 6));

        spinner.setBorder(new RoundedBorder(FIELD_BORDER, 6, 1));
        spinner.setPreferredSize(new Dimension(200, 40));
        return spinner;
    }

    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate dateOf(JSpinner spinner) {
        return ((Date) spinner.getValue()).toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void updateStayNote() {
        if (stayNote == null || checkInSpinner == null || checkOutSpinner == null) {
            return;
        }

        long nights = java.time.temporal.ChronoUnit.DAYS.between(
            dateOf(checkInSpinner), dateOf(checkOutSpinner)
        );

        stayNote.setText("Check-in from 2:00 PM · Check-out by 11:00 AM · "
            + Math.max(nights, 0) + (nights == 1 ? " night" : " nights"));
    }

    private void setWalkIn(boolean value) {

        walkIn = value;

        setKind(advanceButton, value ? ButtonKind.OUTLINE : ButtonKind.PRIMARY);
        setKind(walkInButton, value ? ButtonKind.PRIMARY : ButtonKind.OUTLINE);

        if (value) {
            checkInSpinner.setValue(toDate(LocalDate.now()));
            checkInSpinner.setEnabled(false);
            if (!dateOf(checkOutSpinner).isAfter(LocalDate.now())) {
                checkOutSpinner.setValue(toDate(LocalDate.now().plusDays(1)));
            }
        } else {
            checkInSpinner.setEnabled(true);
            if (dateOf(checkInSpinner).isBefore(LocalDate.now().plusDays(1))) {
                checkInSpinner.setValue(toDate(LocalDate.now().plusDays(1)));
            }
        }

        updateStayNote();
    }

    // =====================================================================
    //  Reloading choices
    // =====================================================================

    public void refreshTable() {

        if (guestBox == null || bookingBox == null) {
            return;
        }

        loading = true;

        // guests
        Guest selectedGuest = (Guest) guestBox.getSelectedItem();
        guestBox.removeAllItems();
        for (Guest g : guestService.getAllGuests()) {
            guestBox.addItem(g);
        }
        if (selectedGuest != null) {
            for (int i = 0; i < guestBox.getItemCount(); i++) {
                if (guestBox.getItemAt(i).getGuestId().equals(selectedGuest.getGuestId())) {
                    guestBox.setSelectedIndex(i);
                    break;
                }
            }
        }

        // available rooms
        Room selectedRoom = (Room) roomBox.getSelectedItem();
        roomBox.removeAllItems();
        for (Room r : roomService.filterRoomsByStatus("Available")) {
            roomBox.addItem(r);
        }
        if (selectedRoom != null) {
            for (int i = 0; i < roomBox.getItemCount(); i++) {
                if (roomBox.getItemAt(i).getRoomNumber().equals(selectedRoom.getRoomNumber())) {
                    roomBox.setSelectedIndex(i);
                    break;
                }
            }
        }

        loading = false;

        reloadAdults();
        reloadBookings(currentBooking == null ? null : currentBooking.getBookingId());
    }

    private void reloadAdults() {

        if (adultsBox == null) {
            return;
        }

        Room room = (Room) roomBox.getSelectedItem();
        int capacity = room == null ? 2 : RoomPanel.capacityFor(room.getRoomType());

        int previous = adultsBox.getSelectedIndex();

        adultsBox.removeAllItems();
        for (int i = 1; i <= capacity; i++) {
            adultsBox.addItem(i + (i == 1 ? " adult" : " adults"));
        }

        adultsBox.setSelectedIndex(previous >= 0 && previous < capacity
            ? previous
            : Math.min(1, capacity - 1));
    }

    private void reloadBookings(String selectId) {

        loading = true;

        bookingBox.removeAllItems();

        List<Booking> all = new ArrayList<>(bookingService.getAllBookings());

        BookingItem toSelect = null;

        // newest first
        for (int i = all.size() - 1; i >= 0; i--) {
            BookingItem item = new BookingItem(all.get(i));
            bookingBox.addItem(item);
            if (selectId != null && all.get(i).getBookingId().equalsIgnoreCase(selectId)) {
                toSelect = item;
            }
        }

        if (toSelect != null) {
            bookingBox.setSelectedItem(toSelect);
        } else if (bookingBox.getItemCount() > 0) {
            bookingBox.setSelectedIndex(0);
        }

        BookingItem selected = (BookingItem) bookingBox.getSelectedItem();
        currentBooking = selected == null ? null : selected.booking;

        loading = false;

        renderReceipt();
    }

    private class BookingItem {

        final Booking booking;

        BookingItem(Booking booking) {
            this.booking = booking;
        }

        @Override
        public String toString() {
            Guest g = guestService.findGuestById(booking.getGuestId());
            String name = g == null ? "Unknown guest" : g.getFullName();

            return name + " · Room " + booking.getRoomNumber()
                + " · " + booking.getCheckInDate().format(DAY_FMT)
                + " · " + booking.getBookingStatus();
        }
    }

    // =====================================================================
    //  Actions
    // =====================================================================

    private void confirmBooking() {

        Guest guest = (Guest) guestBox.getSelectedItem();
        Room room = (Room) roomBox.getSelectedItem();

        if (guest == null) {
            showError("Please add a guest on the Guests page first.");
            return;
        }
        if (room == null) {
            showError("There are no available rooms.");
            return;
        }

        LocalDate checkIn = walkIn ? LocalDate.now() : dateOf(checkInSpinner);
        LocalDate checkOut = dateOf(checkOutSpinner);

        if (checkIn.isBefore(LocalDate.now())) {
            showError("Check-in date cannot be in the past.");
            return;
        }
        if (!checkOut.isAfter(checkIn)) {
            showError("Check-out date must be after check-in date.");
            return;
        }

        try {
            Booking booking = new Booking(
                nextBookingId(),
                guest.getGuestId(),
                room.getRoomNumber(),
                checkIn,
                checkOut,
                "Reserved"
            );

            bookingService.addBooking(booking);

            if (walkIn) {
                bookingService.checkInBooking(booking.getBookingId());
            }

            currentBooking = booking;
            refreshTable();

            JOptionPane.showMessageDialog(this,
                walkIn ? "Walk-in guest checked in."
                       : "Reservation confirmed.");

        } catch (DuplicateRecordException
                | InvalidBookingException
                | IllegalArgumentException e) {

            showError(e.getMessage());
        }
    }

    private void checkIn() {

        if (!hasBooking()) return;

        try {
            bookingService.checkInBooking(currentBooking.getBookingId());
            refreshTable();
        } catch (InvalidBookingException e) {
            showError(e.getMessage());
        }
    }

    private void checkOut() {

        if (!hasBooking()) return;

        try {
            bookingService.checkOutBooking(currentBooking.getBookingId());
            refreshTable();
        } catch (InvalidBookingException e) {
            showError(e.getMessage());
        }
    }

    private void cancelBooking() {

        if (!hasBooking()) return;

        Guest g = guestService.findGuestById(currentBooking.getGuestId());
        String name = g == null ? "this guest" : g.getFullName();

        boolean ok = confirm(
            this,
            "CONFIRM CANCELLATION",
            "Cancel booking for " + name + "?",
            "Room " + currentBooking.getRoomNumber() + " will be released.",
            "This action cannot be undone.",
            "Cancel booking"
        );

        if (!ok) return;

        try {
            bookingService.cancelBooking(currentBooking.getBookingId());
            refreshTable();
        } catch (InvalidBookingException e) {
            showError(e.getMessage());
        }
    }

    private boolean hasBooking() {
        if (currentBooking == null) {
            showError("Please select a booking first.");
            return false;
        }
        return true;
    }

    private void recordPayment() {

        if (!hasBooking()) return;

        double total = totalCost(currentBooking);
        double paid = totalPaid(currentBooking);
        double balance = total - paid;

        if (balance <= 0.004) {
            JOptionPane.showMessageDialog(this, "This booking is already paid in full.");
            return;
        }

        PaymentDialog dialog = new PaymentDialog(balance);
        dialog.setVisible(true);

        if (!dialog.isSaved()) {
            return;
        }

        try {
            Payment payment = new Payment(
                nextPaymentId(),
                currentBooking.getBookingId(),
                dialog.getAmount(),
                dialog.getMethod(),
                LocalDate.now(),
                "Paid"
            );

            paymentService.addPayment(payment);
            renderReceipt();

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private String nextBookingId() {
        int max = 0;
        for (Booking b : bookingService.getAllBookings()) {
            max = Math.max(max, numberAfterDash(b.getBookingId()));
        }
        return String.format("B-%04d", max + 1);
    }

    private String nextPaymentId() {
        int max = 0;
        for (Payment p : paymentService.getAllPayments()) {
            max = Math.max(max, numberAfterDash(p.getPaymentId()));
        }
        return String.format("PAY-%04d", max + 1);
    }

    private static int numberAfterDash(String id) {
        if (id == null) return 0;
        int dash = id.lastIndexOf('-');
        String digits = (dash >= 0 ? id.substring(dash + 1) : id).replaceAll("\\D", "");
        if (digits.isEmpty() || digits.length() > 9) return 0;
        return Integer.parseInt(digits);
    }

    // =====================================================================
    //  Payment maths
    // =====================================================================

    private double totalCost(Booking booking) {
        Room room = roomService.findRoomByNumber(booking.getRoomNumber());
        return room == null ? 0 : room.getPricePerNight() * booking.getNumberOfNights();
    }

    private double totalPaid(Booking booking) {
        double sum = 0;
        for (Payment p : paymentService.getPaymentsByBooking(booking.getBookingId())) {
            if (p.isPaid()) {
                sum += p.getAmount();
            }
        }
        return sum;
    }

    private static String money(double value) {
        return value == Math.floor(value)
            ? String.format("%,.0f", value)
            : String.format("%,.2f", value);
    }

    // =====================================================================
    //  Receipt
    // =====================================================================

    private void renderReceipt() {

        if (receiptCard == null) {
            return;
        }

        receiptCard.removeAll();

        if (currentBooking == null) {
            JLabel empty = new JLabel("No booking selected", SwingConstants.CENTER);
            empty.setFont(new Font("SansSerif", Font.BOLD, 14));
            empty.setForeground(INK);

            JLabel hint = new JLabel("Confirm a reservation to see its receipt here.",
                                     SwingConstants.CENTER);
            hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
            hint.setForeground(MUTED);

            receiptCard.add(Box.createVerticalStrut(80));
            receiptCard.add(centered(empty));
            receiptCard.add(Box.createVerticalStrut(8));
            receiptCard.add(centered(hint));

            receiptCard.revalidate();
            receiptCard.repaint();
            return;
        }

        Booking b = currentBooking;
        Guest guest = guestService.findGuestById(b.getGuestId());
        Room room = roomService.findRoomByNumber(b.getRoomNumber());

        String guestName = guest == null ? "Unknown guest" : guest.getFullName();
        String roomType = room == null ? "Room" : room.getRoomType();
        double rate = room == null ? 0 : room.getPricePerNight();
        long nights = b.getNumberOfNights();
        double total = totalCost(b);
        double paid = totalPaid(b);

        // --- brand + status pill ---
        JPanel brand = new JPanel();
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setOpaque(false);

        JLabel herz = new JLabel("Herz");
        herz.setFont(new Font("Serif", Font.PLAIN, 26));
        herz.setForeground(BURGUNDY);

        JLabel hotel = new JLabel("HOTEL");
        hotel.setFont(new Font("SansSerif", Font.PLAIN, 8));
        hotel.setForeground(BURGUNDY);

        brand.add(herz);
        brand.add(hotel);

        String statusText;
        Color pillBg;
        Color pillFg;

        if (b.getBookingStatus().equalsIgnoreCase("Cancelled")) {
            statusText = "CANCELLED"; pillBg = DANGER_BG; pillFg = DANGER;
        } else if (total > 0 && paid >= total - 0.004) {
            statusText = "PAID"; pillBg = new Color(0xDD, 0xEE, 0xE4); pillFg = new Color(0x2F, 0x6B, 0x45);
        } else if (paid > 0) {
            statusText = "PARTIAL"; pillBg = new Color(0xF7, 0xEB, 0xCF); pillFg = new Color(0x8A, 0x64, 0x20);
        } else {
            statusText = "UNPAID"; pillBg = new Color(0xEC, 0xEC, 0xEC); pillFg = new Color(0x66, 0x66, 0x66);
        }

        JLabel pill = new JLabel(statusText, SwingConstants.CENTER);
        pill.setOpaque(true);
        pill.setBackground(pillBg);
        pill.setForeground(pillFg);
        pill.setFont(new Font("SansSerif", Font.PLAIN, 9));
        pill.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));

        JPanel pillWrap = new JPanel(new GridBagLayout());
        pillWrap.setOpaque(false);
        pillWrap.add(pill);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(brand, BorderLayout.WEST);
        top.add(pillWrap, BorderLayout.EAST);

        receiptCard.add(left(top));
        receiptCard.add(Box.createVerticalStrut(8));
        receiptCard.add(divider(1));
        receiptCard.add(Box.createVerticalStrut(14));

        // --- title + guest ---
        JLabel receiptTitle = new JLabel("PAYMENT RECEIPT");
        receiptTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        receiptTitle.setForeground(MUTED);
        receiptCard.add(left(receiptTitle));
        receiptCard.add(Box.createVerticalStrut(8));

        JLabel name = new JLabel(guestName);
        name.setFont(new Font("SansSerif", Font.BOLD, 19));
        name.setForeground(INK);
        receiptCard.add(left(name));
        receiptCard.add(Box.createVerticalStrut(12));
        receiptCard.add(divider(3));
        receiptCard.add(Box.createVerticalStrut(14));

        // --- stay summary ---
        JLabel summary = new JLabel("Stay Summary");
        summary.setFont(new Font("SansSerif", Font.BOLD, 13));
        summary.setForeground(INK);
        receiptCard.add(left(summary));
        receiptCard.add(Box.createVerticalStrut(12));

        JLabel stay = new JLabel(roomType + " · Room " + b.getRoomNumber());
        stay.setFont(new Font("SansSerif", Font.PLAIN, 12));
        stay.setForeground(INK);
        receiptCard.add(left(stay));
        receiptCard.add(Box.createVerticalStrut(8));

        JLabel dates = new JLabel(dateRange(b) + " · " + nights
            + (nights == 1 ? " night" : " nights"));
        dates.setFont(new Font("SansSerif", Font.PLAIN, 11));
        dates.setForeground(MUTED);
        receiptCard.add(left(dates));
        receiptCard.add(Box.createVerticalStrut(14));
        receiptCard.add(divider(3));
        receiptCard.add(Box.createVerticalStrut(12));

        // --- totals ---
        receiptCard.add(left(amountRow(money(rate) + " × " + nights + " nights",
                                       money(total), false, MUTED)));
        receiptCard.add(Box.createVerticalStrut(8));
        receiptCard.add(left(amountRow("Total stay cost", money(total), true, INK)));
        receiptCard.add(Box.createVerticalStrut(22));
        receiptCard.add(left(amountRow("Room charge", money(total), false, MUTED)));
        receiptCard.add(Box.createVerticalStrut(4));
        receiptCard.add(left(amountRow("Payment received", money(paid), false, MUTED)));

        if (total - paid > 0.004 && !b.getBookingStatus().equalsIgnoreCase("Cancelled")) {
            receiptCard.add(Box.createVerticalStrut(4));
            receiptCard.add(left(amountRow("Balance due", money(total - paid), true, DANGER)));
        }

        receiptCard.add(Box.createVerticalStrut(14));
        receiptCard.add(divider(3));
        receiptCard.add(Box.createVerticalStrut(10));

        // --- paid line ---
        JLabel paidLine = new JLabel(paidLineText(b));
        paidLine.setFont(new Font("SansSerif", Font.PLAIN, 10));
        paidLine.setForeground(MUTED);
        receiptCard.add(left(paidLine));

        receiptCard.add(Box.createVerticalGlue());
        receiptCard.add(Box.createVerticalStrut(22));

        JButton print = button("Print receipt", ButtonKind.PRIMARY);
        print.setPreferredSize(new Dimension(100, 40));
        print.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        print.setAlignmentX(Component.LEFT_ALIGNMENT);
        print.addActionListener(e -> printReceipt());
        receiptCard.add(print);

        receiptCard.add(Box.createVerticalStrut(14));

        JLabel thanks = new JLabel("Thank you for choosing Herz", SwingConstants.CENTER);
        thanks.setFont(new Font("SansSerif", Font.PLAIN, 10));
        thanks.setForeground(BURGUNDY);
        receiptCard.add(centered(thanks));

        receiptCard.revalidate();
        receiptCard.repaint();
    }

    private String dateRange(Booking b) {

        LocalDate in = b.getCheckInDate();
        LocalDate out = b.getCheckOutDate();

        if (in.getYear() == out.getYear() && in.getMonth() == out.getMonth()) {
            return in.getDayOfMonth() + "–" + out.format(DAY_FMT);
        }

        return in.format(DAY_FMT) + " – " + out.format(DAY_FMT);
    }

    private String paidLineText(Booking b) {

        LocalDate last = null;

        for (Payment p : paymentService.getPaymentsByBooking(b.getBookingId())) {
            if (p.isPaid() && (last == null || p.getPaymentDate().isAfter(last))) {
                last = p.getPaymentDate();
            }
        }

        if (last == null) {
            return "No payment recorded yet";
        }

        return "Paid " + last.format(DAY_FMT) + " · Received by " + receivedBy;
    }

    private JPanel amountRow(String label, String amount, boolean bold, Color color) {

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, bold ? 13 : 11));
        l.setForeground(color);

        JLabel a = new JLabel(amount);
        a.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, bold ? 13 : 11));
        a.setForeground(bold ? color : INK);

        row.add(l, BorderLayout.WEST);
        row.add(a, BorderLayout.EAST);
        return row;
    }

    private JPanel divider(int height) {
        JPanel line = new JPanel();
        line.setBackground(height > 1 ? ROW_LINE : CARD_BORDER);
        line.setPreferredSize(new Dimension(10, height));
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        line.setMinimumSize(new Dimension(10, height));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        return line;
    }

    private JComponent centered(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (c instanceof JLabel) {
            ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        }
        return c;
    }

    private void printReceipt() {

        PrinterJob job = PrinterJob.getPrinterJob();

        job.setPrintable(new Printable() {
            @Override
            public int print(Graphics g, PageFormat pf, int page) throws PrinterException {
                if (page > 0) {
                    return NO_SUCH_PAGE;
                }

                Graphics2D g2 = (Graphics2D) g;
                g2.translate(pf.getImageableX(), pf.getImageableY());

                double scale = Math.min(1.0, pf.getImageableWidth() / receiptCard.getWidth());
                g2.scale(scale, scale);

                receiptCard.printAll(g2);
                return PAGE_EXISTS;
            }
        });

        try {
            if (job.printDialog()) {
                job.print();
            }
        } catch (PrinterException e) {
            showError("Could not print the receipt: " + e.getMessage());
        }
    }

    // =====================================================================
    //  Record payment popup
    // =====================================================================

    private class PaymentDialog extends JDialog {

        private final JTextField amountField = formField("0");
        private final JComboBox<String> methodBox = new JComboBox<>(new String[] {
            "Cash", "Card", "Bank Transfer", "E-Wallet"
        });
        private final JLabel errorLabel = new JLabel(" ");
        private final double balance;

        private boolean saved;
        private double amount;
        private String method;

        PaymentDialog(double balance) {
            super(SwingUtilities.getWindowAncestor(BookingPanel.this),
                  "Record payment", ModalityType.APPLICATION_MODAL);

            this.balance = balance;

            setUndecorated(true);
            styleCombo(methodBox);

            amountField.setText(balance == Math.floor(balance)
                ? String.valueOf((long) balance)
                : String.format("%.2f", balance));

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBackground(Color.WHITE);
            root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(22, 22, 20, 22)
            ));

            Guest g = guestService.findGuestById(currentBooking.getGuestId());

            JLabel title = new JLabel("Record payment");
            title.setFont(new Font("SansSerif", Font.BOLD, 20));
            title.setForeground(INK);

            JLabel subtitle = new JLabel((g == null ? "Guest" : g.getFullName())
                + " · Room " + currentBooking.getRoomNumber()
                + " · Balance PHP " + money(balance));
            subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
            subtitle.setForeground(MUTED);

            root.add(left(title));
            root.add(Box.createVerticalStrut(6));
            root.add(left(subtitle));
            root.add(Box.createVerticalStrut(14));

            JSeparator sep = new JSeparator();
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            root.add(sep);
            root.add(Box.createVerticalStrut(16));

            JPanel row = new JPanel(new GridLayout(1, 2, 18, 0));
            row.setOpaque(false);
            row.add(labeled("Amount (PHP)", amountField));
            row.add(labeled("Payment method", methodBox));
            root.add(left(row));
            root.add(Box.createVerticalStrut(12));

            errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
            errorLabel.setForeground(DANGER);
            root.add(left(errorLabel));
            root.add(Box.createVerticalStrut(10));

            JButton cancel = button("Cancel", ButtonKind.OUTLINE);
            cancel.setPreferredSize(new Dimension(110, 40));
            cancel.addActionListener(e -> dispose());

            JButton save = button("Record payment", ButtonKind.PRIMARY);
            save.setPreferredSize(new Dimension(160, 40));
            save.addActionListener(e -> submit());

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
            buttons.setOpaque(false);
            buttons.add(cancel);
            buttons.add(save);
            root.add(left(buttons));

            getRootPane().setDefaultButton(save);
            setContentPane(root);
            setMinimumSize(new Dimension(480, 0));
            pack();
            setLocationRelativeTo(BookingPanel.this);
        }

        private void submit() {

            double value;

            try {
                value = Double.parseDouble(amountField.getText().trim().replace(",", ""));
            } catch (NumberFormatException e) {
                errorLabel.setText("Please enter a valid amount.");
                return;
            }

            if (value <= 0) {
                errorLabel.setText("Amount must be greater than zero.");
                return;
            }
            if (value > balance + 0.004) {
                errorLabel.setText("Amount cannot be more than the balance of PHP "
                    + money(balance) + ".");
                return;
            }

            amount = value;
            method = methodBox.getSelectedItem().toString();
            saved = true;
            dispose();
        }

        boolean isSaved() { return saved; }

        double getAmount() { return amount; }

        String getMethod() { return method; }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Herz Hotel", JOptionPane.ERROR_MESSAGE);
    }
}