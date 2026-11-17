package gui;

import model.Booking;
import model.Payment;
import service.BookingService;
import service.PaymentService;
import util.HerzTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;

public class PaymentPanel extends JPanel {

    private final PaymentService paymentService;
    private final BookingService bookingService;

    private JTable paymentTable;
    private DefaultTableModel tableModel;
    private JLabel totalRevenueLabel;

    public PaymentPanel(
            PaymentService paymentService,
            BookingService bookingService) {

        if (paymentService == null || bookingService == null) {
            throw new IllegalArgumentException(
                "Payment and booking services are required."
            );
        }

        this.paymentService = paymentService;
        this.bookingService = bookingService;

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

        JLabel title = new JLabel("Payment Management");
        title.setFont(HerzTheme.TITLE_FONT);
        title.setForeground(HerzTheme.DARK_TEXT);

        JLabel subtitle = new JLabel(
            "Record payments and monitor booking transactions."
        );
        subtitle.setFont(HerzTheme.BODY_FONT);
        subtitle.setForeground(HerzTheme.SECONDARY_TEXT);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(6));
        titlePanel.add(subtitle);

        JButton addButton = HerzTheme.createPrimaryButton(
            "+ Record Payment"
        );

        addButton.addActionListener(e -> addPayment());

        header.add(titlePanel, BorderLayout.WEST);
        header.add(addButton, BorderLayout.EAST);

