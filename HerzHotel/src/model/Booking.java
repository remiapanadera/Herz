
package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public class Booking {

    // ==========================================
    // HOTEL STANDARD TIMES AND BOOKING TYPES
    // ==========================================

    public static final LocalTime STANDARD_CHECK_IN_TIME  = LocalTime.of(14, 0); // 2:00 PM
    public static final LocalTime STANDARD_CHECK_OUT_TIME = LocalTime.of(11, 0); // 11:00 AM

    public static final String TYPE_WALK_IN = "Walk-in";
    public static final String TYPE_ADVANCE = "Advance Booking";

    // ==========================================
    // BOOKING ATTRIBUTES
    // ==========================================

    private String bookingId;
    private String guestId;
    private String roomNumber;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String bookingStatus;
    private String bookingType;               // "Walk-in" or "Advance Booking"
    private LocalDateTime actualCheckIn;      // set when the guest really checks in
    private LocalDateTime actualCheckOut;     // set when the guest really checks out

    // ==========================================
    // CONSTRUCTORS
    // ==========================================

    // Older code and older saved files use this form (treated as an advance booking).
    public Booking(String bookingId, String guestId,
                   String roomNumber, LocalDate checkInDate,
                   LocalDate checkOutDate, String bookingStatus) {

        this(bookingId, guestId, roomNumber, checkInDate,
             checkOutDate, bookingStatus, TYPE_ADVANCE, null, null);
    }

    public Booking(String bookingId, String guestId,
                   String roomNumber, LocalDate checkInDate,
                   LocalDate checkOutDate, String bookingStatus,
                   String bookingType, LocalDateTime actualCheckIn,
                   LocalDateTime actualCheckOut) {

        setBookingId(bookingId);
        setGuestId(guestId);
        setRoomNumber(roomNumber);
        setCheckInDate(checkInDate);
        setCheckOutDate(checkOutDate);
        setBookingStatus(bookingStatus);
        setBookingType(bookingType);
        this.actualCheckIn = actualCheckIn;
        this.actualCheckOut = actualCheckOut;
    }

    // ==========================================
    // GETTERS
    // ==========================================

    public String getBookingId() {
        return bookingId;
    }

    public String getGuestId() {
        return guestId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public String getBookingType() {
        return bookingType;
    }

    public boolean isWalkIn() {
        return TYPE_WALK_IN.equals(bookingType);
    }

    public LocalDateTime getActualCheckIn() {
        return actualCheckIn;
    }

    public LocalDateTime getActualCheckOut() {
        return actualCheckOut;
    }

    // The standard check-in (2:00 PM) and check-out (11:00 AM) moments of this stay
    public LocalDateTime getStandardCheckIn() {
        return checkInDate.atTime(STANDARD_CHECK_IN_TIME);
    }

    public LocalDateTime getStandardCheckOut() {
        return checkOutDate.atTime(STANDARD_CHECK_OUT_TIME);
    }

    // ==========================================
    // SETTERS
    // ==========================================

    public void setBookingId(String bookingId) {

        if (bookingId == null || bookingId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Booking ID cannot be empty."
            );
        }

        this.bookingId = bookingId.trim();
    }

    public void setGuestId(String guestId) {

        if (guestId == null || guestId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Guest ID cannot be empty."
            );
        }

        this.guestId = guestId.trim();
    }

    public void setRoomNumber(String roomNumber) {

        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Room number cannot be empty."
            );
        }

        this.roomNumber = roomNumber.trim();
    }

    public void setCheckInDate(LocalDate checkInDate) {

        if (checkInDate == null) {
            throw new IllegalArgumentException(
                    "Check-in date cannot be empty."
            );
        }

        if (checkOutDate != null && !checkInDate.isBefore(checkOutDate)) {
            throw new IllegalArgumentException(
                    "Check-in must be before check-out."
            );
        }

        this.checkInDate = checkInDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {

        if (checkOutDate == null) {
            throw new IllegalArgumentException(
                    "Check-out date cannot be empty."
            );
        }

        if (checkInDate != null && !checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException(
                    "Check-out must be after check-in."
            );
        }

        this.checkOutDate = checkOutDate;
    }

    public void setBookingStatus(String bookingStatus) {

        if (bookingStatus == null || bookingStatus.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Booking status cannot be empty."
            );
        }

        String normalizedStatus = bookingStatus.trim();

        if (!normalizedStatus.equalsIgnoreCase("Reserved")
                && !normalizedStatus.equalsIgnoreCase("Checked In")
                && !normalizedStatus.equalsIgnoreCase("Checked Out")
                && !normalizedStatus.equalsIgnoreCase("Cancelled")) {

            throw new IllegalArgumentException(
                    "Invalid booking status."
            );
        }

        this.bookingStatus = normalizedStatus;
    }

    public void setBookingType(String bookingType) {

        if (bookingType != null && bookingType.trim().equalsIgnoreCase(TYPE_WALK_IN)) {
            this.bookingType = TYPE_WALK_IN;
        } else {
            this.bookingType = TYPE_ADVANCE;
        }
    }

    // ==========================================
    // BOOKING METHODS
    // ==========================================

    public long getNumberOfNights() {

        return ChronoUnit.DAYS.between(
                checkInDate,
                checkOutDate
        );
    }

    public void checkIn() {
        checkIn(LocalDateTime.now());
    }

    public void checkIn(LocalDateTime when) {
        setBookingStatus("Checked In");
        this.actualCheckIn = when;
    }

    public void checkOut() {
        checkOut(LocalDateTime.now());
    }

    public void checkOut(LocalDateTime when) {
        setBookingStatus("Checked Out");
        this.actualCheckOut = when;
    }

    public void cancelBooking() {
        setBookingStatus("Cancelled");
    }

    // ==========================================
    // DISPLAY INFORMATION
    // ==========================================

    @Override
    public String toString() {

        return "Booking ID: " + bookingId
                + "\nGuest ID: " + guestId
                + "\nRoom Number: " + roomNumber
                + "\nCheck-in: " + checkInDate
                + "\nCheck-out: " + checkOutDate
                + "\nNumber of Nights: " + getNumberOfNights()
                + "\nStatus: " + bookingStatus
                + "\nType: " + bookingType;
    }
}
