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