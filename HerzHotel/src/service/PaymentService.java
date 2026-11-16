package service;

import model.Booking;
import model.Payment;
import model.Room;

import java.util.ArrayList;
import java.util.List;

public class PaymentService {

    private final ArrayList<Payment> payments;
    private final BookingService bookingService;
    private final RoomService roomService;

    public PaymentService(
            BookingService bookingService,
            RoomService roomService) {

        if (bookingService == null || roomService == null) {
            throw new IllegalArgumentException(
                "BookingService and RoomService are required."
            );
        }

        this.payments = new ArrayList<>();
        this.bookingService = bookingService;
        this.roomService = roomService;
    }

    public void addPayment(Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null.");
        }

        if (findPaymentById(payment.getPaymentId()) != null) {
            throw new IllegalArgumentException(
                "Payment ID already exists."
            );
        }

        if (bookingService.findBookingById(payment.getBookingId()) == null) {
            throw new IllegalArgumentException(
                "Booking does not exist."
            );
        }

        payments.add(payment);
    }

    public double calculateBookingTotal(String bookingId) {
        Booking booking = bookingService.findBookingById(bookingId);

        if (booking == null) {
            throw new IllegalArgumentException("Booking not found.");
        }

        Room room = roomService.findRoomByNumber(booking.getRoomNumber());

        if (room == null) {
            throw new IllegalArgumentException("Room not found.");
        }

        return room.getPricePerNight() * booking.getNumberOfNights();
    }

    public void markPaymentAsPaid(String paymentId) {
        Payment payment = findPaymentById(paymentId);

        if (payment == null) {
            throw new IllegalArgumentException("Payment not found.");
        }

        payment.markAsPaid();
    }

    public void markPaymentAsPending(String paymentId) {
        Payment payment = findPaymentById(paymentId);

        if (payment == null) {
            throw new IllegalArgumentException("Payment not found.");
        }

        payment.markAsPending();
    }

    public Payment findPaymentById(String paymentId) {
        if (paymentId == null) {
            return null;
        }

        for (Payment payment : payments) {
            if (payment.getPaymentId().equalsIgnoreCase(paymentId)) {
                return payment;
            }
        }

        return null;
    }

    public List<Payment> getPaymentsByBooking(String bookingId) {
        ArrayList<Payment> results = new ArrayList<>();

        for (Payment payment : payments) {
            if (payment.getBookingId().equalsIgnoreCase(bookingId)) {
                results.add(payment);
            }
        }

        return results;
    }

    public List<Payment> getAllPayments() {
        return new ArrayList<>(payments);
    }

    public double getTotalPaidAmount() {
        double total = 0;

        for (Payment payment : payments) {
            if (payment.isPaid()) {
                total += payment.getAmount();
            }
        }

        return total;
    }

    public int getPaymentCount() {
        return payments.size();
    }
}