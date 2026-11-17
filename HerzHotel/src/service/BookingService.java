package service;

import exception.DuplicateRecordException;
import exception.InvalidBookingException;
import model.Booking;
import model.Room;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BookingService {

    private final ArrayList<Booking> bookings;
    private final GuestService guestService;
    private final RoomService roomService;

    public BookingService(
            GuestService guestService,
            RoomService roomService) {

        this.bookings = new ArrayList<>();
        this.guestService = guestService;
        this.roomService = roomService;
    }

    public void addBooking(Booking booking)
            throws DuplicateRecordException, InvalidBookingException {

        validateBooking(booking, true);

        for (Booking existing : bookings) {

            if (existing.getBookingId().equalsIgnoreCase(
                    booking.getBookingId())) {

                throw new DuplicateRecordException(
                    "Booking ID already exists."
                );
            }
        }

        bookings.add(booking);

        Room room = roomService.findRoomByNumber(
            booking.getRoomNumber()
        );

        if (room != null) {
            room.setStatus("Reserved");
        }
    }

    /*
     * Restores a booking loaded from file.
     * It does not require the room to be Available because
     * the saved room status may already be Reserved or Occupied.
     */
    public void restoreBooking(Booking booking)
            throws DuplicateRecordException, InvalidBookingException {

        validateBooking(booking, false);

        for (Booking existing : bookings) {

            if (existing.getBookingId().equalsIgnoreCase(
                    booking.getBookingId())) {

                throw new DuplicateRecordException(
                    "Duplicate booking found in saved data."
                );
            }
        }

        bookings.add(booking);
    }

    private void validateBooking(
            Booking booking,
            boolean requireAvailableRoom)
            throws InvalidBookingException {

        if (booking == null) {
            throw new InvalidBookingException(
                "Booking cannot be empty."
            );
        }

        if (guestService.findGuestById(booking.getGuestId()) == null) {
            throw new InvalidBookingException(
                "Guest does not exist."
            );
        }

        Room room = roomService.findRoomByNumber(
            booking.getRoomNumber()
        );

        if (room == null) {
            throw new InvalidBookingException(
                "Room does not exist."
            );
        }

        if (requireAvailableRoom
                && !"Available".equalsIgnoreCase(room.getStatus())) {

            throw new InvalidBookingException(
                "Selected room is not available."
            );
        }

        if (!booking.getCheckOutDate().isAfter(
                booking.getCheckInDate())) {

            throw new InvalidBookingException(
                "Check-out date must be after check-in date."
            );
        }

        for (Booking existing : bookings) {

            if (!existing.getRoomNumber().equalsIgnoreCase(
                    booking.getRoomNumber())) {
                continue;
            }

            if ("Cancelled".equalsIgnoreCase(
                    existing.getBookingStatus())
                    || "Checked Out".equalsIgnoreCase(
                    existing.getBookingStatus())) {
                continue;
            }

            boolean overlaps =
                booking.getCheckInDate().isBefore(
                    existing.getCheckOutDate()
                )
                &&
                booking.getCheckOutDate().isAfter(
                    existing.getCheckInDate()
                );

            if (overlaps) {
                throw new InvalidBookingException(
                    "This room already has a booking during those dates."
                );
            }
        }
    }

    public void cancelBooking(String bookingId)
            throws InvalidBookingException {

        Booking booking = findBookingById(bookingId);

        if (booking == null) {
            throw new InvalidBookingException(
                "Booking not found."
            );
        }

        if ("Checked In".equalsIgnoreCase(
                booking.getBookingStatus())) {

            throw new InvalidBookingException(
                "A checked-in booking cannot be cancelled."
            );
        }

        if ("Cancelled".equalsIgnoreCase(
                booking.getBookingStatus())
                || "Checked Out".equalsIgnoreCase(
                booking.getBookingStatus())) {

            throw new InvalidBookingException(
                "This booking can no longer be cancelled."
            );
        }

        booking.cancelBooking();

        updateRoomStatus(booking.getRoomNumber());
    }

    /*
     * A reserved booking can be checked in on its scheduled check-in date,
     * from the standard check-in time (2:00 PM) onward.
     */
    public boolean isEligibleForCheckIn(Booking booking) {

        if (booking == null
                || !"Reserved".equalsIgnoreCase(booking.getBookingStatus())) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        return now.toLocalDate().equals(booking.getCheckInDate())
                && !now.toLocalTime().isBefore(Booking.STANDARD_CHECK_IN_TIME);
    }

    public void checkInBooking(String bookingId)
            throws InvalidBookingException {

        Booking booking = findBookingById(bookingId);

        if (booking == null) {
            throw new InvalidBookingException(
                "Booking not found."
            );
        }

        if (!"Reserved".equalsIgnoreCase(
                booking.getBookingStatus())) {

            throw new InvalidBookingException(
                "This booking is not eligible for check-in."
            );
        }

        if (!LocalDate.now().equals(booking.getCheckInDate())) {
            throw new InvalidBookingException(
                "Check-in is only allowed on the scheduled check-in date."
            );
        }

        if (!isEligibleForCheckIn(booking)) {
            throw new InvalidBookingException(
                "Standard check-in time is 2:00 PM. "
                + "This guest can be checked in from then."
            );
        }

        markCheckedIn(booking);
    }

    /*
     * Walk-in guests are checked in automatically when the booking is
     * confirmed, so the 2:00 PM standard time does not block them.
     * The actual arrival time is recorded.
     */
    public void checkInWalkIn(String bookingId)
            throws InvalidBookingException {

        Booking booking = findBookingById(bookingId);

        if (booking == null) {
            throw new InvalidBookingException(
                "Booking not found."
            );
        }

        if (!booking.isWalkIn()
                || !"Reserved".equalsIgnoreCase(booking.getBookingStatus())) {

            throw new InvalidBookingException(
                "Only a new walk-in booking can be checked in automatically."
            );
        }

        markCheckedIn(booking);
    }

    private void markCheckedIn(Booking booking) {

        booking.checkIn(LocalDateTime.now());

        Room room = roomService.findRoomByNumber(
            booking.getRoomNumber()
        );

        if (room != null) {
            room.setStatus("Occupied");
        }
    }

    public void checkOutBooking(String bookingId)
            throws InvalidBookingException {

        Booking booking = findBookingById(bookingId);

        if (booking == null) {
            throw new InvalidBookingException(
                "Booking not found."
            );
        }

        if (!LocalDate.now().equals(booking.getCheckOutDate())) {
            throw new InvalidBookingException(
                "Check-out is only allowed on the scheduled check-out date."
            );
        }

        if (!"Checked In".equalsIgnoreCase(
                booking.getBookingStatus())) {

            throw new InvalidBookingException(
                "Only checked-in bookings can be checked out."
            );
        }

        booking.checkOut(LocalDateTime.now());

        updateRoomStatus(booking.getRoomNumber());
    }

    private void updateRoomStatus(String roomNumber) {

        Room room = roomService.findRoomByNumber(roomNumber);

        if (room == null) {
            return;
        }

        boolean occupied = false;
        boolean reserved = false;

        LocalDate today = LocalDate.now();

        for (Booking booking : bookings) {

            if (!booking.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                continue;
            }

            if ("Checked In".equalsIgnoreCase(
                    booking.getBookingStatus())) {

                occupied = true;
                break;
            }

            if ("Reserved".equalsIgnoreCase(
                    booking.getBookingStatus())
                    && !booking.getCheckOutDate().isBefore(today)) {

                reserved = true;
            }
        }

        if (occupied) {
            room.setStatus("Occupied");
        } else if (reserved) {
            room.setStatus("Reserved");
        } else {
            room.setStatus("Available");
        }
    }

    public Booking findBookingById(String bookingId) {

        if (bookingId == null) {
            return null;
        }

        for (Booking booking : bookings) {

            if (booking.getBookingId().equalsIgnoreCase(
                    bookingId.trim())) {

                return booking;
            }
        }

        return null;
    }

    public List<Booking> searchBookings(String keyword) {

        ArrayList<Booking> results = new ArrayList<>();

        if (keyword == null) {
            keyword = "";
        }

        String search = keyword.trim().toLowerCase();

        for (Booking booking : bookings) {

            if (booking.getBookingId().toLowerCase().contains(search)
                    || booking.getGuestId().toLowerCase().contains(search)
                    || booking.getRoomNumber().toLowerCase().contains(search)
                    || booking.getBookingStatus().toLowerCase().contains(search)) {

                results.add(booking);
            }
        }

        return results;
    }

    public List<Booking> getAllBookings() {
        return new ArrayList<>(bookings);
    }

    public int getTotalBookings() {
        return bookings.size();
    }

    public void refreshRoomStatuses() {

        for (Room room : roomService.getAllRooms()) {
            updateRoomStatus(room.getRoomNumber());
        }
    }
}