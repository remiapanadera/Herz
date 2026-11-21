package model;

public class Employee extends Person {

    // ==========================================
    // EMPLOYEE ATTRIBUTES
    // ==========================================

    private String username;
    private String password;
    private String position;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public Employee(String fullName,
                    String username, String password,
                    String position) {

        super(fullName);

        setUsername(username);
        setPassword(password);
        setPosition(position);
    }

    // ==========================================
    // GETTERS
    // ==========================================

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getPosition() {
        return position;
    }

    // ==========================================
    // SETTERS
    // ==========================================

    public void setUsername(String username) {

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Username cannot be empty."
            );
        }

        this.username = username.trim();
    }

    public void setPassword(String password) {

        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException(
                    "Password cannot be empty."
            );
        }

        this.password = password;
    }

    public void setPosition(String position) {

        if (position == null || position.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Employee position cannot be empty."
            );
        }

        this.position = position.trim();
    }

    // ==========================================
    // OVERRIDDEN ROLE
    // ==========================================

    @Override
    public String getRole() {
        return "Employee";
    }

    // ==========================================
    // DISPLAY INFORMATION
    // ==========================================

    @Override
    public String toString() {

        return "Name: " + getFullName()
                + "\nUsername: " + username
                + "\nPosition: " + position;
    }
}