        return header;
    }

    private JPanel createMainContent() {

        JPanel content = new JPanel(new BorderLayout(15, 15));
        content.setOpaque(false);

        JPanel summaryCard = new JPanel(new BorderLayout());
        summaryCard.setBackground(HerzTheme.WHITE);

        summaryCard.setBorder(BorderFactory.createCompoundBorder(
            HerzTheme.cardBorder(),
            BorderFactory.createEmptyBorder(18, 22, 18, 22)
        ));

        JLabel summaryTitle = new JLabel("Total Recorded Payments");
        summaryTitle.setFont(HerzTheme.BODY_FONT);
        summaryTitle.setForeground(HerzTheme.SECONDARY_TEXT);

        totalRevenueLabel = new JLabel("₱0.00");
        totalRevenueLabel.setFont(HerzTheme.TITLE_FONT);
        totalRevenueLabel.setForeground(HerzTheme.BURGUNDY);

        JPanel summaryText = new JPanel();
        summaryText.setLayout(new BoxLayout(summaryText, BoxLayout.Y_AXIS));
        summaryText.setOpaque(false);

        summaryText.add(summaryTitle);
        summaryText.add(Box.createVerticalStrut(8));
        summaryText.add(totalRevenueLabel);

        summaryCard.add(summaryText, BorderLayout.WEST);

        content.add(summaryCard, BorderLayout.NORTH);
        content.add(createTablePanel(), BorderLayout.CENTER);

        return content;
    }

    private JPanel createTablePanel() {

        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(HerzTheme.WHITE);

        panel.setBorder(BorderFactory.createCompoundBorder(
            HerzTheme.cardBorder(),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel tableTitle = new JLabel("Payment Records");
        tableTitle.setFont(HerzTheme.HEADING_FONT);
        tableTitle.setForeground(HerzTheme.DARK_TEXT);

        String[] columns = {
            "Payment ID",
            "Booking ID",
            "Amount",
            "Payment Method",
            "Payment Date",
            "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        paymentTable = new JTable(tableModel);
        paymentTable.setFont(HerzTheme.BODY_FONT);
        paymentTable.setRowHeight(36);

        paymentTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        paymentTable.getTableHeader().setFont(HerzTheme.BUTTON_FONT);
        paymentTable.getTableHeader().setBackground(HerzTheme.BLUSH);
        paymentTable.getTableHeader().setForeground(HerzTheme.DARK_TEXT);

        JScrollPane scrollPane = new JScrollPane(paymentTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(
            HerzTheme.BORDER
        ));

        JButton paidButton = HerzTheme.createPrimaryButton("Mark as Paid");
        paidButton.addActionListener(e -> markAsPaid());

        JButton pendingButton = HerzTheme.createSecondaryButton(
            "Mark as Pending"
        );
        pendingButton.addActionListener(e -> markAsPending());

        JButton refreshButton = HerzTheme.createSecondaryButton("Refresh");
        refreshButton.addActionListener(e -> refreshTable());

        JPanel actions = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 8, 5)
        );
        actions.setOpaque(false);

        actions.add(refreshButton);
        actions.add(pendingButton);
        actions.add(paidButton);

        panel.add(tableTitle, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private void addPayment() {

        JTextField paymentIdField = new JTextField(15);

        JComboBox<String> bookingField = new JComboBox<>();

        for (Booking booking : bookingService.getAllBookings()) {

            if (!"Cancelled".equalsIgnoreCase(
                    booking.getBookingStatus())) {

                bookingField.addItem(
                    booking.getBookingId()
                );
            }
        }

        JTextField amountField = new JTextField(15);

        String[] methods = {
            "Cash",
            "Card",
            "Bank Transfer",
            "E-Wallet"
        };

        JComboBox<String> methodField = new JComboBox<>(methods);

        if (bookingField.getItemCount() == 0) {
            showError("Create a booking before recording a payment.");
            return;
        }

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 12));

        form.add(new JLabel("Payment ID:"));
        form.add(paymentIdField);

        form.add(new JLabel("Booking ID:"));
        form.add(bookingField);

        form.add(new JLabel("Amount (PHP):"));
        form.add(amountField);

        form.add(new JLabel("Payment Method:"));
        form.add(methodField);

        int result = JOptionPane.showConfirmDialog(
            this,
            form,
            "Record New Payment",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {

            String paymentId = paymentIdField.getText().trim();
            String bookingId = bookingField.getSelectedItem().toString();

            double amount = Double.parseDouble(
                amountField.getText().trim()
            );

            Payment payment = new Payment(
                paymentId,
                bookingId,
                amount,
                methodField.getSelectedItem().toString(),
                LocalDate.now(),
                "Pending"
            );

            paymentService.addPayment(payment);

            refreshTable();

            JOptionPane.showMessageDialog(
                this,
                "Payment record created successfully."
            );

        } catch (NumberFormatException e) {

            showError("Please enter a valid payment amount.");

        } catch (IllegalArgumentException e) {

            showError(e.getMessage());
        }
    }

    private void markAsPaid() {

        Payment payment = getSelectedPayment();

        if (payment == null) {
            showError("Please select a payment record.");
            return;
        }

        try {

            paymentService.markPaymentAsPaid(
                payment.getPaymentId()
            );

            refreshTable();

            JOptionPane.showMessageDialog(
                this,
                "Payment marked as paid."
            );

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void markAsPending() {

        Payment payment = getSelectedPayment();

        if (payment == null) {
            showError("Please select a payment record.");
            return;
        }

        try {

            paymentService.markPaymentAsPending(
                payment.getPaymentId()
            );

            refreshTable();

            JOptionPane.showMessageDialog(
                this,
                "Payment marked as pending."
            );

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private Payment getSelectedPayment() {

        int selectedRow = paymentTable.getSelectedRow();

        if (selectedRow == -1) {
            return null;
        }

        String paymentId = tableModel.getValueAt(
            selectedRow, 0
        ).toString();

        return paymentService.findPaymentById(paymentId);
    }

    public void refreshTable() {

        if (tableModel == null) {
            return;
        }

        tableModel.setRowCount(0);

        for (Payment payment : paymentService.getAllPayments()) {

            tableModel.addRow(new Object[] {
                payment.getPaymentId(),
                payment.getBookingId(),
                String.format("₱%,.2f", payment.getAmount()),
                payment.getPaymentMethod(),
                payment.getPaymentDate(),
                payment.getPaymentStatus()
            });
        }

        totalRevenueLabel.setText(
            String.format(
                "₱%,.2f",
                paymentService.getTotalPaidAmount()
            )
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