package gui;

import exception.DuplicateRecordException;
import model.Room;
import service.RoomService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import static gui.UiKit.*;

public class RoomPanel extends JPanel {

    private static final int PAGE_SIZE = 8;

    private static final String[] TYPES = {"Standard", "Deluxe", "Suite"};
    private static final String[] STATUSES = {"Available", "Occupied", "Reserved", "Maintenance"};

    private final RoomService roomService;

    private JTable roomTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> typeFilter;
    private JComboBox<String> statusFilter;
    private JLabel countLabel;
    private JLabel showingLabel;
    private JLabel pageLabel;
    private JButton prevButton;
    private JButton nextButton;

    private final List<TabLabel> tabs = new ArrayList<>();
    private final List<Room> filtered = new ArrayList<>();
    private int currentPage = 0;
    private boolean syncing = false;

    public RoomPanel(RoomService roomService) {

        if (roomService == null) {
            throw new IllegalArgumentException("RoomService is required.");
        }

        this.roomService = roomService;

        setLayout(new BorderLayout(0, 14));
        setBackground(PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(26, 26, 26, 26));

        add(createHeader(), BorderLayout.NORTH);
        add(createCard(), BorderLayout.CENTER);

        refreshTable();
    }

    // =====================================================================
    //  Shared room rules (also used by the Bookings page)
    // =====================================================================

    /** Floor = room number without its last two digits (201 -> 2). */
    static String floorOf(Room room) {
        String digits = room.getRoomNumber().replaceAll("\\D", "");
        return digits.length() >= 3 ? digits.substring(0, digits.length() - 2) : "1";
    }

    /** Standard 2 guests, Deluxe 3, Suite 4. */
    static int capacityFor(String roomType) {
        if (roomType == null) return 2;
        if (roomType.equalsIgnoreCase("Suite")) return 4;
        if (roomType.equalsIgnoreCase("Deluxe")) return 3;
        return 2;
    }

    // =====================================================================
    //  Page layout
    // =====================================================================

    private JPanel createHeader() {

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);

        JLabel crumb = new JLabel("HOTEL OPERATIONS");
        crumb.setFont(new Font("SansSerif", Font.PLAIN, 10));
        crumb.setForeground(GOLD);
        crumb.setAlignmentX(LEFT_ALIGNMENT);

        JLabel title = new JLabel("Room Management");
        title.setFont(new Font("Serif", Font.PLAIN, 34));
        title.setForeground(INK);
        title.setAlignmentX(LEFT_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        titleBox.add(crumb);
        titleBox.add(Box.createVerticalStrut(14));
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(10));

        JButton addButton = button("+ Add room", ButtonKind.PRIMARY);
        addButton.setPreferredSize(new Dimension(112, 38));
        addButton.addActionListener(e -> addRoom());

        JPanel buttonWrap = new JPanel(new GridBagLayout());
        buttonWrap.setOpaque(false);
        buttonWrap.add(addButton);

        header.add(titleBox, BorderLayout.WEST);
        header.add(buttonWrap, BorderLayout.EAST);

