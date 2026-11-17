package service;

import exception.DuplicateRecordException;
import exception.InvalidBookingException;
import model.Booking;
import model.Room;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages hotel reservations, room availability transitions, and
 * explicit check-in / check-out state changes triggered from the UI.
 */
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

    /**
     * Validates and persists a new booking, marking the assigned room as Reserved.
     * Both Advance and Walk-in bookings start in "Reserved" status so that
     * check-in only happens when explicitly triggered on the Guest Management screen.
     */
    public void addBooking(Booking booking)
            throws DuplicateRecordException, InvalidBookingException {

        validateBooking(booking, true);

        for (Booking existing : bookings) {
            if (existing.getBookingId().equalsIgnoreCase(booking.getBookingId())) {
                throw new DuplicateRecordException("Booking ID already exists.");
            }
        }

        bookings.add(booking);

        Room room = roomService.findRoomByNumber(booking.getRoomNumber());
        if (room != null) {
            room.setStatus("Reserved");
        }
    }

    /**
     * Restores a booking loaded from persistent storage without enforcing
     * "Available" room status (since saved rooms may already be Reserved/Occupied).
     */
    public void restoreBooking(Booking booking)
            throws DuplicateRecordException, InvalidBookingException {

        validateBooking(booking, false);

        for (Booking existing : bookings) {
            if (existing.getBookingId().equalsIgnoreCase(booking.getBookingId())) {
                throw new DuplicateRecordException("Duplicate booking found in saved data.");
            }
        }

        bookings.add(booking);
    }

    /**
     * Ensures guest existence, room availability, valid date range,
     * and absence of overlapping active bookings for the same room.
     */
    private void validateBooking(
            Booking booking,
            boolean requireAvailableRoom)
            throws InvalidBookingException {

        if (booking == null) {
            throw new InvalidBookingException("Booking cannot be empty.");
        }

        if (guestService.findGuestById(booking.getGuestId()) == null) {
            throw new InvalidBookingException("Guest does not exist.");
        }

        Room room = roomService.findRoomByNumber(booking.getRoomNumber());

        if (room == null) {
            throw new InvalidBookingException("Room does not exist.");
        }

        if (requireAvailableRoom
                && !"Available".equalsIgnoreCase(room.getStatus())) {
            throw new InvalidBookingException("Selected room is not available.");
        }

        if (!booking.getCheckOutDate().isAfter(booking.getCheckInDate())) {
            throw new InvalidBookingException("Check-out date must be after check-in date.");
        }

        for (Booking existing : bookings) {
            if (!existing.getRoomNumber().equalsIgnoreCase(booking.getRoomNumber())) {
                continue;
            }

            // Cancelled or completed stays do not block new bookings
            if ("Cancelled".equalsIgnoreCase(existing.getBookingStatus())
                    || "Checked Out".equalsIgnoreCase(existing.getBookingStatus())) {
                continue;
            }

            boolean overlaps =
                booking.getCheckInDate().isBefore(existing.getCheckOutDate())
                && booking.getCheckOutDate().isAfter(existing.getCheckInDate());

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
            throw new InvalidBookingException("Booking not found.");
        }

        if ("Checked In".equalsIgnoreCase(booking.getBookingStatus())) {
            throw new InvalidBookingException("A checked-in booking cannot be cancelled.");
        }

        if ("Cancelled".equalsIgnoreCase(booking.getBookingStatus())
                || "Checked Out".equalsIgnoreCase(booking.getBookingStatus())) {
            throw new InvalidBookingException("This booking can no longer be cancelled.");
        }

        booking.cancelBooking();
        updateRoomStatus(booking.getRoomNumber());
    }

    /**
     * Determines whether a Reserved booking can be checked in right now.
     * - Walk-in bookings can be checked in immediately on their check-in date.
     * - Advance bookings can be checked in on their scheduled check-in date from 2:00 PM onward.
     */
    public boolean isEligibleForCheckIn(Booking booking) {

        if (booking == null
                || !"Reserved".equalsIgnoreCase(booking.getBookingStatus())) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        if (!now.toLocalDate().equals(booking.getCheckInDate())) {
            return false;
        }

        if (booking.isWalkIn()) {
            return true;
        }

        return !now.toLocalTime().isBefore(Booking.STANDARD_CHECK_IN_TIME);
    }

    /**
     * Marks a Reserved booking as Checked In, timestamps the actual arrival,
     * and updates the assigned room status to Occupied.
     */
    public void checkInBooking(String bookingId)
            throws InvalidBookingException {

        Booking booking = findBookingById(bookingId);

        if (booking == null) {
            throw new InvalidBookingException("Booking not found.");
        }

        if (!"Reserved".equalsIgnoreCase(booking.getBookingStatus())) {
            throw new InvalidBookingException("This booking is not eligible for check-in.");
        }

        if (!LocalDate.now().equals(booking.getCheckInDate())) {
            throw new InvalidBookingException(
                "Check-in is only allowed on the scheduled check-in date."
            );
        }

        if (!isEligibleForCheckIn(booking)) {
            throw new InvalidBookingException(
                "Standard check-in time is 2:00 PM. This guest can be checked in from then."
            );
        }

        markCheckedIn(booking);
    }

    private void markCheckedIn(Booking booking) {
        booking.checkIn(LocalDateTime.now());

        Room room = roomService.findRoomByNumber(booking.getRoomNumber());
        if (room != null) {
            room.setStatus("Occupied");
        }
    }

    /**
     * Checks out a currently Checked-In guest, timestamps the actual departure,
     * and releases or updates the room status accordingly.
     */
    public void checkOutBooking(String bookingId)
            throws InvalidBookingException {

        Booking booking = findBookingById(bookingId);

        if (booking == null) {
            throw new InvalidBookingException("Booking not found.");
        }

        if (!"Checked In".equalsIgnoreCase(booking.getBookingStatus())) {
            throw new InvalidBookingException("Only checked-in bookings can be checked out.");
        }

        if (LocalDate.now().isBefore(booking.getCheckInDate())) {
            throw new InvalidBookingException("Check-out cannot occur before the check-in date.");
        }

        booking.checkOut(LocalDateTime.now());
        updateRoomStatus(booking.getRoomNumber());
    }

    /**
     * Recalculates room status from active bookings:
     * Occupied if any booking is Checked In, Reserved if an upcoming Reserved booking exists,
     * otherwise Available.
     */
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

            if ("Checked In".equalsIgnoreCase(booking.getBookingStatus())) {
                occupied = true;
                break;
            }

            if ("Reserved".equalsIgnoreCase(booking.getBookingStatus())
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
            if (booking.getBookingId().equalsIgnoreCase(bookingId.trim())) {
                return booking;
            }
        }
        return null;
    }

    public List<Booking> searchBookings(String keyword) {
        ArrayList<Booking> results = new ArrayList<>();
        String search = keyword == null ? "" : keyword.trim().toLowerCase();

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