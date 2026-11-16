package gui;

import exception.DuplicateRecordException;
import exception.InvalidBookingException;
import model.Booking;
import model.Guest;
import model.Room;
import service.BookingService;
import service.GuestService;
import service.RoomService;
import util.HerzTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class BookingPanel extends JPanel {

    private final BookingService bookingService;
    private final GuestService guestService;
    private final RoomService roomService;

    private JTable bookingTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    public BookingPanel(
            BookingService bookingService,
            GuestService guestService,
            RoomService roomService) {

        if (bookingService == null
                || guestService == null
                || roomService == null) {
            throw new IllegalArgumentException(
                "All booking-related services are required."
            );
        }

        this.bookingService = bookingService;
        this.guestService = guestService;
        this.roomService = roomService;

        setLayout(new BorderLayout(20, 20));
        setBackground(HerzTheme.CREAM);
        setBorder(BorderFactory.createEmptyBorder(30, 35, 30, 35));

        add(createHeader(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);

        refreshTable();
    }

    private JPanel createHeader() {

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Booking Management");
        title.setFont(HerzTheme.TITLE_FONT);
        title.setForeground(HerzTheme.DARK_TEXT);

        JLabel subtitle = new JLabel(
            "Manage reservations, check-ins, and check-outs."
        );
        subtitle.setFont(HerzTheme.BODY_FONT);
        subtitle.setForeground(HerzTheme.SECONDARY_TEXT);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(6));
        titlePanel.add(subtitle);

        JButton addButton = HerzTheme.createPrimaryButton(
            "+ New Booking"
        );
        addButton.addActionListener(e -> addBooking());

        header.add(titlePanel, BorderLayout.WEST);
        header.add(addButton, BorderLayout.EAST);

        return header;
    }

    private JPanel createTablePanel() {

        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(HerzTheme.WHITE);

        panel.setBorder(BorderFactory.createCompoundBorder(
            HerzTheme.cardBorder(),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JLabel tableTitle = new JLabel("Booking Records");
        tableTitle.setFont(HerzTheme.HEADING_FONT);
        tableTitle.setForeground(HerzTheme.DARK_TEXT);

        JPanel searchPanel = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 8, 0)
        );
        searchPanel.setOpaque(false);

        searchField = HerzTheme.createTextField(18);

        searchField.setToolTipText(
            "Search booking ID, guest ID, room, or status"
        );

        searchField.addActionListener(e -> refreshTable());

        JButton searchButton = HerzTheme.createSecondaryButton("Search");
        searchButton.addActionListener(e -> refreshTable());

        JButton clearButton = HerzTheme.createSecondaryButton("Clear");
        clearButton.addActionListener(e -> {
            searchField.setText("");
            refreshTable();
        });

        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(clearButton);

        toolbar.add(tableTitle, BorderLayout.WEST);
        toolbar.add(searchPanel, BorderLayout.EAST);

        String[] columns = {
            "Booking ID",
            "Guest ID",
            "Room Number",
            "Check-in",
            "Check-out",
            "Nights",
            "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        bookingTable = new JTable(tableModel);
        bookingTable.setFont(HerzTheme.BODY_FONT);
        bookingTable.setRowHeight(36);

        bookingTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        bookingTable.getTableHeader().setFont(HerzTheme.BUTTON_FONT);
        bookingTable.getTableHeader().setBackground(HerzTheme.BLUSH);
        bookingTable.getTableHeader().setForeground(HerzTheme.DARK_TEXT);

        JScrollPane scrollPane = new JScrollPane(bookingTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(
            HerzTheme.BORDER
        ));

        JPanel actions = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 8, 5)
        );
        actions.setOpaque(false);

        JButton checkInButton = HerzTheme.createSecondaryButton("Check In");
        checkInButton.addActionListener(e -> checkInBooking());

        JButton checkOutButton = HerzTheme.createSecondaryButton("Check Out");
        checkOutButton.addActionListener(e -> checkOutBooking());

        JButton cancelButton = HerzTheme.createSecondaryButton("Cancel");
        cancelButton.addActionListener(e -> cancelBooking());

        actions.add(checkInButton);
        actions.add(checkOutButton);
        actions.add(cancelButton);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private void addBooking() {

        JTextField bookingIdField = new JTextField(15);

        JComboBox<String> guestField = new JComboBox<>();

        for (Guest guest : guestService.getAllGuests()) {
            guestField.addItem(
                guest.getGuestId() + " - " + guest.getFullName()
            );
        }

        JComboBox<String> roomField = new JComboBox<>();

        for (Room room : roomService.getAllRooms()) {
            if ("Available".equalsIgnoreCase(room.getStatus())) {
                roomField.addItem(room.getRoomNumber());
            }
        }

        JTextField checkInField = new JTextField(
            LocalDate.now().toString(), 15
        );

        JTextField checkOutField = new JTextField(
            LocalDate.now().plusDays(1).toString(), 15
        );

        if (guestField.getItemCount() == 0) {
            showError("Add a guest before creating a booking.");
            return;
        }

        if (roomField.getItemCount() == 0) {
            showError("There are no available rooms.");
            return;
        }

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 12));

        form.add(new JLabel("Booking ID:"));
        form.add(bookingIdField);

        form.add(new JLabel("Guest:"));
        form.add(guestField);

        form.add(new JLabel("Available Room:"));
        form.add(roomField);

        form.add(new JLabel("Check-in (YYYY-MM-DD):"));
        form.add(checkInField);

        form.add(new JLabel("Check-out (YYYY-MM-DD):"));
        form.add(checkOutField);

        int result = JOptionPane.showConfirmDialog(
            this,
            form,
            "Create New Booking",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {

            String guestSelection = guestField.getSelectedItem().toString();

            String guestId = guestSelection.split(" - ", 2)[0];

            Booking booking = new Booking(
                bookingIdField.getText().trim(),
                guestId,
                roomField.getSelectedItem().toString(),
                LocalDate.parse(checkInField.getText().trim()),
                LocalDate.parse(checkOutField.getText().trim()),
                "Reserved"
            );

            bookingService.addBooking(booking);

            refreshTable();

            JOptionPane.showMessageDialog(
                this,
                "Booking created successfully."
            );

        } catch (DateTimeParseException e) {

            showError("Enter valid dates using YYYY-MM-DD.");

        } catch (DuplicateRecordException
                | InvalidBookingException
                | IllegalArgumentException e) {

            showError(e.getMessage());
        }
    }

    private void checkInBooking() {

        Booking booking = getSelectedBooking();

        if (booking == null) {
            showError("Please select a booking.");
            return;
        }

        try {
            bookingService.checkInBooking(booking.getBookingId());

            refreshTable();

            JOptionPane.showMessageDialog(
                this,
                "Guest checked in successfully."
            );

        } catch (InvalidBookingException e) {
            showError(e.getMessage());
        }
    }

    private void checkOutBooking() {

        Booking booking = getSelectedBooking();

        if (booking == null) {
            showError("Please select a booking.");
            return;
        }

        try {
            bookingService.checkOutBooking(booking.getBookingId());

            refreshTable();

            JOptionPane.showMessageDialog(
                this,
                "Guest checked out successfully."
            );

        } catch (InvalidBookingException e) {
            showError(e.getMessage());
        }
    }

    private void cancelBooking() {

        Booking booking = getSelectedBooking();

        if (booking == null) {
            showError("Please select a booking.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Cancel booking " + booking.getBookingId() + "?",
            "Confirm Cancellation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {

            try {
                bookingService.cancelBooking(
                    booking.getBookingId()
                );

                refreshTable();

                JOptionPane.showMessageDialog(
                    this,
                    "Booking cancelled successfully."
                );

            } catch (InvalidBookingException e) {
                showError(e.getMessage());
            }
        }
    }

    private Booking getSelectedBooking() {

        int selectedRow = bookingTable.getSelectedRow();

        if (selectedRow == -1) {
            return null;
        }

        String bookingId = tableModel.getValueAt(
            selectedRow, 0
        ).toString();

        return bookingService.findBookingById(bookingId);
    }

    public void refreshTable() {

        if (tableModel == null) {
            return;
        }

        tableModel.setRowCount(0);

        String keyword = searchField.getText();

        for (Booking booking : bookingService.searchBookings(keyword)) {

            tableModel.addRow(new Object[] {
                booking.getBookingId(),
                booking.getGuestId(),
                booking.getRoomNumber(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getNumberOfNights(),
                booking.getBookingStatus()
            });
        }
    }

    private void showError(String message) {

        JOptionPane.showMessageDialog(
            this,
            message,
            "Herz Hotel",
            JOptionPane.ERROR_MESSAGE
        );
    }
}