        return header;
    }

    private JPanel createCard() {

        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(CARD_BORDER, 10, 1),
            BorderFactory.createEmptyBorder(18, 16, 14, 16)
        ));

        // --- Title row ---
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel cardTitle = new JLabel("Room Inventory");
        cardTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        cardTitle.setForeground(INK);

        countLabel = new JLabel("0 rooms");
        countLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        countLabel.setForeground(MUTED);

        titleRow.add(cardTitle, BorderLayout.WEST);
        titleRow.add(countLabel, BorderLayout.EAST);

        // --- Search + filters ---
        searchField = new PlaceholderField("Search room number or type...");
        searchField.setPreferredSize(new Dimension(300, 36));
        searchField.setBackground(FIELD_BG);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(FIELD_BG, 6, 1),
            BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));
        searchField.addActionListener(e -> resetAndRefresh());
        onChange(searchField, this::resetAndRefresh);

        typeFilter = new JComboBox<>(new String[] {
            "All types", "Standard", "Deluxe", "Suite"
        });
        styleFilter(typeFilter, 140);
        typeFilter.addActionListener(e -> resetAndRefresh());

        statusFilter = new JComboBox<>(new String[] {
            "All statuses", "Available", "Occupied", "Reserved", "Maintenance"
        });
        styleFilter(statusFilter, 150);
        statusFilter.addActionListener(e -> {
            if (!syncing) {
                updateTabsFromFilter();
                resetAndRefresh();
            }
        });

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        filters.setOpaque(false);
        filters.add(typeFilter);
        filters.add(statusFilter);

        JPanel searchRow = new JPanel(new BorderLayout(12, 0));
        searchRow.setOpaque(false);
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(filters, BorderLayout.EAST);

        // --- Tabs ---
        JPanel tabRow = new JPanel(new GridLayout(1, 4));
        tabRow.setOpaque(false);

        String[] tabNames = {"All rooms", "Available", "Occupied", "Reserved"};
        for (String name : tabNames) {
            TabLabel tab = new TabLabel(name);
            tabs.add(tab);
            tabRow.add(tab);
            tab.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectTab(name);
                }
            });
        }
        tabs.get(0).setSelected(true);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.add(titleRow);
        top.add(Box.createVerticalStrut(14));
        top.add(searchRow);
        top.add(Box.createVerticalStrut(16));
        top.add(tabRow);

        card.add(top, BorderLayout.NORTH);
        card.add(createTable(), BorderLayout.CENTER);
        card.add(createFooter(), BorderLayout.SOUTH);

        return card;
    }

    private void styleFilter(JComboBox<String> box, int width) {
        box.setFont(new Font("SansSerif", Font.BOLD, 11));
        box.setBackground(Color.WHITE);
        box.setPreferredSize(new Dimension(width, 36));
    }

    private JScrollPane createTable() {

        String[] columns = {
            "Room", "Type", "Floor", "Rate / night", "Status", "Capacity"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        roomTable = new JTable(tableModel);
        roomTable.setFont(new Font("SansSerif", Font.PLAIN, 11));
        roomTable.setForeground(INK);
        roomTable.setRowHeight(40);
        roomTable.setShowVerticalLines(false);
        roomTable.setShowHorizontalLines(true);
        roomTable.setGridColor(ROW_LINE);
        roomTable.setIntercellSpacing(new Dimension(0, 1));
        roomTable.setSelectionBackground(ROW_SELECTED);
        roomTable.setSelectionForeground(INK);
        roomTable.setFillsViewportHeight(true);
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomTable.setToolTipText("Double-click a room to edit");

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < roomTable.getColumnCount(); i++) {
            roomTable.getColumnModel().getColumn(i).setCellRenderer(center);
        }

        DefaultTableCellRenderer bold = new DefaultTableCellRenderer();
        bold.setHorizontalAlignment(SwingConstants.CENTER);
        bold.setFont(new Font("SansSerif", Font.BOLD, 11));
        roomTable.getColumnModel().getColumn(0).setCellRenderer(bold);

        roomTable.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());

        JTableHeader th = roomTable.getTableHeader();
        th.setReorderingAllowed(false);
        th.setPreferredSize(new Dimension(0, 42));
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

        roomTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && roomTable.getSelectedRow() != -1) {
                    editRoom();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(roomTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private JPanel createFooter() {

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        showingLabel = new JLabel("Showing 0 of 0 rooms");
        showingLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        showingLabel.setForeground(MUTED);

        prevButton = button("‹", ButtonKind.OUTLINE);
        prevButton.setPreferredSize(new Dimension(32, 32));
        prevButton.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                refreshTable();
            }
        });

        nextButton = button("›", ButtonKind.OUTLINE);
        nextButton.setPreferredSize(new Dimension(32, 32));
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

        JPanel pager = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        pager.setOpaque(false);
        pager.add(prevButton);
        pager.add(pageLabel);
        pager.add(nextButton);

        JButton editButton = button("Edit", ButtonKind.OUTLINE);
        editButton.setPreferredSize(new Dimension(70, 34));
        editButton.addActionListener(e -> editRoom());

        JButton deleteButton = button("Delete", ButtonKind.DANGER);
        deleteButton.setPreferredSize(new Dimension(80, 34));
        deleteButton.addActionListener(e -> {
            Room selected = getSelectedRoom();
            if (selected == null) {
                showError("Please select a room to delete.");
                return;
            }
            deleteRoom(selected);
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(editButton);
        actions.add(deleteButton);

        // Keep the pager centered: left and right sides get the same width
        JPanel leftSide = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        leftSide.setOpaque(false);
        leftSide.setPreferredSize(new Dimension(200, 34));
        leftSide.add(showingLabel);

        actions.setPreferredSize(new Dimension(200, 34));

        footer.add(leftSide, BorderLayout.WEST);
        footer.add(pager, BorderLayout.CENTER);
        footer.add(actions, BorderLayout.EAST);

        return footer;
    }

    // =====================================================================
    //  Tabs <-> status filter
    // =====================================================================

    private void selectTab(String name) {
        syncing = true;
        statusFilter.setSelectedItem(name.equals("All rooms") ? "All statuses" : name);
        syncing = false;
        updateTabsFromFilter();
        resetAndRefresh();
    }

    private void updateTabsFromFilter() {
        String selected = statusFilter.getSelectedItem().toString();
        String tabName = selected.equals("All statuses") ? "All rooms" : selected;

        for (TabLabel tab : tabs) {
            tab.setSelected(tab.getName().equals(tabName));
        }
    }

    // =====================================================================
    //  Actions
    // =====================================================================

    private void addRoom() {

        RoomDialog dialog = new RoomDialog(null);
        dialog.setVisible(true);

        Room room = dialog.getResult();

        if (room == null) {
            return;
        }

        try {
            roomService.addRoom(room);
            refreshTable();
            JOptionPane.showMessageDialog(this, "Room added successfully.");

        } catch (DuplicateRecordException | IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void editRoom() {

        Room selected = getSelectedRoom();

        if (selected == null) {
            showError("Please select a room to edit.");
            return;
        }

        RoomDialog dialog = new RoomDialog(selected);
        dialog.setVisible(true);

        if (dialog.isDeleteRequested()) {
            deleteRoom(selected);
            return;
        }

        Room updated = dialog.getResult();

        if (updated == null) {
            return;
        }

        try {
            roomService.updateRoom(selected.getRoomNumber(), updated);
            refreshTable();
            JOptionPane.showMessageDialog(this, "Room updated successfully.");

        } catch (DuplicateRecordException | IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void deleteRoom(Room room) {

        String status = room.getStatus();

        if (status.equalsIgnoreCase("Occupied") || status.equalsIgnoreCase("Reserved")) {
            showError("Room " + room.getRoomNumber()
                + " is " + status.toLowerCase()
                + " and cannot be deleted.");
            return;
        }

        boolean ok = confirm(
            this,
            "CONFIRM DELETION",
            "Delete room " + room.getRoomNumber() + "?",
            "This room will be permanently removed from the inventory.",
            "This action cannot be undone.",
            "Delete room"
        );

        if (!ok) {
            return;
        }

        try {
            roomService.deleteRoom(room.getRoomNumber());
            refreshTable();
            JOptionPane.showMessageDialog(this, "Room deleted successfully.");

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private Room getSelectedRoom() {

        int row = roomTable.getSelectedRow();

        if (row == -1) {
            return null;
        }

        int index = currentPage * PAGE_SIZE + roomTable.convertRowIndexToModel(row);

        return index < filtered.size() ? filtered.get(index) : null;
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

        String keyword = searchField.getText().trim();
        String type = typeFilter.getSelectedItem().toString();
        String status = statusFilter.getSelectedItem().toString();

        filtered.clear();

        for (Room room : roomService.searchRooms(keyword)) {

            if (!type.equals("All types") && !room.getRoomType().equalsIgnoreCase(type)) {
                continue;
            }
            if (!status.equals("All statuses") && !room.getStatus().equalsIgnoreCase(status)) {
                continue;
            }

            filtered.add(room);
        }

        if (currentPage >= pageCount()) {
            currentPage = pageCount() - 1;
        }

        tableModel.setRowCount(0);

        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filtered.size());

        for (int i = start; i < end; i++) {
            Room r = filtered.get(i);
            tableModel.addRow(new Object[] {
                r.getRoomNumber(),
                r.getRoomType(),
                floorOf(r),
                String.format("%,.0f", r.getPricePerNight()),
                r.getStatus(),
                capacityFor(r.getRoomType()) + " guests"
            });
        }

        int total = roomService.getRoomCount();

        countLabel.setText(total + (total == 1 ? " room" : " rooms"));

        tabs.get(0).setCount(total);
        tabs.get(1).setCount(roomService.getAvailableRoomCount());
        tabs.get(2).setCount(roomService.getOccupiedRoomCount());
        tabs.get(3).setCount(roomService.getReservedRoomCount());

        int shown = filtered.size();

        showingLabel.setText(shown == 0
            ? "No rooms found"
            : "Showing " + (start + 1) + "–" + end + " of " + shown + " rooms");
        pageLabel.setText(String.valueOf(currentPage + 1));
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage < pageCount() - 1);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Herz Hotel", JOptionPane.ERROR_MESSAGE);
    }

    // =====================================================================
    //  Add / Edit room popup
    // =====================================================================

    private class RoomDialog extends JDialog {

        private final boolean editing;

        private final JTextField numberField = formField("e.g. 201");
        private final JComboBox<String> typeBox = new JComboBox<>(TYPES);
        private final JTextField priceField = formField("e.g. 3500");
        private final JComboBox<String> statusBox = new JComboBox<>(STATUSES);
        private final JLabel errorLabel = new JLabel(" ");

        private Room result;
        private boolean deleteRequested;

        RoomDialog(Room existing) {
            super(SwingUtilities.getWindowAncestor(RoomPanel.this),
                  existing == null ? "Add a room" : "Edit room",
                  ModalityType.APPLICATION_MODAL);

            this.editing = existing != null;

            setUndecorated(true);
            styleCombo(typeBox);
            styleCombo(statusBox);

            if (editing) {
                numberField.setText(existing.getRoomNumber());
                typeBox.setSelectedItem(existing.getRoomType());
                priceField.setText(trimPrice(existing.getPricePerNight()));
                statusBox.setSelectedItem(existing.getStatus());
            }

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBackground(Color.WHITE);
            root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(22, 22, 20, 22)
            ));

            JLabel title = new JLabel(editing ? "Edit room" : "Add a room");
            title.setFont(new Font("SansSerif", Font.BOLD, 20));
            title.setForeground(INK);

            JLabel subtitle = new JLabel(editing
                ? "Update this room's details."
                : "Add a new room to the inventory.");
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

            JPanel row1 = new JPanel(new GridLayout(1, 2, 18, 0));
            row1.setOpaque(false);
            row1.add(labeled("Room number", numberField));
            row1.add(labeled("Room type", typeBox));
            root.add(left(row1));
            root.add(Box.createVerticalStrut(14));

            JPanel row2 = new JPanel(new GridLayout(1, 2, 18, 0));
            row2.setOpaque(false);
            row2.add(labeled("Price per night (PHP)", priceField));
            row2.add(labeled("Status", statusBox));
            root.add(left(row2));
            root.add(Box.createVerticalStrut(12));

            errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
            errorLabel.setForeground(DANGER);
            root.add(left(errorLabel));

            JPanel note = new JPanel(new BorderLayout());
            note.setBackground(FIELD_BG);
            note.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
            JLabel noteText = new JLabel("Floor and guest capacity are set from the room number and type.");
            noteText.setFont(new Font("SansSerif", Font.PLAIN, 10));
            noteText.setForeground(MUTED);
            note.add(noteText, BorderLayout.WEST);
            root.add(left(note));
            root.add(Box.createVerticalStrut(18));

            if (editing) {
                JButton delete = button("Delete room", ButtonKind.DANGER);
                delete.setPreferredSize(new Dimension(100, 38));
                delete.addActionListener(e -> {
                    deleteRequested = true;
                    dispose();
                });
                JPanel deleteRow = new JPanel(new BorderLayout());
                deleteRow.setOpaque(false);
                deleteRow.add(delete, BorderLayout.CENTER);
                root.add(left(deleteRow));
                root.add(Box.createVerticalStrut(10));
            }

            JButton cancel = button("Cancel", ButtonKind.OUTLINE);
            cancel.setPreferredSize(new Dimension(110, 40));
            cancel.addActionListener(e -> dispose());

            JButton save = button(editing ? "Save changes" : "Add room", ButtonKind.PRIMARY);
            save.setPreferredSize(new Dimension(editing ? 220 : 130, 40));
            save.addActionListener(e -> submit());

            JPanel buttons;
            if (editing) {
                buttons = new JPanel(new BorderLayout(12, 0));
                buttons.add(cancel, BorderLayout.WEST);
                buttons.add(save, BorderLayout.CENTER);
            } else {
                buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
                buttons.add(cancel);
                buttons.add(save);
            }
            buttons.setOpaque(false);
            root.add(left(buttons));

            getRootPane().setDefaultButton(save);
            setContentPane(root);
            setMinimumSize(new Dimension(460, 0));
            pack();
            setLocationRelativeTo(RoomPanel.this);
        }

        private void submit() {

            double price;

            try {
                price = Double.parseDouble(priceField.getText().trim().replace(",", ""));
            } catch (NumberFormatException e) {
                errorLabel.setText("Please enter a valid room price.");
                return;
            }

            try {
                result = new Room(
                    numberField.getText().trim(),
                    typeBox.getSelectedItem().toString(),
                    price,
                    statusBox.getSelectedItem().toString()
                );
                dispose();

            } catch (IllegalArgumentException e) {
                result = null;
                errorLabel.setText(e.getMessage());
            }
        }

        Room getResult() { return result; }

        boolean isDeleteRequested() { return deleteRequested; }
    }

    private static String trimPrice(double price) {
        return price == Math.floor(price)
            ? String.valueOf((long) price)
            : String.valueOf(price);
    }

    // =====================================================================
    //  Status pill + tab label
    // =====================================================================

    private static class StatusRenderer implements TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected,
                boolean focus, int row, int column) {

            JPanel cell = new JPanel(new GridBagLayout());
            cell.setOpaque(true);
            cell.setBackground(selected ? ROW_SELECTED : Color.WHITE);

            String status = value == null ? "" : value.toString();

            Color bg;
            Color fg;

            if (status.equalsIgnoreCase("Available")) {
                bg = new Color(0xE3, 0xF1, 0xE7); fg = new Color(0x2F, 0x6B, 0x45);
            } else if (status.equalsIgnoreCase("Reserved")) {
                bg = new Color(0xF7, 0xEB, 0xCF); fg = new Color(0x8A, 0x64, 0x20);
            } else if (status.equalsIgnoreCase("Maintenance")) {
                bg = new Color(0xEC, 0xEC, 0xEC); fg = new Color(0x66, 0x66, 0x66);
            } else {
                bg = new Color(0xE6, 0xEC, 0xF3); fg = new Color(0x3A, 0x4D, 0x63);
            }

            Color fill = bg;

            JLabel pill = new JLabel(status, SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(fill);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 6, 6));
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            pill.setOpaque(false);
            pill.setFont(new Font("SansSerif", Font.PLAIN, 10));
            pill.setForeground(fg);
            pill.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
            pill.setPreferredSize(new Dimension(90, 24));

            cell.add(pill);
            return cell;
        }
    }

    private static class TabLabel extends JLabel {

        private final String tabName;
        private boolean selected;

        TabLabel(String tabName) {
            super(tabName, SwingConstants.CENTER);
            this.tabName = tabName;
            setName(tabName);
            setFont(new Font("SansSerif", Font.PLAIN, 11));
            setForeground(MUTED);
            setBorder(BorderFactory.createEmptyBorder(8, 4, 10, 4));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setCount(int count) {
            setText(tabName + "  " + count);
        }

        void setSelected(boolean selected) {
            this.selected = selected;
            setForeground(selected ? INK : MUTED);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (selected) {
                g.setColor(GOLD);
                g.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        }
    }
}