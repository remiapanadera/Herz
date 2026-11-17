package gui;

import model.Guest;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;

import static gui.UiKit.*;

/**
 * "New guest" form shown from the Bookings page.
 * Guests are only created through a booking, so this is the one place a guest
 * profile is first entered (first + last name, email, contact number, address).
 */
class GuestFormDialog extends JDialog {

    private final String guestId;

    private final JTextField firstField = formField("Enter first name");
    private final JTextField lastField = formField("Enter last name");
    private final JTextField emailField = formField("guest@example.com");
    private final JTextField phoneField = formField("Include country code");
    private final JTextField addressField = formField("Enter Address");
    private final JLabel errorLabel = new JLabel(" ");

    private Guest result;

    GuestFormDialog(Component parent, String guestId) {
        super(SwingUtilities.getWindowAncestor(parent), "New guest",
              ModalityType.APPLICATION_MODAL);

        this.guestId = guestId;

        setUndecorated(true);

        ((AbstractDocument) phoneField.getDocument()).setDocumentFilter(new PhoneFilter());

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER),
            BorderFactory.createEmptyBorder(22, 22, 20, 22)
        ));

        JLabel title = new JLabel("New guest");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(INK);

        JLabel subtitle = new JLabel("Enter the guest's details for this booking.");
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
        root.add(Box.createVerticalStrut(10));

        JButton cancel = button("Cancel", ButtonKind.OUTLINE);
        cancel.setPreferredSize(new Dimension(110, 40));
        cancel.addActionListener(e -> dispose());

        JButton save = button("Add guest", ButtonKind.PRIMARY);
        save.setPreferredSize(new Dimension(130, 40));
        save.addActionListener(e -> submit());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(save);
        root.add(left(buttons));

        getRootPane().setDefaultButton(save);
        setContentPane(root);
        setMinimumSize(new Dimension(520, 0));
        pack();
        setLocationRelativeTo(parent);
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

    /** The guest that was entered, or null if the dialog was cancelled. */
    Guest getResult() {
        return result;
    }

    // =====================================================================
    //  Philippine mobile number rules: +63 9xx xxx xxxx (12 digits total)
    //  (same rules as the guest edit form on the Guests page)
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
}
