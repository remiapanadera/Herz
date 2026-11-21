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
import java.util.Comparator;
import java.util.List;

import static gui.UiKit.*;

/**
 * Screen 4: Room Management.
 * Categorizes rooms by room type (All types, Standard, Deluxe, Suite) via top category tabs
 * with live counts, combined with search, status filtering (All rooms, Available, Occupied, Reserved),
 * and room editing.
 */
public class RoomPanel extends JPanel {

    private static final int PAGE_SIZE = 8;

    private static final String[] TYPES = {"Standard", "Deluxe", "Suite"};
    private static final String[] STATUSES = {"Available", "Occupied", "Reserved"};
    private static final String ALL_STATUS_LABEL = "All rooms";

    private final RoomService roomService;

    private JTable roomTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> statusFilter;
    private JLabel countLabel;
    private JLabel showingLabel;
    private JLabel pageLabel;
    private JButton prevButton;
    private JButton nextButton;

    private final List<TabLabel> typeTabs = new ArrayList<>();
    private final List<Room> filtered = new ArrayList<>();
    private String selectedTypeCategory = "All types";
    private int currentPage = 0;

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

    private static int typeOrder(String roomType) {
        if (roomType == null) return 99;
        if (roomType.equalsIgnoreCase("Standard")) return 1;
        if (roomType.equalsIgnoreCase("Deluxe")) return 2;
        if (roomType.equalsIgnoreCase("Suite")) return 3;
        return 4;
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

        titleBox.add(crumb);
        titleBox.add(Box.createVerticalStrut(14));
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(10));

        header.add(titleBox, BorderLayout.WEST);

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

        // --- Search + status filter ---
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

        statusFilter = new JComboBox<>(new String[] {
            ALL_STATUS_LABEL, "Available", "Occupied", "Reserved"
        });
        styleFilter(statusFilter, 150);
        statusFilter.addActionListener(e -> resetAndRefresh());

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        filters.setOpaque(false);
        filters.add(statusFilter);

        JPanel searchRow = new JPanel(new BorderLayout(12, 0));
        searchRow.setOpaque(false);
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(filters, BorderLayout.EAST);

        // --- Room Type Category Tabs ---
        JPanel tabRow = new JPanel(new GridLayout(1, 4));
        tabRow.setOpaque(false);

