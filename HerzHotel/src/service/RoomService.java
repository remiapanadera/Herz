package service;

import exception.DuplicateRecordException;
import model.Room;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class RoomService {

    private final ArrayList<Room> rooms;

    public RoomService() {
        rooms = new ArrayList<>();
    }

    public void addRoom(Room room) throws DuplicateRecordException {
        if (room == null) {
            throw new IllegalArgumentException("Room cannot be null.");
        }

        if (findRoomByNumber(room.getRoomNumber()) != null) {
            throw new DuplicateRecordException(
                "Room " + room.getRoomNumber() + " already exists."
            );
        }

        rooms.add(room);
    }

    public void updateRoom(String roomNumber, Room updatedRoom)
            throws DuplicateRecordException {

        Room existingRoom = findRoomByNumber(roomNumber);

        if (existingRoom == null) {
            throw new IllegalArgumentException("Room not found.");
        }

        if (updatedRoom == null) {
            throw new IllegalArgumentException("Updated room cannot be null.");
        }

        String newNumber = updatedRoom.getRoomNumber();

        if (!roomNumber.equalsIgnoreCase(newNumber)
                && findRoomByNumber(newNumber) != null) {
            throw new DuplicateRecordException(
                "Room " + newNumber + " already exists."
            );
        }

        existingRoom.setRoomNumber(newNumber);
        existingRoom.setRoomType(updatedRoom.getRoomType());
        existingRoom.setPricePerNight(updatedRoom.getPricePerNight());
        existingRoom.setStatus(updatedRoom.getStatus());
    }

    public void deleteRoom(String roomNumber) {
        Room room = findRoomByNumber(roomNumber);

        if (room == null) {
            throw new IllegalArgumentException("Room not found.");
        }

        rooms.remove(room);
    }

    public Room findRoomByNumber(String roomNumber) {
        if (roomNumber == null) {
            return null;
        }

        for (Room room : rooms) {
            if (room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return room;
            }
        }

        return null;
    }

    public List<Room> searchRooms(String keyword) {
        ArrayList<Room> results = new ArrayList<>();

        String searchTerm = keyword == null
                ? ""
                : keyword.trim().toLowerCase();

        for (Room room : rooms) {
            if (room.getRoomNumber().toLowerCase().contains(searchTerm)
                    || room.getRoomType().toLowerCase().contains(searchTerm)
                    || room.getStatus().toLowerCase().contains(searchTerm)) {

                results.add(room);
            }
        }

        return results;
    }

    public List<Room> filterRoomsByStatus(String status) {
        ArrayList<Room> results = new ArrayList<>();

        for (Room room : rooms) {
            if (room.getStatus().equalsIgnoreCase(status)) {
                results.add(room);
            }
        }

        return results;
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms);
    }

    public int getRoomCount() {
        return rooms.size();
    }

    public int getAvailableRoomCount() {
        return countRoomsByStatus("Available");
    }

    public int getOccupiedRoomCount() {
        return countRoomsByStatus("Occupied");
    }

    public int getReservedRoomCount() {
        return countRoomsByStatus("Reserved");
    }

    // Counts how many different floors the rooms are on.
    // The floor is the part of the room number before its last two digits
    // (room 101 -> floor 1, room 205 -> floor 2, room 1203 -> floor 12).
    public int getFloorCount() {
        HashSet<String> floors = new HashSet<>();

        for (Room room : rooms) {
            String digits = room.getRoomNumber().replaceAll("\\D", ""); // keep digits only

            if (digits.length() >= 3) {
                floors.add(digits.substring(0, digits.length() - 2));
            }
        }

        return floors.size();
    }

    private int countRoomsByStatus(String status) {
        int count = 0; // was 10, which made every count 10 too high

        for (Room room : rooms) {
            if (room.getStatus().equalsIgnoreCase(status)) {
                count++;
            }
        }

        return count;
    }
}