package storage;

import model.FileSavable;
import model.Guest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class GuestFileStorage implements FileSavable {

    private final ArrayList<Guest> guests;

    public GuestFileStorage() {
        guests = new ArrayList<>();
    }

    public void setGuests(List<Guest> guestList) {
        guests.clear();

        if (guestList != null) {
            guests.addAll(guestList);
        }
    }

    public List<Guest> getGuests() {
        return new ArrayList<>(guests);
    }

    @Override
    public void saveToFile(String filePath) throws IOException {

        Path path = Path.of(filePath);

        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        List<String> lines = new ArrayList<>();

        for (Guest guest : guests) {
            String line =
                    encode(guest.getGuestId()) + "|"
                    + encode(guest.getFullName()) + "|"
                    + encode(guest.getEmail()) + "|"
                    + encode(guest.getPhoneNumber()) + "|"
                    + encode(guest.getAddress());

            lines.add(line);
        }

        Files.write(
                path,
                lines,
                StandardCharsets.UTF_8
        );
    }

    @Override
    public void loadFromFile(String filePath) throws IOException {

        Path path = Path.of(filePath);

        guests.clear();

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

            if (fields.length != 5) {
                throw new IOException(
                        "Invalid guest record in storage file."
                );
            }

            try {
                Guest guest = new Guest(
                        decode(fields[0]),
                        decode(fields[1]),
                        decode(fields[2]),
                        decode(fields[3]),
                        decode(fields[4])
                );

                guests.add(guest);

            } catch (IllegalArgumentException e) {
                throw new IOException(
                        "Unable to read a guest record.",
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