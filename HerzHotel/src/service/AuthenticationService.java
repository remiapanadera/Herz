package service;

import model.Admin;

public class AuthenticationService {

    private Admin administrator;

    public AuthenticationService(Admin administrator) {
        if (administrator == null) {
            throw new IllegalArgumentException(
                "Administrator account is required."
            );
        }

        this.administrator = administrator;
    }

    public boolean authenticate(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        return administrator.getUsername().equals(username)
                && administrator.getPassword().equals(password);
    }

    public Admin getAdministrator() {
        return administrator;
    }

    public void updateAdministrator(Admin administrator) {
        if (administrator == null) {
            throw new IllegalArgumentException(
                "Administrator cannot be null."
            );
        }

        this.administrator = administrator;
    }
}