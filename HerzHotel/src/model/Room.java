
package model;

public class Room {

    // ==========================================
    // ROOM ATTRIBUTES
    // ==========================================

    private String roomNumber;
    private String roomType;
    private double pricePerNight;
    private String status;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public Room(String roomNumber, String roomType,
                double pricePerNight, String status) {

        setRoomNumber(roomNumber);
        setRoomType(roomType);
        setPricePerNight(pricePerNight);
        setStatus(status);
    }

    // ==========================================
    // GETTERS
    // ==========================================

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public double getPricePerNight() {
        return pricePerNight;
    }

    public String getStatus() {
        return status;
    }

    // ==========================================
    // SETTERS
    // ==========================================

    public void setRoomNumber(String roomNumber) {

        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Room number cannot be empty."
            );
        }

        this.roomNumber = roomNumber.trim();
    }

    public void setRoomType(String roomType) {

        if (roomType == null || roomType.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Room type cannot be empty."
            );
        }

        this.roomType = roomType.trim();
    }

    public void setPricePerNight(double pricePerNight) {

        if (pricePerNight <= 0) {
            throw new IllegalArgumentException(
                    "Room price must be greater than zero."
            );
        }

        this.pricePerNight = pricePerNight;
    }

    public void setStatus(String status) {

        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Room status cannot be empty."
            );
        }

        String normalizedStatus = status.trim();

        if (!normalizedStatus.equalsIgnoreCase("Available")
                && !normalizedStatus.equalsIgnoreCase("Occupied")
                && !normalizedStatus.equalsIgnoreCase("Reserved")
                && !normalizedStatus.equalsIgnoreCase("Maintenance")) {

            throw new IllegalArgumentException(
                    "Invalid room status."
            );
        }

        this.status = normalizedStatus;
    }

    // ==========================================
    // ROOM INFORMATION
    // ==========================================

    public boolean isAvailable() {
        return status.equalsIgnoreCase("Available");
    }

    public void markAsOccupied() {
        setStatus("Occupied");
    }

    public void markAsAvailable() {
        setStatus("Available");
    }

    // ==========================================
    // DISPLAY INFORMATION
    // ==========================================

    @Override
    public String toString() {

        return "Room Number: " + roomNumber
                + "\nRoom Type: " + roomType
                + "\nPrice per Night: " + pricePerNight
                + "\nStatus: " + status;
    }
}
