package model;

import java.util.List;

public class SuiteRoom extends RoomType {

    // ==========================================
    // RATE AND CAPACITY RULES
    // ==========================================

    public static final double RATE_MULTIPLIER = 4000.0 / DEFAULT_BASE_PRICE;
    public static final int CAPACITY = 4;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public SuiteRoom(double basePrice) {
        super("Suite", basePrice);
    }

    // ==========================================
    // OVERRIDDEN METHODS
    // ==========================================

    @Override
    public double calculateNightlyRate() {

        return roundMoney(getBasePrice() * RATE_MULTIPLIER);
    }

    @Override
    public double getRateMultiplier() {
        return RATE_MULTIPLIER;
    }

    @Override
    public int getCapacity() {
        return CAPACITY;
    }

    @Override
    public List<String> getFeatures() {
        return List.of("2 Queen beds", "Air conditioning", "Private bathroom", "Television");
    }
}