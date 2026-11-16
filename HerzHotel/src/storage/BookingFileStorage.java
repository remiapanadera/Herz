package storage;

import model.Booking;
import model.FileSavable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class BookingFileStorage implements FileSavable {

    private final ArrayList<Booking> bookings;

    public BookingFileStorage() {
        bookings = new ArrayList<>();
    }

    public void setBookings(List<Booking> bookingList) {
        bookings.clear();

        if (bookingList != null) {
            bookings.addAll(bookingList);
        }
    }

    public List<Booking> getBookings() {
        return new ArrayList<>(bookings);
    }

    @Override
    public void saveToFile(String filePath) throws IOException {

        Path path = Path.of(filePath);

        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        List<String> lines = new ArrayList<>();

        for (Booking booking : bookings) {
            String line =
                    encode(booking.getBookingId()) + "|"
                    + encode(booking.getGuestId()) + "|"
                    + encode(booking.getRoomNumber()) + "|"
                    + booking.getCheckInDate() + "|"
                    + booking.getCheckOutDate() + "|"
                    + encode(booking.getBookingStatus());

            lines.add(line);
        }

        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    @Override
    public void loadFromFile(String filePath) throws IOException {

        Path path = Path.of(filePath);

        bookings.clear();

        if (!Files.exists(path)) {
            return;
        }

        List<String> lines = Files.readAllLines(
                path,
                StandardCharsets.UTF_8
        );

        for (String line : lines) {

            if (line.isBlank()) {
                continue;
            }

            String[] fields = line.split("\\|", -1);

            if (fields.length != 6) {
                throw new IOException(
                        "Invalid booking record in storage file."
                );
            }

            try {
                Booking booking = new Booking(
                        decode(fields[0]),
                        decode(fields[1]),
                        decode(fields[2]),
                        LocalDate.parse(fields[3]),
                        LocalDate.parse(fields[4]),
                        decode(fields[5])
                );

                bookings.add(booking);

            } catch (IllegalArgumentException e) {
                throw new IOException(
                        "Unable to read a booking record.",
                        e
                );
            }
        }
    }

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String decode(String value) {
        return new String(
                Base64.getDecoder().decode(value),
                StandardCharsets.UTF_8
        );
    }
}