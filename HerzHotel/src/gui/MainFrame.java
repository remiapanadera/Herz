package gui;

import service.HotelSystem;
import util.HerzTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class MainFrame extends JFrame {

    private final HotelSystem hotelSystem;

    private JPanel contentPanel;
    private CardLayout cardLayout;

    private JButton dashboardButton;
    private JButton guestsButton;
    private JButton roomsButton;
    private JButton bookingsButton;
    private JButton paymentsButton;

    private DashboardPanel dashboardPanel;
    private GuestPanel guestPanel;
    private RoomPanel roomPanel;
    private BookingPanel bookingPanel;
    private PaymentPanel paymentPanel;

    public MainFrame() {

        hotelSystem = new HotelSystem();

        try {
            hotelSystem.loadData();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                null,
                "Could not load saved hotel data:\n" + e.getMessage(),
                "Herz Hotel Data Error",
                JOptionPane.ERROR_MESSAGE
            );
        }

        initializePanels();
        initializeFrame();
    }

    private void initializeFrame() {

        setTitle("Herz Hotel | Hotel Management System");
        setSize(
            HerzTheme.WINDOW_WIDTH,
            HerzTheme.WINDOW_HEIGHT
        );

        setMinimumSize(new Dimension(1100, 700));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        add(createSidebar(), BorderLayout.WEST);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(HerzTheme.CREAM);

        contentPanel.add(dashboardPanel, "Dashboard");
        contentPanel.add(guestPanel, "Guests");
        contentPanel.add(roomPanel, "Rooms");
        contentPanel.add(bookingPanel, "Bookings");
        contentPanel.add(paymentPanel, "Payments");

        add(contentPanel, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                saveAndExit();
            }
        });

        showPage("Dashboard");
    }

    private void initializePanels() {

        dashboardPanel = new DashboardPanel(
            hotelSystem.getReportService()
        );

        guestPanel = new GuestPanel(
            hotelSystem.getGuestService()
        );

        roomPanel = new RoomPanel(
            hotelSystem.getRoomService()
        );

        bookingPanel = new BookingPanel(
            hotelSystem.getBookingService(),
            hotelSystem.getGuestService(),
            hotelSystem.getRoomService()
        );

        paymentPanel = new PaymentPanel(
            hotelSystem.getPaymentService(),
            hotelSystem.getBookingService()
        );
    }

    private JPanel createSidebar() {

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(HerzTheme.BURGUNDY);

        sidebar.setPreferredSize(new Dimension(
            HerzTheme.SIDEBAR_WIDTH,
            HerzTheme.WINDOW_HEIGHT
        ));

        sidebar.setBorder(BorderFactory.createEmptyBorder(
            25, 18, 25, 18
        ));

        JLabel logo = new JLabel("HERZ");
        logo.setFont(new Font("Serif", Font.BOLD, 32));
        logo.setForeground(HerzTheme.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hotelName = new JLabel("HOTEL");
        hotelName.setFont(HerzTheme.BUTTON_FONT);
        hotelName.setForeground(HerzTheme.BLUSH);
        hotelName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel location = new JLabel("LEGAZPI");
        location.setFont(HerzTheme.SMALL_FONT);
        location.setForeground(HerzTheme.BLUSH);
        location.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(3));
        sidebar.add(hotelName);
        sidebar.add(Box.createVerticalStrut(3));
        sidebar.add(location);

        sidebar.add(Box.createVerticalStrut(45));

        dashboardButton = createNavigationButton(
            "Dashboard"
        );

        guestsButton = createNavigationButton(
            "Guests"
        );

        roomsButton = createNavigationButton(
            "Rooms"
        );

        bookingsButton = createNavigationButton(
            "Bookings"
        );

        paymentsButton = createNavigationButton(
            "Payments"
        );

        addNavigation(sidebar, dashboardButton, "Dashboard");
        addNavigation(sidebar, guestsButton, "Guests");
        addNavigation(sidebar, roomsButton, "Rooms");
        addNavigation(sidebar, bookingsButton, "Bookings");
        addNavigation(sidebar, paymentsButton, "Payments");

        sidebar.add(Box.createVerticalGlue());

        JButton logoutButton = createNavigationButton("Logout");
        logoutButton.addActionListener(e -> logout());

        sidebar.add(logoutButton);

        return sidebar;
    }

    private JButton createNavigationButton(String text) {

        JButton button = new JButton(text);

        button.setFont(HerzTheme.BUTTON_FONT);
        button.setForeground(HerzTheme.WHITE);
        button.setBackground(HerzTheme.BURGUNDY);

        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);

        button.setMaximumSize(new Dimension(
            Integer.MAX_VALUE,
            48
        ));

        button.setPreferredSize(new Dimension(205, 48));

        button.setBorder(BorderFactory.createEmptyBorder(
            10, 15, 10, 10
        ));

        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    private void addNavigation(
            JPanel sidebar,
            JButton button,
            String pageName) {

        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        button.addActionListener(e -> showPage(pageName));

        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(8));
    }

    private void showPage(String pageName) {

        cardLayout.show(contentPanel, pageName);

        dashboardButton.setBackground(HerzTheme.BURGUNDY);
        guestsButton.setBackground(HerzTheme.BURGUNDY);
        roomsButton.setBackground(HerzTheme.BURGUNDY);
        bookingsButton.setBackground(HerzTheme.BURGUNDY);
        paymentsButton.setBackground(HerzTheme.BURGUNDY);

        switch (pageName) {

            case "Dashboard":
                dashboardButton.setBackground(
                    HerzTheme.DARK_BURGUNDY
                );
                dashboardPanel.refreshDashboard();
                break;

            case "Guests":
                guestsButton.setBackground(
                    HerzTheme.DARK_BURGUNDY
                );
                guestPanel.refreshTable();
                break;

            case "Rooms":
                roomsButton.setBackground(
                    HerzTheme.DARK_BURGUNDY
                );
                roomPanel.refreshTable();
                break;

            case "Bookings":
                bookingsButton.setBackground(
                    HerzTheme.DARK_BURGUNDY
                );
                bookingPanel.refreshTable();
                break;

            case "Payments":
                paymentsButton.setBackground(
                    HerzTheme.DARK_BURGUNDY
                );
                paymentPanel.refreshTable();
                break;

            default:
                break;
        }
    }

    private void saveAndExit() {

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Save all hotel records before exiting?",
            "Exit Herz Hotel",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (choice == JOptionPane.CANCEL_OPTION
                || choice == JOptionPane.CLOSED_OPTION) {
            return;
        }

        if (choice == JOptionPane.YES_OPTION) {

            try {

                hotelSystem.saveData();

                JOptionPane.showMessageDialog(
                    this,
                    "Hotel records saved successfully."
                );

            } catch (IOException e) {

                JOptionPane.showMessageDialog(
                    this,
                    "Could not save hotel records:\n" + e.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE
                );

                return;
            }
        }

        dispose();
        System.exit(0);
    }

    private void logout() {

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Save your changes and log out?",
            "Logout",
            JOptionPane.YES_NO_CANCEL_OPTION
        );

        if (choice == JOptionPane.CANCEL_OPTION
                || choice == JOptionPane.CLOSED_OPTION) {
            return;
        }

        if (choice == JOptionPane.YES_OPTION) {

            try {

                hotelSystem.saveData();

            } catch (IOException e) {

                JOptionPane.showMessageDialog(
                    this,
                    "Could not save data:\n" + e.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE
                );

                return;
            }
        }

        LoginFrame loginFrame = new LoginFrame();
        loginFrame.setVisible(true);

        dispose();
    }
}