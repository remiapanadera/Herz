package service;

import exception.DuplicateRecordException;
import model.Admin;

import java.util.ArrayList;
import java.util.List;

public class AuthenticationService {

    private final ArrayList<Admin> admins;

    public AuthenticationService() {
        admins = new ArrayList<>();
        loadAdmins();
    }

    // =====================================================================
    //  >>> THE ADMIN LIST: EDIT THIS METHOD TO ADD, CHANGE OR DELETE ADMINS <<<
    //
    //  Every admin is one "addAdmin(new Admin(...))" line below.
    //
    //  The values, in order:
    //     1. full name     (shown at the top right after logging in)
    //     2. username      (what is typed on the login screen)
    //     3. password      (what is typed on the login screen)
    //     4. position      (any text, e.g. "Front Desk Manager")
    //     5. access level  (any text, e.g. "Full Access")
    //
    //  Two admins cannot have the same username. If they do, the program
    //  stops at login and says which username is repeated.
    // =====================================================================
    private void loadAdmins() {

        addAdmin(new Admin("Trisha Dajac",   "Trisha Dajac",   "admin123", "Hotel Administrator", "Full Access"));
        addAdmin(new Admin("Heini Llaneta",  "Heini Llaneta",  "admin12",  "Hotel Administrator", "Full Access"));
        addAdmin(new Admin("Remia Panadera", "Remia Panadera", "admin1",   "Hotel Administrator", "Full Access"));

        // To add another admin, copy a line above and paste it here.
    }
    // ======================= END OF THE ADMIN LIST =======================

    // Adds an admin. Used by loadAdmins() above.
    private void addAdmin(Admin admin) {

        try {
            register(admin);

        } catch (DuplicateRecordException e) {
            throw new IllegalStateException(
                "Fix the admin list in AuthenticationService.loadAdmins(): "
                    + e.getMessage(),
                e
            );
        }
    }

    // Adds an admin while the program is running. Usernames must be unique.
    public void register(Admin admin) throws DuplicateRecordException {

        if (admin == null) {
            throw new IllegalArgumentException("Administrator cannot be null.");
        }

        if (findByUsername(admin.getUsername()) != null) {
            throw new DuplicateRecordException(
                "The username \"" + admin.getUsername() + "\" is used by more than one admin."
            );
        }

        admins.add(admin);
    }

    // Removes the admin with this username. Returns true if one was removed.
    public boolean remove(String username) {

        Admin admin = findByUsername(username);

        if (admin == null) {
            return false;
        }

        return admins.remove(admin);
    }

    // Returns the matching admin, or null when the username or password is wrong.
    public Admin login(String username, String password) {

        if (username == null || password == null) {
            return null;
        }

        Admin admin = findByUsername(username.trim());

        if (admin != null && admin.getPassword().equals(password)) {
            return admin;
        }

        return null;
    }

    public Admin findByUsername(String username) {

        if (username == null) {
            return null;
        }

        for (Admin admin : admins) {
            if (admin.getUsername().equals(username)) {
                return admin;
            }
        }

        return null;
    }

    public List<Admin> getAllAdmins() {
        return new ArrayList<>(admins);
    }
}