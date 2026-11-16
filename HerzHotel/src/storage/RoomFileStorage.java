package storage;

import model.FileSavable;
import model.Room;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class RoomFileStorage implements FileSavable {

    private final ArrayList<Room> rooms;

    public RoomFileStorage() {
        rooms = new ArrayList<>();
    }

    public void setRooms(List<Room> roomList) {
        rooms.clear();

        if (roomList != null) {
            rooms.addAll(roomList);
        }
    }

    public List<Room> getRooms() {
        return new ArrayList<>(rooms);
    }

    @Override
    public void saveToFile(String filePath) throws IOException {

        Path path = Path.of(filePath);

        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        List<String> lines = new ArrayList<>();

        for (Room room : rooms) {
            String line =
                    encode(room.getRoomNumber()) + "|"
                    + encode(room.getRoomType()) + "|"
                    + room.getPricePerNight() + "|"
                    + encode(room.getStatus());

            lines.add(line);
        }

        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    @Override
    public void loadFromFile(String filePath) throws IOException {

        Path path = Path.of(filePath);

        rooms.clear();

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

            if (fields.length != 4) {
                throw new IOException(
                        "Invalid room record in storage file."
                );
            }

            try {
                Room room = new Room(
                        decode(fields[0]),
                        decode(fields[1]),
                        Double.parseDouble(fields[2]),
                        decode(fields[3])
                );

                rooms.add(room);

            } catch (IllegalArgumentException e) {
                throw new IOException(
                        "Unable to read a room record.",
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