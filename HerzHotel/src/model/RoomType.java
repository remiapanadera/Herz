
package model;

public abstract class RoomType {

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
    // ABSTRACT METHOD
    // ==========================================

    public abstract double calculateNightlyRate();

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
