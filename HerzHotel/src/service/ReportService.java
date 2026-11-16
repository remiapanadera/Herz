package service;

public class ReportService {

    private GuestService guestService;
    private RoomService roomService;
    private BookingService bookingService;
    private PaymentService paymentService;

    public ReportService(
            GuestService guestService,
            RoomService roomService,
            BookingService bookingService,
            PaymentService paymentService) {

        this.guestService = guestService;
        this.roomService = roomService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
    }

    public int getTotalGuests() {
        return guestService.getAllGuests().size();
    }

    public int getTotalRooms() {
        return roomService.getAllRooms().size();
    }

    public int getFloorCount() {
        return roomService.getFloorCount();
    }

    public int getAvailableRooms() {
        return roomService.getAvailableRoomCount();
    }

    public int getOccupiedRooms() {
        return roomService.getOccupiedRoomCount();
    }

    public int getReservedRooms() {
        return roomService.getReservedRoomCount();
    }

    public int getTotalBookings() {
        return bookingService.getAllBookings().size();
    }

    public int getTotalPayments() {
        return paymentService.getAllPayments().size();
    }

    public double getTotalRevenue() {
        return paymentService.getTotalPaidAmount();
    }

    public double getOccupancyRate() {

        int totalRooms = getTotalRooms();

        if (totalRooms == 0) {
            return 0;
        }

        return (getOccupiedRooms() * 100.0) / totalRooms;
    }
}