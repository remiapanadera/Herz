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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static gui.UiKit.*;

/**
 * Screen 5: New Booking / Reservation.
 * - Requires every field on the Reservation details form (including Address) to be completed.
 * - Cascaded Room Selection: Category dropdown defaults to "Standard" and displays
 *   type, available count, and nightly rate; Room dropdown shows only the room number.
 * - Cash-only payment mode (Payment Method selector removed; payments default to Cash).
 * - Highlights the Total Due amount prominently before a reservation is confirmed.
 * - Top-right receipt search bar filters active (non-cancelled) bookings only,
 *   and keeps the receipt card empty when no guest is chosen.
 */
public class BookingPanel extends JPanel {

    private static final DateTimeFormatter DAY_FMT =
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private static final int PH_DIGITS = 12;
    private static final String PAYMENT_METHOD_CASH = "Cash";

    private final BookingService bookingService;
    private final GuestService guestService;
    private final RoomService roomService;
    private final PaymentService paymentService;

    private String receivedBy = "Front desk";
    private boolean walkIn = false;
    private boolean loading = false;
    private boolean submitting = false;

    // Reservation form inputs
    private JButton advanceButton;
    private JButton walkInButton;
    private JTextField guestNameField;
    private JTextField contactField;
    private JTextField addressField;
    private JComboBox<String> roomTypeFilterBox;
    private JComboBox<Room> roomBox;
    private JComboBox<Integer> occupantsBox;
    private JSpinner checkInSpinner;
    private JSpinner checkOutSpinner;
    private JTextField paymentAmountField;
    private JButton confirmButton;
    private JLabel stayNote;
    private JLabel totalDueLabel;

    // Receipt search bar + popup list + receipt card
    private JTextField receiptSearchField;
    private JPopupMenu receiptSearchPopup;
    private DefaultListModel<BookingItem> receiptListModel;
    private JList<BookingItem> receiptResultList;
    private final List<BookingItem> allBookingItems = new ArrayList<>();
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

    /** Name shown in the receipt's "Received by" line (the logged-in staff member). */
    public void setReceivedBy(String name) {
        if (name != null && !name.trim().isEmpty()) {
            receivedBy = name.trim();
        }
    }

    // =====================================================================
    //  Top bar: receipt search bar
    // =====================================================================

    private JPanel createTopBar() {

        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);

        JLabel sectionTitle = new JLabel("New Booking & Reservation");
        sectionTitle.setFont(new Font("Serif", Font.PLAIN, 26));
        sectionTitle.setForeground(INK);

        receiptSearchField = formField("Search receipt by guest, room, ID, or status...");
        receiptSearchField.setPreferredSize(new Dimension(360, 38));

        receiptListModel = new DefaultListModel<>();
        receiptResultList = new JList<>(receiptListModel);
        receiptResultList.setFont(new Font("SansSerif", Font.PLAIN, 12));
        receiptResultList.setForeground(INK);
        receiptResultList.setSelectionBackground(new Color(0xF3, 0xE4, 0xE4));
        receiptResultList.setSelectionForeground(INK);
        receiptResultList.setFixedCellHeight(34);
        receiptResultList.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JScrollPane popupScroll = new JScrollPane(receiptResultList);
        popupScroll.setBorder(BorderFactory.createEmptyBorder());
        popupScroll.setPreferredSize(new Dimension(360, 210));

        receiptSearchPopup = new JPopupMenu();
        receiptSearchPopup.setFocusable(false);
        receiptSearchPopup.setBorder(new RoundedBorder(CARD_BORDER, 8, 1));
        receiptSearchPopup.add(popupScroll);

        receiptResultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                BookingItem chosen = receiptResultList.getSelectedValue();
                if (chosen != null) {
                    selectReceiptBooking(chosen);
                }
            }
        });

        receiptSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterReceiptSearch(true); }
            @Override public void removeUpdate(DocumentEvent e) { filterReceiptSearch(true); }
            @Override public void changedUpdate(DocumentEvent e) { filterReceiptSearch(true); }
        });

        receiptSearchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                filterReceiptSearch(true);
            }
        });

        receiptSearchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int count = receiptListModel.getSize();
                if (count == 0) {
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (!receiptSearchPopup.isVisible()) {
                        filterReceiptSearch(true);
                    }
                    int next = Math.min(receiptResultList.getSelectedIndex() + 1, count - 1);
                    receiptResultList.setSelectedIndex(next);
                    receiptResultList.ensureIndexIsVisible(next);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    int prev = Math.max(receiptResultList.getSelectedIndex() - 1, 0);
                    receiptResultList.setSelectedIndex(prev);
                    receiptResultList.ensureIndexIsVisible(prev);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    BookingItem chosen = receiptResultList.getSelectedValue();
                    if (chosen == null && count > 0) {
                        chosen = receiptListModel.getElementAt(0);
                    }
                    if (chosen != null) {
                        selectReceiptBooking(chosen);
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    receiptSearchPopup.setVisible(false);
                }
            }
        });

        bar.add(sectionTitle, BorderLayout.WEST);
        bar.add(receiptSearchField, BorderLayout.EAST);

        return bar;
    }

    private void filterReceiptSearch(boolean showPopup) {
        if (loading || receiptSearchField == null) {
            return;
        }

        syncActiveBookingItems();

        String query = receiptSearchField.getText().trim().toLowerCase();

        // Keep the receipt card empty when no guest is chosen/searched
        if (query.isEmpty() && currentBooking != null) {
            currentBooking = null;
            renderReceipt();
        }

        receiptListModel.clear();

        for (BookingItem item : allBookingItems) {
            if (query.isEmpty() || item.matches(query)) {
                receiptListModel.addElement(item);
            }
        }

        if (receiptListModel.getSize() > 0) {
            receiptResultList.setSelectedIndex(0);
            int rows = Math.min(receiptListModel.getSize(), 6);
            int height = rows * 34 + 10;
            receiptSearchPopup.setPopupSize(receiptSearchField.getWidth(), height);

            if (showPopup && receiptSearchField.isShowing()) {
                receiptSearchPopup.show(
                    receiptSearchField,
                    0,
                    receiptSearchField.getHeight() + 4
                );
            }
        } else {
            receiptSearchPopup.setVisible(false);
        }
    }

    private void selectReceiptBooking(BookingItem item) {
        if (item == null || "Cancelled".equalsIgnoreCase(item.booking.getBookingStatus())) {
            return;
        }
        currentBooking = item.booking;
        loading = true;
        receiptSearchField.setText(item.toString());
        loading = false;
        receiptSearchPopup.setVisible(false);
        renderReceipt();
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

        // --- Title ---
        JLabel title = new JLabel("Reservation details");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(INK);

        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;
        gc.insets = new Insets(0, 0, 18, 0);
        card.add(title, gc);

        // --- Booking type toggle ---
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
        gc.insets = new Insets(0, 0, 20, 0);
        card.add(types, gc);

        // --- Direct Guest Name & Contact Number ---
        guestNameField = formField("Enter guest full name");
        ((AbstractDocument) guestNameField.getDocument()).setDocumentFilter(new NameFilter());

        contactField = formField("+63 9XX XXX XXXX");
        ((AbstractDocument) contactField.getDocument()).setDocumentFilter(new PhoneFilter());

        addPair(
            card, 2,
            labeled("Guest name", guestNameField),
            labeled("Contact number", contactField)
        );

        // --- Guest Address (full-width row) ---
        addressField = formField("Enter guest address");

        gc.gridx = 0; gc.gridy = 3; gc.gridwidth = 2;
        gc.insets = new Insets(0, 0, 18, 0);
        card.add(labeled("Address", addressField), gc);

        // --- Cascaded Room Selection: Category (with price, defaults to Standard) + Room Number only ---
        roomTypeFilterBox = new JComboBox<>(new String[] {
            "Standard", "Deluxe", "Suite"
        });
        styleCombo(roomTypeFilterBox);
        roomTypeFilterBox.setSelectedItem("Standard");
        roomTypeFilterBox.setPreferredSize(new Dimension(225, 40));
        roomTypeFilterBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean sel, boolean focus) {
                String raw = value == null ? "Standard" : value.toString();
                String label = formatTypeFilterLabel(raw);
                return super.getListCellRendererComponent(list, label, index, sel, focus);
            }
        });
        roomTypeFilterBox.addActionListener(e -> {
            if (!loading) {
                reloadAvailableRooms();
            }
        });

        roomBox = new JComboBox<>();
        styleCombo(roomBox);
        roomBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean sel, boolean focus) {
                String text = "No rooms available";
                if (value instanceof Room r) {
                    text = "Room " + r.getRoomNumber();
                }
                return super.getListCellRendererComponent(list, text, index, sel, focus);
            }
        });
        roomBox.addActionListener(e -> {
            if (!loading) {
                reloadOccupants();
                updateStayNote();
                updateConfirmButtonState();
            }
        });

        JPanel roomSelectorGroup = new JPanel(new BorderLayout(8, 0));
        roomSelectorGroup.setOpaque(false);
        roomSelectorGroup.add(roomTypeFilterBox, BorderLayout.WEST);
        roomSelectorGroup.add(roomBox, BorderLayout.CENTER);

        occupantsBox = new JComboBox<>();
        styleCombo(occupantsBox);
        occupantsBox.addActionListener(e -> updateConfirmButtonState());

        addPair(
            card, 4,
            labeled("Room selection (Category & Room)", roomSelectorGroup),
            labeled("Number of Occupants", occupantsBox)
        );

        // --- Check-in & Check-out dates ---
        checkInSpinner = dateSpinner(LocalDate.now().plusDays(1));
        checkOutSpinner = dateSpinner(LocalDate.now().plusDays(2));

        checkInSpinner.addChangeListener(e -> {
            LocalDate in = dateOf(checkInSpinner);
            if (!dateOf(checkOutSpinner).isAfter(in)) {
                checkOutSpinner.setValue(toDate(in.plusDays(1)));
            }
            updateStayNote();
            updateConfirmButtonState();
        });
        checkOutSpinner.addChangeListener(e -> {
            updateStayNote();
            updateConfirmButtonState();
        });

        addPair(
            card, 6,
            labeled("Check-in date", checkInSpinner),
            labeled("Check-out date", checkOutSpinner)
        );

        // --- Payment Amount (Cash only, full-width row) ---
        paymentAmountField = formField("Enter full cash amount (PHP)");
        ((AbstractDocument) paymentAmountField.getDocument()).setDocumentFilter(new AmountFilter());

        gc.gridx = 0; gc.gridy = 8; gc.gridwidth = 2;
        gc.insets = new Insets(0, 0, 18, 0);
        card.add(labeled("Payment Amount (PHP · Cash Only)", paymentAmountField), gc);

        DocumentListener formWatcher = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateConfirmButtonState(); }
            @Override public void removeUpdate(DocumentEvent e) { updateConfirmButtonState(); }
            @Override public void changedUpdate(DocumentEvent e) { updateConfirmButtonState(); }
        };
        guestNameField.getDocument().addDocumentListener(formWatcher);
        contactField.getDocument().addDocumentListener(formWatcher);
        addressField.getDocument().addDocumentListener(formWatcher);
        paymentAmountField.getDocument().addDocumentListener(formWatcher);

        // --- Highlighted Total Due Banner + Confirm Button ---
        JPanel totalBanner = new JPanel(new BorderLayout(12, 0));
        totalBanner.setBackground(new Color(0xFB, 0xF3, 0xE4));
        totalBanner.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(new Color(0xE2, 0xC6, 0x99), 8, 1),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        stayNote = new JLabel("");
        stayNote.setFont(new Font("SansSerif", Font.PLAIN, 11));
        stayNote.setForeground(INK);

        totalDueLabel = new JLabel("TOTAL DUE: PHP 0");
        totalDueLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        totalDueLabel.setForeground(BURGUNDY);

        totalBanner.add(stayNote, BorderLayout.WEST);
        totalBanner.add(totalDueLabel, BorderLayout.EAST);

        gc.gridx = 0; gc.gridy = 10; gc.gridwidth = 2;
        gc.insets = new Insets(4, 0, 14, 0);
        card.add(totalBanner, gc);

        confirmButton = button("Confirm booking", ButtonKind.PRIMARY);
        confirmButton.setPreferredSize(new Dimension(170, 40));
        confirmButton.addActionListener(e -> confirmBooking());

        JPanel confirmRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        confirmRow.setOpaque(false);
        confirmRow.add(confirmButton);

        gc.gridy = 11;
        gc.insets = new Insets(0, 0, 0, 0);
        card.add(confirmRow, gc);

        gc.gridy = 12; gc.weighty = 1; gc.fill = GridBagConstraints.BOTH;
        card.add(Box.createVerticalGlue(), gc);

        updateStayNote();
        updateConfirmButtonState();
        return card;
    }

    private String formatTypeFilterLabel(String category) {
        if (roomService == null) {
            return category;
        }
        List<Room> available = roomService.filterRoomsByStatus("Available");

        List<Room> matchingAvailable = new ArrayList<>();
        for (Room r : available) {
            if (r.getRoomType() != null && r.getRoomType().equalsIgnoreCase(category)) {
                matchingAvailable.add(r);
            }
        }

        List<Room> priceSource = matchingAvailable;
        if (priceSource.isEmpty()) {
            priceSource = new ArrayList<>();
            for (Room r : roomService.getAllRooms()) {
                if (r.getRoomType() != null && r.getRoomType().equalsIgnoreCase(category)) {
                    priceSource.add(r);
                }
            }
        }

        if (priceSource.isEmpty()) {
            return category + " (" + matchingAvailable.size() + ")";
        }

        double minPrice = Double.MAX_VALUE;
        double maxPrice = 0;
        for (Room r : priceSource) {
            minPrice = Math.min(minPrice, r.getPricePerNight());
            maxPrice = Math.max(maxPrice, r.getPricePerNight());
        }

        String priceText = Math.abs(maxPrice - minPrice) < 0.01
            ? "PHP " + money(minPrice) + " / night"
            : "PHP " + money(minPrice) + "–" + money(maxPrice) + " / night";

        return category + " (" + matchingAvailable.size() + ") · " + priceText;
    }

    private void addPair(JPanel card, int row, JComponent a, JComponent b) {

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 0.5;
        gc.gridwidth = 1;
        gc.gridy = row;

        gc.gridx = 0;
        gc.insets = new Insets(0, 0, 18, 12);
        card.add(a, gc);

        gc.gridx = 1;
        gc.insets = new Insets(0, 12, 18, 0);
        card.add(b, gc);
    }

    // =====================================================================
    //  Completeness & State Helpers
    // =====================================================================

    private List<String> getMissingFields() {
        List<String> missing = new ArrayList<>();

        if (guestNameField == null || guestNameField.getText().trim().isEmpty()) {
            missing.add("Guest name");
        }
        if (contactField == null || contactField.getText().trim().isEmpty()) {
            missing.add("Contact number");
        }
        if (addressField == null || addressField.getText().trim().isEmpty()) {
            missing.add("Address");
        }
        if (roomBox == null || roomBox.getSelectedItem() == null) {
            missing.add("Room selection");
        }
        if (occupantsBox == null || occupantsBox.getSelectedItem() == null) {
            missing.add("Number of Occupants");
        }
        if (checkInSpinner == null || checkInSpinner.getValue() == null) {
            missing.add("Check-in date");
        }
        if (checkOutSpinner == null || checkOutSpinner.getValue() == null) {
            missing.add("Check-out date");
        }
        if (paymentAmountField == null || paymentAmountField.getText().trim().isEmpty()) {
            missing.add("Payment Amount (PHP)");
        }

        return missing;
    }

    private void updateConfirmButtonState() {
        if (confirmButton == null) {
            return;
        }
        List<String> missing = getMissingFields();
        if (missing.isEmpty()) {
            confirmButton.setToolTipText("All fields completed. Click to confirm booking.");
        } else {
            confirmButton.setToolTipText("Please complete all fields: " + String.join(", ", missing));
        }
    }

    // =====================================================================
    //  Date, Room Capacity & Cost helpers
    // =====================================================================

    private static int maxOccupantsForRoom(Room room) {
        if (room == null || room.getRoomType() == null) {
            return 2;
        }
        String type = room.getRoomType().trim();
        if (type.equalsIgnoreCase("Suite")) {
            return 4;
        }
        if (type.equalsIgnoreCase("Deluxe")) {
            return 3;
        }
        return 2;
    }

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

    private double calculateSelectedTotal() {
        Room room = (Room) roomBox.getSelectedItem();
        if (room == null || checkInSpinner == null || checkOutSpinner == null) {
            return 0;
        }
        LocalDate in = walkIn ? LocalDate.now() : dateOf(checkInSpinner);
        LocalDate out = dateOf(checkOutSpinner);
        long nights = Math.max(ChronoUnit.DAYS.between(in, out), 0);
        return room.getPricePerNight() * nights;
    }

    private void updateStayNote() {
        if (stayNote == null || totalDueLabel == null
                || checkInSpinner == null || checkOutSpinner == null) {
            return;
        }

        LocalDate in = walkIn ? LocalDate.now() : dateOf(checkInSpinner);
        LocalDate out = dateOf(checkOutSpinner);
        long nights = Math.max(ChronoUnit.DAYS.between(in, out), 0);
        double expectedTotal = calculateSelectedTotal();

        Room room = (Room) roomBox.getSelectedItem();
        int maxOcc = maxOccupantsForRoom(room);

        stayNote.setText("Check-in: 2:00 PM · Check-out: 11:00 AM · Max " + maxOcc
            + " occupants · " + nights + (nights == 1 ? " night" : " nights"));

        totalDueLabel.setText("TOTAL DUE: PHP " + money(expectedTotal));
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
        updateConfirmButtonState();
    }

    // =====================================================================
    //  Reloading choices
    // =====================================================================

    public void refreshTable() {

        if (roomBox == null || receiptSearchField == null) {
            return;
        }

        if (roomTypeFilterBox != null) {
            if (roomTypeFilterBox.getSelectedItem() == null) {
                roomTypeFilterBox.setSelectedItem("Standard");
            }
            roomTypeFilterBox.repaint();
        }

        reloadAvailableRooms();
        reloadBookings(currentBooking == null ? null : currentBooking.getBookingId());
    }

    private void reloadAvailableRooms() {

        if (roomBox == null) {
            return;
        }

        loading = true;

        String chosenType = roomTypeFilterBox == null || roomTypeFilterBox.getSelectedItem() == null
            ? "Standard"
            : roomTypeFilterBox.getSelectedItem().toString();

        Room selectedRoom = (Room) roomBox.getSelectedItem();
        roomBox.removeAllItems();

        List<Room> available = new ArrayList<>(roomService.filterRoomsByStatus("Available"));
        available.sort(Comparator.comparing(Room::getRoomNumber));

        for (Room r : available) {
            if (r.getRoomType() != null && r.getRoomType().equalsIgnoreCase(chosenType)) {
                roomBox.addItem(r);
            }
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

        reloadOccupants();
        updateStayNote();
        updateConfirmButtonState();
    }

    private void reloadOccupants() {

        if (occupantsBox == null) {
            return;
        }

        Room room = (Room) roomBox.getSelectedItem();
        int maxCapacity = maxOccupantsForRoom(room);

        Integer previous = (Integer) occupantsBox.getSelectedItem();

        occupantsBox.removeAllItems();
        for (int i = 1; i <= maxCapacity; i++) {
            occupantsBox.addItem(i);
        }

        if (previous != null && previous >= 1 && previous <= maxCapacity) {
            occupantsBox.setSelectedItem(previous);
        } else {
            occupantsBox.setSelectedIndex(0);
        }
    }

    private void syncActiveBookingItems() {
        allBookingItems.clear();
        List<Booking> all = new ArrayList<>(bookingService.getAllBookings());
        for (int i = all.size() - 1; i >= 0; i--) {
            Booking b = all.get(i);
            if (!"Cancelled".equalsIgnoreCase(b.getBookingStatus())) {
                allBookingItems.add(new BookingItem(b));
            }
        }
    }

    private void reloadBookings(String selectId) {

        loading = true;

        syncActiveBookingItems();
        receiptListModel.clear();

        BookingItem toSelect = null;

        for (BookingItem item : allBookingItems) {
            receiptListModel.addElement(item);
            if (selectId != null && item.booking.getBookingId().equalsIgnoreCase(selectId)) {
                toSelect = item;
            }
        }

        // Leave currentBooking null when no guest is explicitly chosen
        currentBooking = toSelect == null ? null : toSelect.booking;
        receiptSearchField.setText(toSelect == null ? "" : toSelect.toString());

        loading = false;

        renderReceipt();
    }

    private class BookingItem {

        final Booking booking;

        BookingItem(Booking booking) {
            this.booking = booking;
        }

        boolean matches(String query) {
            if ("Cancelled".equalsIgnoreCase(booking.getBookingStatus())) {
                return false;
            }
            Guest g = guestService.findGuestById(booking.getGuestId());
            String name = g == null ? "" : g.getFullName().toLowerCase();
            return name.contains(query)
                || booking.getBookingId().toLowerCase().contains(query)
                || booking.getRoomNumber().toLowerCase().contains(query)
                || booking.getBookingStatus().toLowerCase().contains(query)
                || booking.getCheckInDate().format(DAY_FMT).toLowerCase().contains(query);
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
    //  Booking Confirmation, Guest Sync, and Full Cash Payment Validation
    // =====================================================================

    private void confirmBooking() {

        if (submitting) {
            return;
        }

        List<String> missing = getMissingFields();
        if (!missing.isEmpty()) {
            showError("All booking fields are required before confirming.\n\nPlease complete:\n• "
                + String.join("\n• ", missing));

            if (guestNameField.getText().trim().isEmpty()) {
                guestNameField.requestFocus();
            } else if (contactField.getText().trim().isEmpty()) {
                contactField.requestFocus();
            } else if (addressField.getText().trim().isEmpty()) {
                addressField.requestFocus();
            } else if (roomBox.getSelectedItem() == null) {
                roomBox.requestFocus();
            } else if (occupantsBox.getSelectedItem() == null) {
                occupantsBox.requestFocus();
            } else {
                paymentAmountField.requestFocus();
            }
            return;
        }

        String rawName = guestNameField.getText().trim();
        if (rawName.matches(".*\\d.*")) {
            showError("Guest name cannot contain numbers.");
            guestNameField.requestFocus();
            return;
        }
        if (!rawName.matches(".*\\p{L}.*")) {
            showError("Guest name must contain valid letters.");
            guestNameField.requestFocus();
            return;
        }

        String rawPhone = contactField.getText().trim();
        String phoneError = validatePhone(rawPhone);
        if (phoneError != null) {
            showError(phoneError);
            contactField.requestFocus();
            return;
        }

        String rawAddress = addressField.getText().trim();
        if (rawAddress.isEmpty()) {
            showError("Guest address is required.");
            addressField.requestFocus();
            return;
        }

        Room room = (Room) roomBox.getSelectedItem();
        if (room == null || !"Available".equalsIgnoreCase(room.getStatus())) {
            showError("Please select an available room.");
            roomBox.requestFocus();
            return;
        }

        Integer occupants = (Integer) occupantsBox.getSelectedItem();
        int maxAllowed = maxOccupantsForRoom(room);
        if (occupants == null || occupants < 1 || occupants > maxAllowed) {
            showError("Number of occupants exceeds the maximum of " + maxAllowed
                + " for a " + room.getRoomType() + " room.");
            occupantsBox.requestFocus();
            return;
        }

        LocalDate checkIn = walkIn ? LocalDate.now() : dateOf(checkInSpinner);
        LocalDate checkOut = dateOf(checkOutSpinner);

        if (checkIn.isBefore(LocalDate.now())) {
            showError("Check-in date cannot be in the past.");
            checkInSpinner.requestFocus();
            return;
        }
        if (!checkOut.isAfter(checkIn)) {
            showError("Check-out date must be after check-in date.");
            checkOutSpinner.requestFocus();
            return;
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        double expectedTotal = room.getPricePerNight() * nights;

        String rawAmount = paymentAmountField.getText().trim();
        if (!rawAmount.matches("\\d+(\\.\\d{1,2})?")) {
            showError("Payment amount must contain numbers only.");
            paymentAmountField.requestFocus();
            return;
        }

        double enteredAmount;
        try {
            enteredAmount = Double.parseDouble(rawAmount);
        } catch (NumberFormatException e) {
            showError("Please enter a valid numeric payment amount.");
            paymentAmountField.requestFocus();
            return;
        }

        if (enteredAmount <= 0) {
            showError("Payment amount must be greater than zero.");
            paymentAmountField.requestFocus();
            return;
        }

        if (Math.abs(enteredAmount - expectedTotal) > 0.004) {
            if (enteredAmount < expectedTotal) {
                showError("Partial payments are not allowed. Full cash payment of PHP "
                    + money(expectedTotal) + " is required to confirm this booking.");
            } else {
                showError("Payment amount (PHP " + money(enteredAmount)
                    + ") exceeds the total booking cost of PHP " + money(expectedTotal) + ".");
            }
            paymentAmountField.requestFocus();
            return;
        }

        submitting = true;

        try {
            Guest guest = createOrUpdateGuest(rawName, formatPhone(rawPhone), rawAddress);

            Booking booking = new Booking(
                nextBookingId(),
                guest.getGuestId(),
                room.getRoomNumber(),
                checkIn,
                checkOut,
                "Reserved",
                walkIn ? Booking.TYPE_WALK_IN : Booking.TYPE_ADVANCE,
                null,
                null
            );

            bookingService.addBooking(booking);

            Payment payment = new Payment(
                nextPaymentId(),
                booking.getBookingId(),
                enteredAmount,
                PAYMENT_METHOD_CASH,
                LocalDate.now(),
                "Paid"
            );

            paymentService.addPayment(payment);

            currentBooking = booking;
            clearForm();
            refreshTable();

            JOptionPane.showMessageDialog(
                this,
                "Reservation confirmed and paid in cash for " + guest.getFullName() + ".",
                "Booking Confirmed",
                JOptionPane.INFORMATION_MESSAGE
            );

        } catch (DuplicateRecordException
                | InvalidBookingException
                | IllegalArgumentException e) {

            showError(e.getMessage());
        } finally {
            submitting = false;
        }
    }

    private Guest createOrUpdateGuest(String fullName, String formattedPhone, String address)
            throws DuplicateRecordException {

        String normalizedName = fullName.trim().replaceAll("\\s+", " ");
        String normalizedAddress = address.trim().replaceAll("\\s+", " ");

        for (Guest existing : guestService.getAllGuests()) {
            if (existing.getFullName() != null
                    && existing.getFullName().trim().equalsIgnoreCase(normalizedName)) {

                Guest updated = new Guest(
                    existing.getGuestId(),
                    normalizedName,
                    existing.getEmail(),
                    formattedPhone,
                    normalizedAddress
                );
                guestService.updateGuest(existing.getGuestId(), updated);
                return existing;
            }
        }

        Guest created = new Guest(
            nextGuestId(),
            normalizedName,
            "",
            formattedPhone,
            normalizedAddress
        );
        guestService.addGuest(created);
        return created;
    }

    private void clearForm() {
        guestNameField.setText("");
        contactField.setText("");
        addressField.setText("");
        paymentAmountField.setText("");
        updateConfirmButtonState();
    }

    private String nextGuestId() {
        int max = 0;
        for (Guest g : guestService.getAllGuests()) {
            String id = g.getGuestId();
            if (id != null && id.matches("(?i)G-\\d+")) {
                max = Math.max(max, Integer.parseInt(id.substring(2)));
            }
        }
        return String.format("G-%04d", max + 1);
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

        if (currentBooking == null || "Cancelled".equalsIgnoreCase(currentBooking.getBookingStatus())) {
            JLabel empty = new JLabel("No booking selected", SwingConstants.CENTER);
            empty.setFont(new Font("SansSerif", Font.BOLD, 14));
            empty.setForeground(INK);

            JLabel hint = new JLabel("Confirm a reservation or search above to view its receipt.",
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

        // --- Brand + PAID status pill ---
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

        JLabel pill = new JLabel("PAID", SwingConstants.CENTER);
        pill.setOpaque(true);
        pill.setBackground(new Color(0xDD, 0xEE, 0xE4));
        pill.setForeground(new Color(0x2F, 0x6B, 0x45));
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

        // --- Title + Guest Name ---
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

        // --- Stay Summary + Explicit Check-in / Check-out Schedules ---
        JLabel summary = new JLabel("Stay Summary");
        summary.setFont(new Font("SansSerif", Font.BOLD, 13));
        summary.setForeground(INK);
        receiptCard.add(left(summary));
        receiptCard.add(Box.createVerticalStrut(10));

        JLabel stay = new JLabel(roomType + " · Room " + b.getRoomNumber()
            + " · " + nights + (nights == 1 ? " night" : " nights"));
        stay.setFont(new Font("SansSerif", Font.PLAIN, 12));
        stay.setForeground(INK);
        receiptCard.add(left(stay));
        receiptCard.add(Box.createVerticalStrut(10));

        receiptCard.add(left(detailRow(
            "Check-in",
            b.getCheckInDate().format(DAY_FMT) + " · 2:00 PM"
        )));
        receiptCard.add(Box.createVerticalStrut(6));

        receiptCard.add(left(detailRow(
            "Check-out",
            b.getCheckOutDate().format(DAY_FMT) + " · 11:00 AM"
        )));

        receiptCard.add(Box.createVerticalStrut(14));
        receiptCard.add(divider(3));
        receiptCard.add(Box.createVerticalStrut(12));

        // --- Payment Breakdown ---
        receiptCard.add(left(amountRow(
            "PHP " + money(rate) + " × " + nights + (nights == 1 ? " night" : " nights"),
            "PHP " + money(total),
            false,
            MUTED
        )));
        receiptCard.add(Box.createVerticalStrut(8));
        receiptCard.add(left(amountRow(
            "Total stay cost",
            "PHP " + money(total),
            true,
            INK
        )));
        receiptCard.add(Box.createVerticalStrut(14));
        receiptCard.add(left(amountRow(
            "Payment received",
            "PHP " + money(paid > 0 ? paid : total),
            false,
            MUTED
        )));
        receiptCard.add(Box.createVerticalStrut(6));
        receiptCard.add(left(detailRow(
            "Payment method",
            paymentMethodText(b)
        )));

        receiptCard.add(Box.createVerticalStrut(14));
        receiptCard.add(divider(3));
        receiptCard.add(Box.createVerticalStrut(10));

        // --- Paid line ---
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

    private String paymentMethodText(Booking b) {
        List<Payment> payments = paymentService.getPaymentsByBooking(b.getBookingId());
        for (int i = payments.size() - 1; i >= 0; i--) {
            if (payments.get(i).isPaid()) {
                return payments.get(i).getPaymentMethod();
            }
        }
        return PAYMENT_METHOD_CASH;
    }

    private String paidLineText(Booking b) {
        LocalDate last = null;
        for (Payment p : paymentService.getPaymentsByBooking(b.getBookingId())) {
            if (p.isPaid() && (last == null || p.getPaymentDate().isAfter(last))) {
                last = p.getPaymentDate();
            }
        }
        LocalDate paidDate = last != null ? last : b.getCheckInDate();
        return "Paid " + paidDate.format(DAY_FMT) + " · Received by " + receivedBy;
    }

    private JPanel detailRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setForeground(MUTED);

        JLabel v = new JLabel(value);
        v.setFont(new Font("SansSerif", Font.BOLD, 11));
        v.setForeground(INK);

        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        return row;
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
        if (c instanceof JLabel lbl) {
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
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
    //  Input Filters & Validation Helpers
    // =====================================================================

    private static String digitsOnly(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private static String validatePhone(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return "Contact number is required.";
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

    private static class NameFilter extends DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                throws BadLocationException {
            replace(fb, offset, 0, text, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text,
                            AttributeSet attrs) throws BadLocationException {

            if (text == null) {
                super.replace(fb, offset, length, text, attrs);
                return;
            }

            if (text.matches(".*\\d.*")) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }

            super.replace(fb, offset, length, text, attrs);
        }
    }

    private static class AmountFilter extends DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                throws BadLocationException {
            replace(fb, offset, 0, text, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text,
                            AttributeSet attrs) throws BadLocationException {

            if (text == null) {
                super.replace(fb, offset, length, text, attrs);
                return;
            }

            String current = fb.getDocument().getText(0, fb.getDocument().getLength());
            String next = current.substring(0, offset) + text
                    + current.substring(offset + length);

            if (!next.matches("\\d*(\\.\\d{0,2})?")) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }

            super.replace(fb, offset, length, text, attrs);
        }
    }

    private static class PhoneFilter extends DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                throws BadLocationException {
            replace(fb, offset, 0, text, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text,
                            AttributeSet attrs) throws BadLocationException {

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

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Herz Hotel", JOptionPane.ERROR_MESSAGE);
    }
}