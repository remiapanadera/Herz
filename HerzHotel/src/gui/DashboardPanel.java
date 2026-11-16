package gui;

import service.ReportService;
import util.HerzTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DashboardPanel extends JPanel {

    private final ReportService reportService;

    private JLabel guestsValue;
    private JLabel roomsValue;
    private JLabel availableValue;
    private JLabel occupiedValue;
    private JLabel reservedValue;
    private JLabel bookingsValue;
    private JLabel revenueValue;
    private JLabel occupancyValue;

    private JProgressBar occupancyProgress;

    public DashboardPanel(ReportService reportService) {

        if (reportService == null) {
            throw new IllegalArgumentException(
                "Report service is required."
            );
        }

        this.reportService = reportService;

        setLayout(new BorderLayout(20, 20));
        setBackground(HerzTheme.CREAM);
        setBorder(new EmptyBorder(30, 35, 30, 35));

        add(createHeader(), BorderLayout.NORTH);
        add(createDashboardContent(), BorderLayout.CENTER);

        refreshDashboard();
    }

    private JPanel createHeader() {

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Hotel Dashboard");
        title.setFont(HerzTheme.TITLE_FONT);
        title.setForeground(HerzTheme.DARK_TEXT);

        JLabel subtitle = new JLabel(
            "Welcome to Herz Hotel. Here's your hotel overview."
        );
        subtitle.setFont(HerzTheme.BODY_FONT);
        subtitle.setForeground(HerzTheme.SECONDARY_TEXT);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(6));
        titlePanel.add(subtitle);

        JButton refreshButton = HerzTheme.createSecondaryButton("Refresh");
        refreshButton.addActionListener(e -> refreshDashboard());

        header.add(titlePanel, BorderLayout.WEST);
        header.add(refreshButton, BorderLayout.EAST);

        return header;
    }

    private JPanel createDashboardContent() {

        JPanel content = new JPanel(new BorderLayout(18, 18));
        content.setOpaque(false);

        JPanel statistics = new JPanel(new GridLayout(2, 3, 15, 15));
        statistics.setOpaque(false);

        guestsValue = new JLabel("0");
        roomsValue = new JLabel("0");
        availableValue = new JLabel("0");
        occupiedValue = new JLabel("0");
        reservedValue = new JLabel("0");
        bookingsValue = new JLabel("0");

        statistics.add(createStatCard(
            "Total Guests", guestsValue, "Registered guests"
        ));

        statistics.add(createStatCard(
            "Total Rooms", roomsValue, "Hotel room inventory"
        ));

        statistics.add(createStatCard(
            "Available Rooms", availableValue, "Ready for reservations"
        ));

        statistics.add(createStatCard(
            "Occupied Rooms", occupiedValue, "Currently occupied"
        ));

        statistics.add(createStatCard(
            "Reserved Rooms", reservedValue, "Reserved for guests"
        ));

        statistics.add(createStatCard(
            "Total Bookings", bookingsValue, "Recorded reservations"
        ));

        JPanel bottom = new JPanel(new GridLayout(1, 2, 15, 15));
        bottom.setOpaque(false);

        bottom.add(createRevenueCard());
        bottom.add(createOccupancyCard());

        content.add(statistics, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);

        return content;
    }

    private JPanel createStatCard(
            String title,
            JLabel valueLabel,
            String description) {

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(HerzTheme.WHITE);

        card.setBorder(BorderFactory.createCompoundBorder(
            HerzTheme.cardBorder(),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(HerzTheme.BODY_FONT);
        titleLabel.setForeground(HerzTheme.SECONDARY_TEXT);

        valueLabel.setFont(new Font(
            "SansSerif",
            Font.BOLD,
            32
        ));

        valueLabel.setForeground(HerzTheme.BURGUNDY);

        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setFont(HerzTheme.SMALL_FONT);
        descriptionLabel.setForeground(HerzTheme.SECONDARY_TEXT);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(12));
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(descriptionLabel);

        return card;
    }

    private JPanel createRevenueCard() {

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(HerzTheme.WHITE);

        card.setBorder(BorderFactory.createCompoundBorder(
            HerzTheme.cardBorder(),
            new EmptyBorder(20, 22, 20, 22)
        ));

        JLabel title = new JLabel("Recorded Payment Revenue");
        title.setFont(HerzTheme.HEADING_FONT);
        title.setForeground(HerzTheme.DARK_TEXT);

        revenueValue = new JLabel("₱0.00");
        revenueValue.setFont(new Font(
            "SansSerif",
            Font.BOLD,
            28
        ));
        revenueValue.setForeground(HerzTheme.BURGUNDY);

        JLabel description = new JLabel(
            "Total payments marked as paid"
        );
        description.setFont(HerzTheme.SMALL_FONT);
        description.setForeground(HerzTheme.SECONDARY_TEXT);

        card.add(title);
        card.add(Box.createVerticalStrut(15));
        card.add(revenueValue);
        card.add(Box.createVerticalStrut(8));
        card.add(description);

        return card;
    }

    private JPanel createOccupancyCard() {

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(HerzTheme.WHITE);

        card.setBorder(BorderFactory.createCompoundBorder(
            HerzTheme.cardBorder(),
            new EmptyBorder(20, 22, 20, 22)
        ));

        JLabel title = new JLabel("Room Occupancy Rate");
        title.setFont(HerzTheme.HEADING_FONT);
        title.setForeground(HerzTheme.DARK_TEXT);

        occupancyValue = new JLabel("0.0%");
        occupancyValue.setFont(new Font(
            "SansSerif",
            Font.BOLD,
            28
        ));
        occupancyValue.setForeground(HerzTheme.BURGUNDY);

        occupancyProgress = new JProgressBar(0, 100);
        occupancyProgress.setStringPainted(true);
        occupancyProgress.setForeground(HerzTheme.BURGUNDY);
        occupancyProgress.setBackground(HerzTheme.BLUSH);
        occupancyProgress.setPreferredSize(new Dimension(300, 18));

        JLabel description = new JLabel(
            "Percentage of rooms currently occupied"
        );
        description.setFont(HerzTheme.SMALL_FONT);
        description.setForeground(HerzTheme.SECONDARY_TEXT);

        card.add(title);
        card.add(Box.createVerticalStrut(15));
        card.add(occupancyValue);
        card.add(Box.createVerticalStrut(12));
        card.add(occupancyProgress);
        card.add(Box.createVerticalStrut(10));
        card.add(description);

        return card;
    }

    public void refreshDashboard() {

        guestsValue.setText(
            String.valueOf(reportService.getTotalGuests())
        );

        roomsValue.setText(
            String.valueOf(reportService.getTotalRooms())
        );

        availableValue.setText(
            String.valueOf(reportService.getAvailableRooms())
        );

        occupiedValue.setText(
            String.valueOf(reportService.getOccupiedRooms())
        );

        reservedValue.setText(
            String.valueOf(reportService.getReservedRooms())
        );

        bookingsValue.setText(
            String.valueOf(reportService.getTotalBookings())
        );

        revenueValue.setText(
            String.format(
                "₱%,.2f",
                reportService.getTotalRevenue()
            )
        );

        double rate = reportService.getOccupancyRate();

        occupancyValue.setText(
            String.format("%.1f%%", rate)
        );

        occupancyProgress.setValue(
            (int) Math.round(rate)
        );
    }
}