
package model;

public class Guest {

    // ==========================================
    // GUEST ATTRIBUTES
    // ==========================================

    private String guestId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public Guest(String guestId, String fullName,
                 String email, String phoneNumber,
                 String address) {

        setGuestId(guestId);
        setFullName(fullName);
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

    public String getFullName() {
        return fullName;
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

    public void setFullName(String fullName) {

        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Guest name cannot be empty."
            );
        }

        this.fullName = fullName.trim();
    }

    public void setEmail(String email) {

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Email cannot be empty."
            );
        }

        this.email = email.trim();
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

        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Address cannot be empty."
            );
        }

        this.address = address.trim();
    }

    // ==========================================
    // DISPLAY INFORMATION
    // ==========================================

    @Override
    public String toString() {

        return "Guest ID: " + guestId
                + "\nName: " + fullName
                + "\nEmail: " + email
                + "\nPhone: " + phoneNumber
                + "\nAddress: " + address;
    }
}
