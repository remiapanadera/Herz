package model;

import java.util.List;

public class DeluxeRoom extends RoomType {

    // ==========================================
    // RATE AND CAPACITY RULES
    // ==========================================

    public static final double RATE_MULTIPLIER = 2500.0 / DEFAULT_BASE_PRICE;
    public static final int CAPACITY = 3;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public DeluxeRoom(double basePrice) {
        super("Deluxe", basePrice);
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
        return List.of("Queen + Single", "Air conditioning", "Private bathroom", "Television");
    }
}