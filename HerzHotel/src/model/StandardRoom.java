
package model;

public class StandardRoom extends RoomType {

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public StandardRoom(double basePrice) {
        super("Standard", basePrice);
    }

    // ==========================================
    // OVERRIDDEN NIGHTLY RATE
    // ==========================================

    @Override
    public double calculateNightlyRate() {

        return getBasePrice();
    }
}
