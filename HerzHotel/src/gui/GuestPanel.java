package gui;

import exception.DuplicateRecordException;
import model.Guest;
import service.GuestService;
import util.HerzTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class GuestPanel extends JPanel {

    private final GuestService guestService;

    private JTable guestTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    public GuestPanel(GuestService guestService) {

        if (guestService == null) {
            throw new IllegalArgumentException(
                "GuestService is required."
            );
        }

        this.guestService = guestService;

        setLayout(new BorderLayout(20, 20));
        setBackground(HerzTheme.CREAM);
        setBorder(BorderFactory.createEmptyBorder(30, 35, 30, 35));

        add(createHeader(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);

        refreshTable();
    }

    private JPanel createHeader() {

        JPanel header = new JPanel(new BorderLayout(15, 15));
        header.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Guest Management");
        title.setFont(HerzTheme.TITLE_FONT);
        title.setForeground(HerzTheme.DARK_TEXT);

        JLabel subtitle = new JLabel(
            "Manage and organize your hotel guest records."
        );
        subtitle.setFont(HerzTheme.BODY_FONT);
        subtitle.setForeground(HerzTheme.SECONDARY_TEXT);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(6));
        titlePanel.add(subtitle);

        JButton addButton = HerzTheme.createPrimaryButton("+ Add Guest");
        addButton.addActionListener(e -> addGuest());

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

        JPanel toolbar = new JPanel(new BorderLayout(10, 10));
        toolbar.setOpaque(false);

        JLabel tableTitle = new JLabel("Guest Records");
        tableTitle.setFont(HerzTheme.HEADING_FONT);
        tableTitle.setForeground(HerzTheme.DARK_TEXT);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.setOpaque(false);

        searchField = HerzTheme.createTextField(20);
        searchField.setToolTipText("Search guest records");
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
            "Guest ID",
            "Full Name",
            "Email",
            "Phone Number",
            "Address"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        guestTable = new JTable(tableModel);
        guestTable.setFont(HerzTheme.BODY_FONT);
        guestTable.setRowHeight(35);
        guestTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        guestTable.getTableHeader().setFont(HerzTheme.BUTTON_FONT);
        guestTable.getTableHeader().setBackground(HerzTheme.BLUSH);
        guestTable.getTableHeader().setForeground(HerzTheme.DARK_TEXT);

        JScrollPane scrollPane = new JScrollPane(guestTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(
            HerzTheme.BORDER
        ));

        JPanel actionPanel = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 10, 5)
        );
        actionPanel.setOpaque(false);

        JButton editButton = HerzTheme.createSecondaryButton("Edit Guest");
        editButton.addActionListener(e -> editGuest());

        JButton deleteButton = HerzTheme.createSecondaryButton("Delete Guest");
        deleteButton.addActionListener(e -> deleteGuest());

        actionPanel.add(editButton);
        actionPanel.add(deleteButton);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void addGuest() {

        Guest guest = showGuestDialog(null);

        if (guest == null) {
            return;
        }

        try {
            guestService.addGuest(guest);
            refreshTable();

            JOptionPane.showMessageDialog(
                this,
                "Guest added successfully."
            );

        } catch (DuplicateRecordException
                | IllegalArgumentException e) {

            showError(e.getMessage());
        }
    }

    private void editGuest() {

        Guest selectedGuest = getSelectedGuest();

        if (selectedGuest == null) {
            showError("Please select a guest to edit.");
            return;
        }

        Guest updatedGuest = showGuestDialog(selectedGuest);

        if (updatedGuest == null) {
            return;
        }

        try {
            guestService.updateGuest(
                selectedGuest.getGuestId(),
                updatedGuest
            );

            refreshTable();

            JOptionPane.showMessageDialog(
                this,
                "Guest updated successfully."
            );

        } catch (DuplicateRecordException
                | IllegalArgumentException e) {

            showError(e.getMessage());
        }
    }

    private void deleteGuest() {

        Guest selectedGuest = getSelectedGuest();

        if (selectedGuest == null) {
            showError("Please select a guest to delete.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete "
                + selectedGuest.getFullName() + "?",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {

            try {
                guestService.deleteGuest(
                    selectedGuest.getGuestId()
                );

                refreshTable();

                JOptionPane.showMessageDialog(
                    this,
                    "Guest deleted successfully."
                );

            } catch (IllegalArgumentException e) {
                showError(e.getMessage());
            }
        }
    }

    private Guest showGuestDialog(Guest existingGuest) {

        JTextField idField = new JTextField(20);
        JTextField nameField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField phoneField = new JTextField(20);
        JTextField addressField = new JTextField(20);

        if (existingGuest != null) {
            idField.setText(existingGuest.getGuestId());
            nameField.setText(existingGuest.getFullName());
            emailField.setText(existingGuest.getEmail());
            phoneField.setText(existingGuest.getPhoneNumber());
            addressField.setText(existingGuest.getAddress());
        }

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 12));

        form.add(new JLabel("Guest ID:"));
        form.add(idField);

        form.add(new JLabel("Full Name:"));
        form.add(nameField);

        form.add(new JLabel("Email:"));
        form.add(emailField);

        form.add(new JLabel("Phone Number:"));
        form.add(phoneField);

        form.add(new JLabel("Address:"));
        form.add(addressField);

        int result = JOptionPane.showConfirmDialog(
            this,
            form,
            existingGuest == null ? "Add Guest" : "Edit Guest",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        try {
            return new Guest(
                idField.getText().trim(),
                nameField.getText().trim(),
                emailField.getText().trim(),
                phoneField.getText().trim(),
                addressField.getText().trim()
            );

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            return null;
        }
    }

    private Guest getSelectedGuest() {

        int selectedRow = guestTable.getSelectedRow();

        if (selectedRow == -1) {
            return null;
        }

        String guestId = tableModel.getValueAt(
            selectedRow, 0
        ).toString();

        return guestService.findGuestById(guestId);
    }

    public void refreshTable() {

        tableModel.setRowCount(0);

        String keyword = searchField == null
                ? ""
                : searchField.getText();

        for (Guest guest : guestService.searchGuests(keyword)) {

            tableModel.addRow(new Object[] {
                guest.getGuestId(),
                guest.getFullName(),
                guest.getEmail(),
                guest.getPhoneNumber(),
                guest.getAddress()
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