        String[] categoryNames = {"All types", "Standard", "Deluxe", "Suite"};
        for (String name : categoryNames) {
            TabLabel tab = new TabLabel(name);
            typeTabs.add(tab);
            tabRow.add(tab);
            tab.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectCategoryTab(name);
                }
            });
        }
        typeTabs.get(0).setSelected(true);

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

        roomTable.getColumnModel().getColumn(1).setCellRenderer(new TypeBadgeRenderer());
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
        editButton.setPreferredSize(new Dimension(80, 34));
        editButton.addActionListener(e -> editRoom());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(editButton);

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
    //  Room Type Category Tabs
    // =====================================================================

    private void selectCategoryTab(String name) {
        selectedTypeCategory = name;
        for (TabLabel tab : typeTabs) {
            tab.setSelected(tab.getName().equals(name));
        }
        resetAndRefresh();
    }

    // =====================================================================
    //  Actions
    // =====================================================================

    private void editRoom() {

        Room selected = getSelectedRoom();

        if (selected == null) {
            showError("Please select a room to edit.");
            return;
        }

        RoomDialog dialog = new RoomDialog(selected);
        dialog.setVisible(true);

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
        String status = statusFilter.getSelectedItem().toString();

        filtered.clear();

        for (Room room : roomService.searchRooms(keyword)) {

            if (!selectedTypeCategory.equals("All types")
                    && !room.getRoomType().equalsIgnoreCase(selectedTypeCategory)) {
                continue;
            }
            if (!status.equalsIgnoreCase(ALL_STATUS_LABEL)
                    && !room.getStatus().equalsIgnoreCase(status)) {
                continue;
            }

            filtered.add(room);
        }

        // Categorized order: Standard -> Deluxe -> Suite, then sorted by room number
        filtered.sort(
            Comparator.comparingInt((Room r) -> typeOrder(r.getRoomType()))
                      .thenComparing(Room::getRoomNumber)
        );

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
                r.getCapacity() + " occupants"
            });
        }

        List<Room> allRooms = roomService.getAllRooms();
        int total = allRooms.size();
        int standardCount = 0;
        int deluxeCount = 0;
        int suiteCount = 0;

        for (Room r : allRooms) {
            if ("Standard".equalsIgnoreCase(r.getRoomType())) {
                standardCount++;
            } else if ("Deluxe".equalsIgnoreCase(r.getRoomType())) {
                deluxeCount++;
            } else if ("Suite".equalsIgnoreCase(r.getRoomType())) {
                suiteCount++;
            }
        }

        countLabel.setText(total + (total == 1 ? " room" : " rooms"));

        typeTabs.get(0).setCount(total);
        typeTabs.get(1).setCount(standardCount);
        typeTabs.get(2).setCount(deluxeCount);
        typeTabs.get(3).setCount(suiteCount);

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
    //  Edit room popup
    // =====================================================================

    private class RoomDialog extends JDialog {

        private final JTextField numberField = formField("e.g. 201");
        private final JComboBox<String> typeBox = new JComboBox<>(TYPES);
        private final JTextField priceField = formField("e.g. 3500");
        private final JComboBox<String> statusBox = new JComboBox<>(STATUSES);
        private final JLabel errorLabel = new JLabel(" ");

        private Room result;

        RoomDialog(Room existing) {
            super(SwingUtilities.getWindowAncestor(RoomPanel.this),
                  "Edit room",
                  ModalityType.APPLICATION_MODAL);

            setUndecorated(true);
            styleCombo(typeBox);
            styleCombo(statusBox);

            numberField.setText(existing.getRoomNumber());
            typeBox.setSelectedItem(existing.getRoomType());
            priceField.setText(trimPrice(existing.getPricePerNight()));
            statusBox.setSelectedItem(existing.getStatus());

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBackground(Color.WHITE);
            root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(22, 22, 20, 22)
            ));

            JLabel title = new JLabel("Edit room");
            title.setFont(new Font("SansSerif", Font.BOLD, 20));
            title.setForeground(INK);

            JLabel subtitle = new JLabel("Update this room's details.");
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
            JLabel noteText = new JLabel("Floor and occupant capacity are set from the room number and type.");
            noteText.setFont(new Font("SansSerif", Font.PLAIN, 10));
            noteText.setForeground(MUTED);
            note.add(noteText, BorderLayout.WEST);
            root.add(left(note));
            root.add(Box.createVerticalStrut(18));

            JButton cancel = button("Cancel", ButtonKind.OUTLINE);
            cancel.setPreferredSize(new Dimension(110, 40));
            cancel.addActionListener(e -> dispose());

            JButton save = button("Save changes", ButtonKind.PRIMARY);
            save.setPreferredSize(new Dimension(220, 40));
            save.addActionListener(e -> submit());

            JPanel buttons = new JPanel(new BorderLayout(12, 0));
            buttons.add(cancel, BorderLayout.WEST);
            buttons.add(save, BorderLayout.CENTER);
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
    }

    private static String trimPrice(double price) {
        return price == Math.floor(price)
            ? String.valueOf((long) price)
            : String.valueOf(price);
    }

    // =====================================================================
    //  Room Type Badge + Status Pill + Tab Label
    // =====================================================================

    private static class TypeBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected,
                boolean focus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(
                table, value, selected, focus, row, column
            );
            String type = value == null ? "" : value.toString();
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setFont(new Font("SansSerif", Font.BOLD, 11));
            if ("Suite".equalsIgnoreCase(type)) {
                l.setForeground(BURGUNDY);
            } else if ("Deluxe".equalsIgnoreCase(type)) {
                l.setForeground(GOLD);
            } else {
                l.setForeground(INK);
            }
            return l;
        }
    }

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
            setFont(new Font("SansSerif", selected ? Font.BOLD : Font.PLAIN, 11));
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