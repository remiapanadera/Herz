package model;

import java.util.List;

public abstract class RoomType {

    // ==========================================
    // RATE RULES
    // ==========================================

    // Standard room price per night. Deluxe and Suite rates are worked out from this
    // (Deluxe = 2,500 and Suite = 4,000 when the base price is 1,500).
    public static final double DEFAULT_BASE_PRICE = 1500.0;

    // ==========================================
    // ATTRIBUTES
    // ==========================================

    private String typeName;
    private double basePrice;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public RoomType(String typeName, double basePrice) {

        setTypeName(typeName);
        setBasePrice(basePrice);
    }

    // ==========================================
    // FACTORY METHODS
    // ==========================================

    // Creates the right subclass for a type name ("Standard", "Deluxe" or "Suite").
    public static RoomType create(String typeName, double basePrice) {

        if (typeName == null || typeName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Room type cannot be empty."
            );
        }

        String name = typeName.trim();

        if (name.equalsIgnoreCase("Standard")) {
            return new StandardRoom(basePrice);
        }

        if (name.equalsIgnoreCase("Deluxe")) {
            return new DeluxeRoom(basePrice);
        }

        if (name.equalsIgnoreCase("Suite")) {
            return new SuiteRoom(basePrice);
        }

        throw new IllegalArgumentException(
                "Invalid room type: " + name + "."
        );
    }

    // Creates a room type whose nightly rate comes out to exactly the given rate.
    // The base price is worked back from the rate, so rates already saved in the
    // data files keep their value.
    public static RoomType fromNightlyRate(String typeName, double nightlyRate) {

        double multiplier = create(typeName, 1.0).getRateMultiplier();

        return create(typeName, nightlyRate / multiplier);
    }

    // ==========================================
    // GETTERS
    // ==========================================

    public String getTypeName() {
        return typeName;
    }

    public double getBasePrice() {
        return basePrice;
    }

    // ==========================================
    // SETTERS
    // ==========================================

    public void setTypeName(String typeName) {

        if (typeName == null || typeName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Room type name cannot be empty."
            );
        }

        this.typeName = typeName.trim();
    }

    public void setBasePrice(double basePrice) {

        if (!Double.isFinite(basePrice) || basePrice <= 0) {
            throw new IllegalArgumentException(
                    "Base price must be greater than zero."
            );
        }

        this.basePrice = basePrice;
    }

    // ==========================================
    // ABSTRACT METHODS (every room type overrides these)
    // ==========================================

    // Price per night for this type of room.
    public abstract double calculateNightlyRate();

    // How much the base price is scaled to get the nightly rate.
    public abstract double getRateMultiplier();

    // Maximum number of guests the room can hold.
    public abstract int getCapacity();

    // What the room comes with (shown on the Dashboard rate cards).
    public abstract List<String> getFeatures();

    // ==========================================
    // SHARED HELPERS
    // ==========================================

    // Total price of a stay. Works for any subclass through dynamic binding.
    public double calculateTotal(long nights) {

        return roundMoney(calculateNightlyRate() * Math.max(nights, 0));
    }

    // Rounds to 2 decimals so values like 2500.0000000000005 stay 2500.0.
    protected static double roundMoney(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    // ==========================================
    // DISPLAY INFORMATION
    // ==========================================

    @Override
    public String toString() {

        return typeName
                + " - PHP "
                + String.format("%.2f", calculateNightlyRate());
    }
}