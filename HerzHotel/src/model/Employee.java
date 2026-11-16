
package model;

public class Employee {

    // ==========================================
    // EMPLOYEE ATTRIBUTES
    // ==========================================

    private String employeeId;
    private String fullName;
    private String username;
    private String password;
    private String position;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public Employee(String employeeId, String fullName,
                    String username, String password,
                    String position) {

        setEmployeeId(employeeId);
        setFullName(fullName);
        setUsername(username);
        setPassword(password);
        setPosition(position);
    }

    // ==========================================
    // GETTERS
    // ==========================================

    public String getEmployeeId() {
        return employeeId;
    }

    public String getFullName() {
        return fullName;
    }

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

    public void setEmployeeId(String employeeId) {

        if (employeeId == null || employeeId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Employee ID cannot be empty."
            );
        }

        this.employeeId = employeeId.trim();
    }

    public void setFullName(String fullName) {

        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Employee name cannot be empty."
            );
        }

        this.fullName = fullName.trim();
    }

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
    // DISPLAY INFORMATION
    // ==========================================

    @Override
    public String toString() {

        return "Employee ID: " + employeeId
                + "\nName: " + fullName
                + "\nUsername: " + username
                + "\nPosition: " + position;
    }
}
