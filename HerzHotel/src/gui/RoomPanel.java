package gui;

import exception.DuplicateRecordException;
import model.Room;
import service.RoomService;
import util.HerzTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class RoomPanel extends JPanel {

    private final RoomService roomService;

    private JTable roomTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> statusFilter;

    private JLabel totalRoomsLabel;
    private JLabel availableRoomsLabel;
    private JLabel occupiedRoomsLabel;
    private JLabel reservedRoomsLabel;

    public RoomPanel(RoomService roomService) {

        if (roomService == null) {
            throw new IllegalArgumentException(
                "RoomService is required."
            );
        }

        this.roomService = roomService;

        setLayout(new BorderLayout(20, 20));
        setBackground(HerzTheme.CREAM);
        setBorder(BorderFactory.createEmptyBorder(30, 35, 30, 35));

        add(createHeader(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);

        refreshTable();
    }

    private JPanel createHeader() {

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Room Management");
        title.setFont(HerzTheme.TITLE_FONT);
        title.setForeground(HerzTheme.DARK_TEXT);

        JLabel subtitle = new JLabel(
            "Manage room information, rates, and availability."
        );
        subtitle.setFont(HerzTheme.BODY_FONT);
        subtitle.setForeground(HerzTheme.SECONDARY_TEXT);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(6));
        titlePanel.add(subtitle);

        JButton addButton = HerzTheme.createPrimaryButton("+ Add Room");
        addButton.addActionListener(e -> addRoom());

        header.add(titlePanel, BorderLayout.WEST);
        header.add(addButton, BorderLayout.EAST);

        return header;
    }

    private JPanel createMainContent() {

        JPanel content = new JPanel(new BorderLayout(15, 15));
        content.setOpaque(false);

        content.add(createStatisticsPanel(), BorderLayout.NORTH);
        content.add(createTablePanel(), BorderLayout.CENTER);

        return content;
    }

    private JPanel createStatisticsPanel() {

        JPanel statistics = new JPanel(new GridLayout(1, 4, 15, 0));
        statistics.setOpaque(false);

        totalRoomsLabel = createStatValue();
        availableRoomsLabel = createStatValue();
        occupiedRoomsLabel = createStatValue();
        reservedRoomsLabel = createStatValue();

        statistics.add(createStatCard(
            "Total Rooms", totalRoomsLabel
        ));

        statistics.add(createStatCard(
            "Available", availableRoomsLabel
        ));

        statistics.add(createStatCard(
            "Occupied", occupiedRoomsLabel
        ));

        statistics.add(createStatCard(
            "Reserved", reservedRoomsLabel
        ));

        return statistics;
    }

    private JLabel createStatValue() {

        JLabel label = new JLabel("0");
        label.setFont(HerzTheme.TITLE_FONT);
        label.setForeground(HerzTheme.BURGUNDY);

        return label;
    }

    private JPanel createStatCard(String title, JLabel value) {

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(HerzTheme.WHITE);

        card.setBorder(BorderFactory.createCompoundBorder(
            HerzTheme.cardBorder(),
            BorderFactory.createEmptyBorder(15, 18, 15, 18)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(HerzTheme.SMALL_FONT);
        titleLabel.setForeground(HerzTheme.SECONDARY_TEXT);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(value);

        return card;
    }

    private JPanel createTablePanel() {

        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(HerzTheme.WHITE);

        panel.setBorder(BorderFactory.createCompoundBorder(
            HerzTheme.cardBorder(),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JPanel toolbar = new JPanel(new BorderLayout(10, 10));
        toolbar.setOpaque(false);

        JLabel tableTitle = new JLabel("Room Records");
        tableTitle.setFont(HerzTheme.HEADING_FONT);
        tableTitle.setForeground(HerzTheme.DARK_TEXT);

        JPanel searchPanel = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 8, 0)
        );
        searchPanel.setOpaque(false);

        searchField = HerzTheme.createTextField(15);
        searchField.setToolTipText(
            "Search room number, type, or status"
        );

        searchField.addActionListener(e -> refreshTable());

        statusFilter = new JComboBox<>(new String[] {
            "All Status",
            "Available",
            "Occupied",
            "Reserved",
            "Maintenance"
        });

        statusFilter.setFont(HerzTheme.BODY_FONT);
        statusFilter.addActionListener(e -> refreshTable());

        JButton searchButton = HerzTheme.createSecondaryButton("Search");
        searchButton.addActionListener(e -> refreshTable());

        JButton clearButton = HerzTheme.createSecondaryButton("Clear");
        clearButton.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            refreshTable();
        });

        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(statusFilter);
        searchPanel.add(clearButton);

        toolbar.add(tableTitle, BorderLayout.WEST);
        toolbar.add(searchPanel, BorderLayout.EAST);

        String[] columns = {
            "Room Number",
            "Room Type",
            "Price / Night",
            "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        roomTable = new JTable(tableModel);
        roomTable.setFont(HerzTheme.BODY_FONT);
        roomTable.setRowHeight(36);
        roomTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        roomTable.getTableHeader().setFont(HerzTheme.BUTTON_FONT);
        roomTable.getTableHeader().setBackground(HerzTheme.BLUSH);
        roomTable.getTableHeader().setForeground(HerzTheme.DARK_TEXT);

        JScrollPane scrollPane = new JScrollPane(roomTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(
            HerzTheme.BORDER
        ));

        JPanel actions = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 10, 5)
        );
        actions.setOpaque(false);

        JButton editButton = HerzTheme.createSecondaryButton("Edit Room");
        editButton.addActionListener(e -> editRoom());

        JButton deleteButton = HerzTheme.createSecondaryButton("Delete Room");
        deleteButton.addActionListener(e -> deleteRoom());

        actions.add(editButton);
        actions.add(deleteButton);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private void addRoom() {

        Room room = showRoomDialog(null);

        if (room == null) {
            return;
        }

        try {
            roomService.addRoom(room);
            refreshTable();

            JOptionPane.showMessageDialog(
                this,
                "Room added successfully."
            );

        } catch (DuplicateRecordException
                | IllegalArgumentException e) {

            showError(e.getMessage());
        }
    }

    private void editRoom() {

        Room selectedRoom = getSelectedRoom();

        if (selectedRoom == null) {
            showError("Please select a room to edit.");
            return;
        }

        Room updatedRoom = showRoomDialog(selectedRoom);

        if (updatedRoom == null) {
            return;
        }

        try {
            roomService.updateRoom(
                selectedRoom.getRoomNumber(),
                updatedRoom
            );

            refreshTable();

            JOptionPane.showMessageDialog(
                this,
                "Room updated successfully."
            );

        } catch (DuplicateRecordException
                | IllegalArgumentException e) {

            showError(e.getMessage());
        }
    }

    private void deleteRoom() {

        Room selectedRoom = getSelectedRoom();

        if (selectedRoom == null) {
            showError("Please select a room to delete.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Delete room " + selectedRoom.getRoomNumber() + "?",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {

            try {
                roomService.deleteRoom(
                    selectedRoom.getRoomNumber()
                );

                refreshTable();

                JOptionPane.showMessageDialog(
                    this,
                    "Room deleted successfully."
                );

            } catch (IllegalArgumentException e) {
                showError(e.getMessage());
            }
        }
    }

    private Room showRoomDialog(Room existingRoom) {

        JTextField numberField = new JTextField(15);

        JComboBox<String> typeField = new JComboBox<>(
            new String[] {
                "Standard",
                "Deluxe",
                "Suite"
            }
        );

        JTextField priceField = new JTextField(15);

        JComboBox<String> statusField = new JComboBox<>(
            new String[] {
                "Available",
                "Occupied",
                "Reserved",
                "Maintenance"
            }
        );

        if (existingRoom != null) {
            numberField.setText(existingRoom.getRoomNumber());
            typeField.setSelectedItem(existingRoom.getRoomType());
            priceField.setText(
                String.valueOf(existingRoom.getPricePerNight())
            );
            statusField.setSelectedItem(existingRoom.getStatus());
        }

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 12));

        form.add(new JLabel("Room Number:"));
        form.add(numberField);

        form.add(new JLabel("Room Type:"));
        form.add(typeField);

        form.add(new JLabel("Price per Night:"));
        form.add(priceField);

        form.add(new JLabel("Room Status:"));
        form.add(statusField);

        int result = JOptionPane.showConfirmDialog(
            this,
            form,
            existingRoom == null ? "Add Room" : "Edit Room",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        try {

            double price = Double.parseDouble(
                priceField.getText().trim()
            );

            return new Room(
                numberField.getText().trim(),
                typeField.getSelectedItem().toString(),
                price,
                statusField.getSelectedItem().toString()
            );

        } catch (NumberFormatException e) {

            showError("Please enter a valid room price.");

        } catch (IllegalArgumentException e) {

            showError(e.getMessage());
        }

        return null;
    }

    private Room getSelectedRoom() {

        int selectedRow = roomTable.getSelectedRow();

        if (selectedRow == -1) {
            return null;
        }

        String roomNumber = tableModel.getValueAt(
            selectedRow, 0
        ).toString();

        return roomService.findRoomByNumber(roomNumber);
    }

    public void refreshTable() {

        if (tableModel == null) {
            return;
        }

        tableModel.setRowCount(0);

        String keyword = searchField.getText().trim();

        String selectedStatus = statusFilter.getSelectedItem()
                .toString();

        for (Room room : roomService.searchRooms(keyword)) {

            if (!selectedStatus.equals("All Status")
                    && !room.getStatus().equalsIgnoreCase(selectedStatus)) {
                continue;
            }

            tableModel.addRow(new Object[] {
                room.getRoomNumber(),
                room.getRoomType(),
                String.format("₱%,.2f", room.getPricePerNight()),
                room.getStatus()
            });
        }

        totalRoomsLabel.setText(
            String.valueOf(roomService.getRoomCount())
        );

        availableRoomsLabel.setText(
            String.valueOf(roomService.getAvailableRoomCount())
        );

        occupiedRoomsLabel.setText(
            String.valueOf(roomService.getOccupiedRoomCount())
        );

        reservedRoomsLabel.setText(
            String.valueOf(roomService.getReservedRoomCount())
        );
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