
package model;

public class DeluxeRoom extends RoomType {

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public DeluxeRoom(double basePrice) {
        super("Deluxe", basePrice);
    }

    // ==========================================
    // OVERRIDDEN NIGHTLY RATE
    // ==========================================

    @Override
    public double calculateNightlyRate() {

        return getBasePrice() * 1.20;
    }
}
