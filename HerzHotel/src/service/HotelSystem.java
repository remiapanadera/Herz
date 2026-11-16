package service;

import model.Booking;
import model.Guest;
import model.Payment;
import model.Room;
import storage.HotelDataManager;

import java.io.IOException;

public class HotelSystem {

    private final GuestService guestService;
    private final RoomService roomService;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final ReportService reportService;

    private final HotelDataManager dataManager;

    public HotelSystem() {

        guestService = new GuestService();
        roomService = new RoomService();

        bookingService = new BookingService(
            guestService,
            roomService
        );

        paymentService = new PaymentService(
            bookingService,
            roomService
        );

        reportService = new ReportService(
            guestService,
            roomService,
            bookingService,
            paymentService
        );

        dataManager = new HotelDataManager();
    }

    public void loadData() throws IOException {

        dataManager.loadAllData();

        // Restore guests first.
        for (Guest guest : dataManager.getGuestStorage().getGuests()) {

            try {
                guestService.addGuest(guest);
            } catch (Exception e) {
                throw new IOException(
                    "Could not restore guest: " + guest.getGuestId(),
                    e
                );
            }
        }

        // Restore rooms before bookings.
        for (Room room : dataManager.getRoomStorage().getRooms()) {

            try {
                roomService.addRoom(room);
            } catch (Exception e) {
                throw new IOException(
                    "Could not restore room: " + room.getRoomNumber(),
                    e
                );
            }
        }

        // First run: there are no saved rooms yet, so start with the default 30 rooms.
        if (roomService.getRoomCount() == 0) {
            addDefaultRooms();
        }

        // Restore bookings without treating them as new reservations.
        for (Booking booking :
                dataManager.getBookingStorage().getBookings()) {

            try {
                bookingService.restoreBooking(booking);
            } catch (Exception e) {
                throw new IOException(
                    "Could not restore booking: " + booking.getBookingId(),
                    e
                );
            }
        }

        // Recalculate room statuses from the restored bookings.
        bookingService.refreshRoomStatuses();

        // Restore payments after bookings.
        for (Payment payment :
                dataManager.getPaymentStorage().getPayments()) {

            try {
                paymentService.addPayment(payment);
            } catch (Exception e) {
                throw new IOException(
                    "Could not restore payment: " + payment.getPaymentId(),
                    e
                );
            }
        }
    }

    // Creates the hotel's 30 starting rooms across 4 floors. All of them start as "Available"
    // because there are no guests or bookings yet.
    private void addDefaultRooms() throws IOException {

        int[] roomsPerFloor = {8, 8, 8, 6};                          // floors 1-4 = 30 rooms
        String[] roomTypes  = {"Standard", "Standard", "Deluxe", "Suite"};
        double[] prices     = {1500, 1500, 2500, 4000};              // price per night for each floor

        for (int floor = 1; floor <= 4; floor++) {
            for (int number = 1; number <= roomsPerFloor[floor - 1]; number++) {

                // floor 1, number 5 -> "105"; floor 4, number 2 -> "402"
                String roomNumber = String.format("%d%02d", floor, number);

                try {
                    roomService.addRoom(new Room(
                        roomNumber,
                        roomTypes[floor - 1],
                        prices[floor - 1],
                        "Available"
                    ));
                } catch (Exception e) {
                    throw new IOException(
                        "Could not create default room: " + roomNumber,
                        e
                    );
                }
            }
        }
    }

    public void saveData() throws IOException {

        dataManager.getGuestStorage().setGuests(
            guestService.getAllGuests()
        );

        dataManager.getRoomStorage().setRooms(
            roomService.getAllRooms()
        );

        dataManager.getBookingStorage().setBookings(
            bookingService.getAllBookings()
        );

        dataManager.getPaymentStorage().setPayments(
            paymentService.getAllPayments()
        );

        dataManager.saveAllData();
    }

    public GuestService getGuestService() {
        return guestService;
    }

    public RoomService getRoomService() {
        return roomService;
    }

    public BookingService getBookingService() {
        return bookingService;
    }

    public PaymentService getPaymentService() {
        return paymentService;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public HotelDataManager getDataManager() {
        return dataManager;
    }
}