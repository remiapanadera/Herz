package storage;

import java.io.IOException;
import java.nio.file.Path;

public class HotelDataManager {

    private final GuestFileStorage guestStorage;
    private final RoomFileStorage roomStorage;
    private final BookingFileStorage bookingStorage;
    private final PaymentFileStorage paymentStorage;

    private final Path dataDirectory;

    public HotelDataManager() {
        this("data");
    }

    public HotelDataManager(String directory) {
        dataDirectory = Path.of(directory);

        guestStorage = new GuestFileStorage();
        roomStorage = new RoomFileStorage();
        bookingStorage = new BookingFileStorage();
        paymentStorage = new PaymentFileStorage();
    }

    public GuestFileStorage getGuestStorage() {
        return guestStorage;
    }

    public RoomFileStorage getRoomStorage() {
        return roomStorage;
    }

    public BookingFileStorage getBookingStorage() {
        return bookingStorage;
    }

    public PaymentFileStorage getPaymentStorage() {
        return paymentStorage;
    }

    public void loadAllData() throws IOException {
        guestStorage.loadFromFile(
                dataDirectory.resolve("guests.txt").toString()
        );

        roomStorage.loadFromFile(
                dataDirectory.resolve("rooms.txt").toString()
        );

        bookingStorage.loadFromFile(
                dataDirectory.resolve("bookings.txt").toString()
        );

        paymentStorage.loadFromFile(
                dataDirectory.resolve("payments.txt").toString()
        );
    }

    public void saveAllData() throws IOException {
        guestStorage.saveToFile(
                dataDirectory.resolve("guests.txt").toString()
        );

        roomStorage.saveToFile(
                dataDirectory.resolve("rooms.txt").toString()
        );

        bookingStorage.saveToFile(
                dataDirectory.resolve("bookings.txt").toString()
        );

        paymentStorage.saveToFile(
                dataDirectory.resolve("payments.txt").toString()
        );
    }
}