
package gui;

import util.HerzTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox keepSignedInCheckBox;

    public LoginFrame() {

        setTitle("Herz Hotel | Admin Login");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new GridLayout(1, 2));
        mainPanel.setBackground(HerzTheme.CREAM);

        mainPanel.add(createBrandPanel());
        mainPanel.add(createLoginPanel());

        add(mainPanel);
    }

    private JPanel createBrandPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(HerzTheme.BURGUNDY);
        panel.setBorder(new EmptyBorder(90, 55, 70, 55));

        JLabel logo = new JLabel("HERZ");
        logo.setFont(new Font("Serif", Font.BOLD, 58));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hotel = new JLabel("HOTEL");
        hotel.setFont(new Font("SansSerif", Font.PLAIN, 19));
        hotel.setForeground(Color.WHITE);
        hotel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel welcome = new JLabel(
            "<html><div style='text-align:center;'>"
            + "Welcome to<br>Herz Hotel"
            + "</div></html>"
        );
        welcome.setFont(new Font("Serif", Font.BOLD, 34));
        welcome.setForeground(Color.WHITE);
        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel description = new JLabel(
            "<html><div style='text-align:center;'>"
            + "A comfortable stay, managed with care."
            + "</div></html>"
        );
        description.setFont(new Font("SansSerif", Font.PLAIN, 15));
        description.setForeground(new Color(235, 220, 220));
        description.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(Box.createVerticalGlue());
        panel.add(logo);
        panel.add(Box.createVerticalStrut(5));
        panel.add(hotel);
        panel.add(Box.createVerticalStrut(65));
        panel.add(welcome);
        panel.add(Box.createVerticalStrut(20));
        panel.add(description);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createLoginPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(HerzTheme.CREAM);
        panel.setBorder(new EmptyBorder(90, 75, 70, 75));

        JLabel heading = new JLabel("Admin Access");
        heading.setFont(new Font("Serif", Font.BOLD, 34));
        heading.setForeground(HerzTheme.DARK_TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Sign in to manage hotel operations.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(HerzTheme.SECONDARY_TEXT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel usernameLabel = new JLabel("Username");
        styleLabel(usernameLabel);

        usernameField = HerzTheme.createTextField(20);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel passwordLabel = new JLabel("Password");
        styleLabel(passwordLabel);

        passwordField = HerzTheme.createPasswordField(20);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        keepSignedInCheckBox = new JCheckBox("Keep me signed in");
        keepSignedInCheckBox.setBackground(HerzTheme.CREAM);
        keepSignedInCheckBox.setForeground(HerzTheme.SECONDARY_TEXT);
        keepSignedInCheckBox.setFocusPainted(false);
        keepSignedInCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton signInButton = HerzTheme.createPrimaryButton("Sign In");
        signInButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        signInButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        signInButton.addActionListener(e -> handleLogin());

        JLabel forgotPassword = new JLabel("Forgot your password?");
        forgotPassword.setFont(new Font("SansSerif", Font.PLAIN, 13));
        forgotPassword.setForeground(HerzTheme.BURGUNDY);
        forgotPassword.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel footer = new JLabel("HERZ HOTEL  |  LEGAZPI");
        footer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        footer.setForeground(HerzTheme.SECONDARY_TEXT);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(Box.createVerticalGlue());
        panel.add(heading);
        panel.add(Box.createVerticalStrut(10));
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(45));

        panel.add(usernameLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(usernameField);
        panel.add(Box.createVerticalStrut(22));

        panel.add(passwordLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(passwordField);
        panel.add(Box.createVerticalStrut(15));

        panel.add(keepSignedInCheckBox);
        panel.add(Box.createVerticalStrut(30));
        panel.add(signInButton);
        panel.add(Box.createVerticalStrut(20));
        panel.add(forgotPassword);
        panel.add(Box.createVerticalGlue());
        panel.add(footer);

        return panel;
    }

    private void styleLabel(JLabel label) {
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(HerzTheme.DARK_TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void handleLogin() {

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        // Temporary demonstration credentials.
        if (username.equals("admin") && password.equals("admin123")) {

            JOptionPane.showMessageDialog(
                this,
                "Welcome to Herz Hotel!",
                "Login Successful",
                JOptionPane.INFORMATION_MESSAGE
            );

            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);

            dispose();

        } else {

            JOptionPane.showMessageDialog(
                this,
                "Invalid username or password.",
                "Login Failed",
                JOptionPane.ERROR_MESSAGE
            );

            passwordField.setText("");
            passwordField.requestFocus();
        }
    }
}
