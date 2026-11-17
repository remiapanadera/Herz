package gui;

import exception.DuplicateRecordException;
import model.Guest;
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
import java.util.ArrayList;
import java.util.List;

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
    private static final Color DANGER_BG    = new Color(0xF8, 0xE9, 0xE6);
    private static final Color DANGER_LINE  = new Color(0xEB, 0xCF, 0xC9);

    private static final int PAGE_SIZE = 8;

    private final GuestService guestService;

    private JTable guestTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> filterBox;
    private JLabel countLabel;
    private JLabel showingLabel;
    private JButton prevButton;
    private JButton nextButton;
    private JLabel pageLabel;

    private final List<Guest> filtered = new ArrayList<>();
    private int currentPage = 0;

    public GuestPanel(GuestService guestService) {

        if (guestService == null) {
            throw new IllegalArgumentException("GuestService is required.");
        }

        this.guestService = guestService;

        setLayout(new BorderLayout(0, 14));
        setBackground(PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(26, 26, 26, 26));

        add(createHeader(), BorderLayout.NORTH);
        add(createCard(), BorderLayout.CENTER);

        refreshTable();
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

        searchField = new PlaceholderField("Search by name, email or phone...");
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

        JButton addButton = styledButton("+ Add guest", ButtonKind.PRIMARY);
        addButton.setPreferredSize(new Dimension(120, 36));
        addButton.addActionListener(e -> addGuest());

        JButton editButton = styledButton("Edit", ButtonKind.OUTLINE);
        editButton.setPreferredSize(new Dimension(70, 36));
        editButton.addActionListener(e -> editGuest());

        JButton deleteButton = styledButton("Delete", ButtonKind.DANGER);
        deleteButton.setPreferredSize(new Dimension(80, 36));
        deleteButton.addActionListener(e -> {
            Guest selected = getSelectedGuest();
            if (selected == null) {
                showError("Please select a guest to delete.");
                return;
            }
            deleteGuest(selected);
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(filterBox);
        right.add(editButton);
        right.add(deleteButton);
        right.add(addButton);

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
            "Guest name", "Contact number", "Email", "Address"
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
        guestTable.setRowHeight(40);
        guestTable.setShowVerticalLines(false);
        guestTable.setShowHorizontalLines(true);
        guestTable.setGridColor(ROW_LINE);
        guestTable.setIntercellSpacing(new Dimension(0, 1));
        guestTable.setSelectionBackground(ROW_SELECTED);
        guestTable.setSelectionForeground(INK);
        guestTable.setFillsViewportHeight(true);
        guestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        guestTable.setToolTipText("Double-click a guest to edit");

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < guestTable.getColumnCount(); i++) {
            guestTable.getColumnModel().getColumn(i).setCellRenderer(center);
        }

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

    private void addGuest() {

        GuestDialog dialog = new GuestDialog(null, nextGuestId());
        dialog.setVisible(true);

        Guest guest = dialog.getResult();

        if (guest == null) {
            return;
        }

        try {
            guestService.addGuest(guest);
            refreshTable();

            JOptionPane.showMessageDialog(this, "Guest added successfully.");

        } catch (DuplicateRecordException | IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void editGuest() {

        Guest selected = getSelectedGuest();

        if (selected == null) {
            showError("Please select a guest to edit.");
            return;
        }

        GuestDialog dialog = new GuestDialog(selected, selected.getGuestId());
        dialog.setVisible(true);

        if (dialog.isDeleteRequested()) {
            deleteGuest(selected);
            return;
        }

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

    private void deleteGuest(Guest guest) {

        ConfirmDeleteDialog confirm = new ConfirmDeleteDialog(guest.getFullName());
        confirm.setVisible(true);

        if (!confirm.isConfirmed()) {
            return;
        }

        try {
            guestService.deleteGuest(guest.getGuestId());
            refreshTable();

            JOptionPane.showMessageDialog(this, "Guest deleted successfully.");

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private Guest getSelectedGuest() {

        int row = guestTable.getSelectedRow();

        if (row == -1) {
            return null;
        }

        int index = currentPage * PAGE_SIZE + guestTable.convertRowIndexToModel(row);

        return index < filtered.size() ? filtered.get(index) : null;
    }

    /** Generates the next "G-0001" style ID so the form doesn't need an ID field. */
    private String nextGuestId() {

        int max = 0;

        for (Guest g : guestService.searchGuests("")) {
            String id = g.getGuestId();
            if (id != null && id.matches("(?i)G-\\d+")) {
                max = Math.max(max, Integer.parseInt(id.substring(2)));
            }
        }

        return String.format("G-%04d", max + 1);
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

        for (Guest g : guestService.searchGuests(keyword)) {

            boolean noEmail = g.getEmail() == null || g.getEmail().isBlank();
            boolean noPhone = g.getPhoneNumber() == null || g.getPhoneNumber().isBlank();

            if (filter.equals("Missing email") && !noEmail) continue;
            if (filter.equals("Missing phone") && !noPhone) continue;

            filtered.add(g);
        }

        if (currentPage >= pageCount()) {
            currentPage = pageCount() - 1;
        }

        tableModel.setRowCount(0);

        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filtered.size());

        for (int i = start; i < end; i++) {
            Guest g = filtered.get(i);
            tableModel.addRow(new Object[] {
                g.getFullName(),
                g.getPhoneNumber(),
                g.getEmail(),
                g.getAddress()
            });
        }

        int total = filtered.size();

        countLabel.setText(total + (total == 1 ? " guest profile" : " guest profiles"));
        showingLabel.setText(total == 0
            ? "No guests found"
            : "Showing " + (start + 1) + "\u2013" + end + " of " + total + " guests");
        pageLabel.setText(String.valueOf(currentPage + 1));
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage < pageCount() - 1);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Herz Hotel", JOptionPane.ERROR_MESSAGE);
    }

    // =====================================================================
    //  Dialogs
    // =====================================================================

    /** Add / Edit guest form (first + last name, email, contact, address). */
    private class GuestDialog extends JDialog {

        private final String guestId;
        private final boolean editing;

        private final JTextField firstField = formField("Enter first name");
        private final JTextField lastField = formField("Enter last name");
        private final JTextField emailField = formField("guest@example.com");
        private final JTextField phoneField = formField("Include country code");
        private final JTextField addressField = formField("Enter Address");
        private final JLabel errorLabel = new JLabel(" ");

        private Guest result;
        private boolean deleteRequested;

        GuestDialog(Guest existing, String guestId) {
            super(SwingUtilities.getWindowAncestor(GuestPanel.this),
                  existing == null ? "Add a guest" : "Edit guest",
                  ModalityType.APPLICATION_MODAL);

            this.guestId = guestId;
            this.editing = existing != null;

            setUndecorated(true);

            ((javax.swing.text.AbstractDocument) phoneField.getDocument())
                .setDocumentFilter(new PhoneFilter());

            if (editing) {
                String full = existing.getFullName() == null ? "" : existing.getFullName().trim();
                int split = full.lastIndexOf(' ');
                firstField.setText(split < 0 ? full : full.substring(0, split));
                lastField.setText(split < 0 ? "" : full.substring(split + 1));
                emailField.setText(existing.getEmail());
                phoneField.setText(existing.getPhoneNumber());
                addressField.setText(existing.getAddress());
            }

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBackground(Color.WHITE);
            root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(22, 22, 20, 22)
            ));

            JLabel title = new JLabel(editing ? "Edit guest" : "Add a guest");
            title.setFont(new Font("SansSerif", Font.BOLD, 20));
            title.setForeground(INK);

            JLabel subtitle = new JLabel(editing
                ? "Update this guest's profile."
                : "Create a guest profile for a future stay.");
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

            if (editing) {
                JPanel note = new JPanel(new BorderLayout());
                note.setBackground(FIELD_BG);
                note.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
                JLabel noteText = new JLabel("Guest details are used only for hotel operations.");
                noteText.setFont(new Font("SansSerif", Font.PLAIN, 10));
                noteText.setForeground(MUTED);
                note.add(noteText, BorderLayout.WEST);
                root.add(left(note));
                root.add(Box.createVerticalStrut(18));

                JButton delete = styledButton("Delete guest", ButtonKind.DANGER);
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
            } else {
                root.add(Box.createVerticalStrut(10));
            }

            JButton cancel = styledButton("Cancel", ButtonKind.OUTLINE);
            cancel.setPreferredSize(new Dimension(110, 40));
            cancel.addActionListener(e -> dispose());

            JButton save = styledButton(editing ? "Save changes" : "Add guest", ButtonKind.PRIMARY);
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
            setMinimumSize(new Dimension(editing ? 400 : 520, 0));
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

        boolean isDeleteRequested() { return deleteRequested; }
    }

    /** "CONFIRM DELETION" dialog. */
    private class ConfirmDeleteDialog extends JDialog {

        private boolean confirmed;

        ConfirmDeleteDialog(String guestName) {
            super(SwingUtilities.getWindowAncestor(GuestPanel.this),
                  "Confirm Deletion", ModalityType.APPLICATION_MODAL);

            setUndecorated(true);

            JPanel root = new JPanel();
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBackground(Color.WHITE);
            root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(24, 26, 22, 26)
            ));

            JLabel tag = new JLabel("CONFIRM DELETION");
            tag.setFont(new Font("SansSerif", Font.BOLD, 10));
            tag.setForeground(DANGER);
            tag.setOpaque(true);
            tag.setBackground(DANGER_BG);
            tag.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));

            JLabel title = new JLabel("Delete " + guestName + "?");
            title.setFont(new Font("SansSerif", Font.BOLD, 20));
            title.setForeground(INK);

            JLabel line1 = new JLabel("This guest profile will be permanently removed.");
            line1.setFont(new Font("SansSerif", Font.PLAIN, 12));
            line1.setForeground(MUTED);

            JLabel warning = new JLabel("This action cannot be undone.");
            warning.setFont(new Font("SansSerif", Font.PLAIN, 12));
            warning.setForeground(DANGER);

            root.add(left(tag));
            root.add(Box.createVerticalStrut(18));
            root.add(left(title));
            root.add(Box.createVerticalStrut(16));
            root.add(left(line1));
            root.add(Box.createVerticalStrut(14));
            root.add(left(warning));
            root.add(Box.createVerticalStrut(24));

            JButton cancel = styledButton("Cancel", ButtonKind.OUTLINE);
            cancel.setPreferredSize(new Dimension(110, 40));
            cancel.addActionListener(e -> dispose());

            JButton delete = styledButton("Delete guest", ButtonKind.PRIMARY);
            delete.setPreferredSize(new Dimension(150, 40));
            delete.addActionListener(e -> {
                confirmed = true;
                dispose();
            });

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
            buttons.setOpaque(false);
            buttons.add(cancel);
            buttons.add(delete);
            root.add(left(buttons));

            setContentPane(root);
            setMinimumSize(new Dimension(440, 0));
            pack();
            setLocationRelativeTo(GuestPanel.this);
        }

        boolean isConfirmed() { return confirmed; }
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
    //  Philippine mobile number rules: +63 9XX XXX XXXX (12 digits total)
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