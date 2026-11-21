package model;

// Parent of Guest and Employee (Admin extends Employee).
// Holds what every person in the system has: a name and a role.
public abstract class Person {

    // ==========================================
    // PERSON ATTRIBUTE
    // ==========================================

    private String fullName;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public Person(String fullName) {
        setFullName(fullName);
    }

    // ==========================================
    // GETTER
    // ==========================================

    public String getFullName() {
        return fullName;
    }

    // ==========================================
    // SETTER
    // ==========================================

    public void setFullName(String fullName) {

        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    getRole() + " name cannot be empty."
            );
        }

        this.fullName = fullName.trim();
    }

    // ==========================================
    // ABSTRACT METHOD (each subclass names its own role)
    // ==========================================

    public abstract String getRole();
}