package utils;

import model.User;

/**
 * Singleton class that holds the currently logged-in user's session.
 * Use Session.getInstance() from any controller to access the logged-in user.
 */
public class Session {

    private static Session instance;
    private User currentUser;

    private Session() {
        // Private constructor — use getInstance()
    }

    /**
     * Returns the single Session instance (creates it on first call).
     */
    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    /**
     * Stores the authenticated user in the session.
     * Call this after a successful login.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Returns the currently logged-in user, or null if no one is logged in.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Returns true if a user is currently logged in.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Clears the session (logout).
     */
    public void logout() {
        currentUser = null;

    }

    /**
     * Destroys the session instance entirely.
     */
    public static void destroy() {
        instance = null;
    }
}
