
package model;

public class SuiteRoom extends RoomType {

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public SuiteRoom(double basePrice) {
        super("Suite", basePrice);
    }

    // ==========================================
    // OVERRIDDEN NIGHTLY RATE
    // ==========================================

    @Override
    public double calculateNightlyRate() {

        return getBasePrice() * 1.40;
    }
}
