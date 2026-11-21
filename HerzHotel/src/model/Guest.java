package model;

public class Guest extends Person {

    // ==========================================
    // GUEST ATTRIBUTES
    // ==========================================

    private String guestId;
    private String email;
    private String phoneNumber;
    private String address;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public Guest(String guestId, String fullName,
                 String email, String phoneNumber,
                 String address) {

        super(fullName);

        setGuestId(guestId);
        setEmail(email);
        setPhoneNumber(phoneNumber);
        setAddress(address);
    }

    // ==========================================
    // GETTERS
    // ==========================================

    public String getGuestId() {
        return guestId;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    // ==========================================
    // SETTERS
    // ==========================================

    public void setGuestId(String guestId) {

        if (guestId == null || guestId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Guest ID cannot be empty."
            );
        }

        this.guestId = guestId.trim();
    }

    public void setEmail(String email) {
        this.email = email == null ? "" : email.trim();
    }

    public void setPhoneNumber(String phoneNumber) {

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Phone number cannot be empty."
            );
        }

        this.phoneNumber = phoneNumber.trim();
    }

    public void setAddress(String address) {
        this.address = address == null ? "" : address.trim();
    }

    // ==========================================
    // OVERRIDDEN ROLE
    // ==========================================

    @Override
    public String getRole() {
        return "Guest";
    }

    // ==========================================
    // DISPLAY INFORMATION
    // ==========================================

    @Override
    public String toString() {

        return "Guest ID: " + guestId
                + "\nName: " + getFullName()
                + "\nEmail: " + email
                + "\nPhone: " + phoneNumber
                + "\nAddress: " + address;
    }
}