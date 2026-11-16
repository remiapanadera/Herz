
package model;

public class Admin extends Employee {

    // ==========================================
    // ADMIN ATTRIBUTE
    // ==========================================

    private String accessLevel;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public Admin(String employeeId, String fullName,
                 String username, String password,
                 String position, String accessLevel) {

        super(
                employeeId,
                fullName,
                username,
                password,
                position
        );

        setAccessLevel(accessLevel);
    }

    // ==========================================
    // GETTER
    // ==========================================

    public String getAccessLevel() {
        return accessLevel;
    }

    // ==========================================
    // SETTER
    // ==========================================

    public void setAccessLevel(String accessLevel) {

        if (accessLevel == null || accessLevel.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Access level cannot be empty."
            );
        }

        this.accessLevel = accessLevel.trim();
    }

    // ==========================================
    // ADMIN METHODS
    // ==========================================

    public boolean canManageHotel() {
        return accessLevel.equalsIgnoreCase("Full Access");
    }

    // ==========================================
    // DISPLAY INFORMATION
    // ==========================================

    @Override
    public String toString() {

        return super.toString()
                + "\nAccess Level: " + accessLevel;
    }
}
