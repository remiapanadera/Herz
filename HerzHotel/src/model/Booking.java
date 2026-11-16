
package model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Booking {

    // ==========================================
    // BOOKING ATTRIBUTES
    // ==========================================

    private String bookingId;
    private String guestId;
    private String roomNumber;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String bookingStatus;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public Booking(String bookingId, String guestId,
                   String roomNumber, LocalDate checkInDate,
                   LocalDate checkOutDate, String bookingStatus) {

        setBookingId(bookingId);
        setGuestId(guestId);
        setRoomNumber(roomNumber);
        setCheckInDate(checkInDate);
        setCheckOutDate(checkOutDate);
        setBookingStatus(bookingStatus);
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
        setBookingStatus("Checked In");
    }

    public void checkOut() {
        setBookingStatus("Checked Out");
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
                + "\nStatus: " + bookingStatus;
    }
}
