package cnu.ssd.pennywise.model;

import org.springframework.stereotype.Component;

/**
 * Holds the current logged-in {@link User} for the running client.
 */
@Component
public class Session {

    private User currentUser;

    public User getCurrentUser() {
        return currentUser;
    }

    public void login(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }
}
