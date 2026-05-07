package utils;

import Model.user.User;

/**
 * Session — holds the currently logged-in user.
 * Usage:
 *   Session.getInstance().setCurrentUser(user)  - set after login
 *   Session.getInstance().logout()              - clear on logout
 *   Session.getCurrentUser()                    - get user (static, used by task services)
 *   Session.isLoggedIn()                        - check login (static, used by task services)
 *   Session.getInstance().getCurrentUser()      - also works (delegates to static field)
 */
public class Session {

    private static Session instance;
    private static User currentUser;
    private static String resetEmail;
    private static String verificationCode;

    private Session() {}

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    public void setResetData(String email, String code) {
        resetEmail = email;
        verificationCode = code;
    }

    public String getResetEmail() {
        return resetEmail;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void clearResetData() {
        resetEmail = null;
        verificationCode = null;
    }

    /** Set the logged-in user after successful authentication. */
    public void setCurrentUser(User user) {
        currentUser = user;
    }

    /** @return the currently logged-in user, or null */
    public static User getCurrentUser() {
        return currentUser;
    }

    /** @return true if a user is currently logged in */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /** Clear the session (logout). */
    public void logout() {
        currentUser = null;
    }

    public static void destroy() {
        instance = null;
        currentUser = null;
    }
}
