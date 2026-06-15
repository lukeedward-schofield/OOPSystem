package oopsystem.util;

import oopsystem.model.User;

public final class SessionManager {

    private static User currentUser = null;

    private SessionManager() {}

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    // Convenience getters so existing code doesn't break
    public static int getLoggedInUserId() {
        return currentUser != null ? currentUser.getUserId() : -1;
    }

    public static String getLoggedInUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }

    public static String getLoggedInFullName() {
        return currentUser != null ? currentUser.getFirstName() + " " + currentUser.getLastName() : null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void clearSession() {
        currentUser = null;
    }
}