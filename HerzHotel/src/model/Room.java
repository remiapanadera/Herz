package model;

public class Room {

    // ==========================================
    // ROOM ATTRIBUTES
    // ==========================================

    private String roomNumber;
    private RoomType type;          // a StandardRoom, DeluxeRoom or SuiteRoom object
    private String status;

    // ==========================================
    // CONSTRUCTORS
    // ==========================================

    // Used by the data files and the Edit Room form.
    // pricePerNight is the final nightly rate; the room type works out its base price from it.
    public Room(String roomNumber, String roomType,
                double pricePerNight, String status) {

        setRoomNumber(roomNumber);

        if (roomType == null || roomType.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Room type cannot be empty."
            );
        }

        validatePrice(pricePerNight);

        this.type = RoomType.fromNightlyRate(roomType, pricePerNight);

        setStatus(status);
    }

    // Builds a room straight from a RoomType object (its rate decides the price).
    public Room(String roomNumber, RoomType type, String status) {

        setRoomNumber(roomNumber);
        setType(type);
        setStatus(status);
    }

    // ==========================================
    // GETTERS
    // ==========================================

    public String getRoomNumber() {
        return roomNumber;
    }

    // Name of the room type, e.g. "Deluxe".
    public String getRoomType() {
        return type.getTypeName();
    }

    // The RoomType object itself.
    public RoomType getType() {
        return type;
    }

    // Nightly rate, calculated by the room's type.
    public double getPricePerNight() {
        return type.calculateNightlyRate();
    }

    // Maximum number of guests, decided by the room's type.
    public int getCapacity() {
        return type.getCapacity();
    }

    // Total price for a stay. Each room type calculates its own rate.
    public double calculateTotal(long nights) {
        return type.calculateTotal(nights);
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

    // Switches to the new type and keeps the same base price.
    public void setRoomType(String roomType) {

        if (roomType == null || roomType.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Room type cannot be empty."
            );
        }

        this.type = RoomType.create(roomType, type.getBasePrice());
    }

    public void setType(RoomType type) {

        if (type == null) {
            throw new IllegalArgumentException(
                    "Room type cannot be empty."
            );
        }

        this.type = type;
    }

    // Sets the final nightly rate; the base price is worked back from it.
    public void setPricePerNight(double pricePerNight) {

        validatePrice(pricePerNight);

        type.setBasePrice(pricePerNight / type.getRateMultiplier());
    }

    private static void validatePrice(double pricePerNight) {

        if (!Double.isFinite(pricePerNight) || pricePerNight <= 0) {
            throw new IllegalArgumentException(
                    "Room price must be greater than zero."
            );
        }
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
                + "\nRoom Type: " + getRoomType()
                + "\nPrice per Night: " + getPricePerNight()
                + "\nStatus: " + status;
    }
}