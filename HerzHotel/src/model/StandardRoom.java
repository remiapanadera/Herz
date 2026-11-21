package model;

import java.util.List;

public class StandardRoom extends RoomType {

    // ==========================================
    // RATE AND CAPACITY RULES
    // ==========================================

    public static final double RATE_MULTIPLIER = 1.0;
    public static final int CAPACITY = 2;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public StandardRoom(double basePrice) {
        super("Standard", basePrice);
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
        return List.of("1 Queen bed", "Air conditioning", "Private bathroom", "Wi-Fi");
    }
}