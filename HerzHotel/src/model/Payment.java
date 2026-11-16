
package model;

import java.time.LocalDate;

public class Payment {

    // ==========================================
    // PAYMENT ATTRIBUTES
    // ==========================================

    private String paymentId;
    private String bookingId;
    private double amount;
    private String paymentMethod;
    private LocalDate paymentDate;
    private String paymentStatus;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public Payment(String paymentId, String bookingId,
                   double amount, String paymentMethod,
                   LocalDate paymentDate, String paymentStatus) {

        setPaymentId(paymentId);
        setBookingId(bookingId);
        setAmount(amount);
        setPaymentMethod(paymentMethod);
        setPaymentDate(paymentDate);
        setPaymentStatus(paymentStatus);
    }

    // ==========================================
    // GETTERS
    // ==========================================

    public String getPaymentId() {
        return paymentId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public double getAmount() {
        return amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    // ==========================================
    // SETTERS
    // ==========================================

    public void setPaymentId(String paymentId) {

        if (paymentId == null || paymentId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Payment ID cannot be empty."
            );
        }

        this.paymentId = paymentId.trim();
    }

    public void setBookingId(String bookingId) {

        if (bookingId == null || bookingId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Booking ID cannot be empty."
            );
        }

        this.bookingId = bookingId.trim();
    }

    public void setAmount(double amount) {

        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero."
            );
        }

        this.amount = amount;
    }

    public void setPaymentMethod(String paymentMethod) {

        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Payment method cannot be empty."
            );
        }

        String normalizedMethod = paymentMethod.trim();

        if (!normalizedMethod.equalsIgnoreCase("Cash")
                && !normalizedMethod.equalsIgnoreCase("Card")
                && !normalizedMethod.equalsIgnoreCase("Bank Transfer")
                && !normalizedMethod.equalsIgnoreCase("E-Wallet")) {

            throw new IllegalArgumentException(
                    "Invalid payment method."
            );
        }

        this.paymentMethod = normalizedMethod;
    }

    public void setPaymentDate(LocalDate paymentDate) {

        if (paymentDate == null) {
            throw new IllegalArgumentException(
                    "Payment date cannot be empty."
            );
        }

        this.paymentDate = paymentDate;
    }

    public void setPaymentStatus(String paymentStatus) {

        if (paymentStatus == null || paymentStatus.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Payment status cannot be empty."
            );
        }

        String normalizedStatus = paymentStatus.trim();

        if (!normalizedStatus.equalsIgnoreCase("Paid")
                && !normalizedStatus.equalsIgnoreCase("Pending")
                && !normalizedStatus.equalsIgnoreCase("Refunded")) {

            throw new IllegalArgumentException(
                    "Invalid payment status."
            );
        }

        this.paymentStatus = normalizedStatus;
    }

    // ==========================================
    // PAYMENT METHODS
    // ==========================================

    public void markAsPaid() {
        setPaymentStatus("Paid");
    }

    public void markAsPending() {
        setPaymentStatus("Pending");
    }

    public boolean isPaid() {
        return paymentStatus.equalsIgnoreCase("Paid");
    }

    // ==========================================
    // DISPLAY INFORMATION
    // ==========================================

    @Override
    public String toString() {

        return "Payment ID: " + paymentId
                + "\nBooking ID: " + bookingId
                + "\nAmount: PHP " + String.format("%.2f", amount)
                + "\nPayment Method: " + paymentMethod
                + "\nPayment Date: " + paymentDate
                + "\nPayment Status: " + paymentStatus;
    }
}
