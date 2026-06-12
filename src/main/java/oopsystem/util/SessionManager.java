package oopsystem.util;

/**
 * Holds the currently logged-in user's data for the duration of the session.
 *
 * This is a static singleton — no DB calls are made here.
 * LoginController calls setLoggedInUser() once after authentication.
 * Every other controller reads from it (e.g. PassSlipIssuanceController
 * needs getLoggedInUserId() to populate the issued_by column).
 *
 * Cleared on logout via clearSession().
 */
public final class SessionManager {

    private static int    loggedInUserId   = -1;
    private static String loggedInUsername = null;
    private static String loggedInFullName = null;

    private SessionManager() {}

    // -------------------------------------------------------------------------
    // SET  — called once by LoginController after authentication succeeds
    // -------------------------------------------------------------------------

    public static void setLoggedInUser(int userId, String username, String fullName) {
        loggedInUserId   = userId;
        loggedInUsername = username;
        loggedInFullName = fullName;
    }

    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    /** Returns the user_id of the active session. -1 if no user is logged in. */
    public static int getLoggedInUserId() {
        return loggedInUserId;
    }

    public static String getLoggedInUsername() {
        return loggedInUsername;
    }

    public static String getLoggedInFullName() {
        return loggedInFullName;
    }

    public static boolean isLoggedIn() {
        return loggedInUserId != -1;
    }

    // -------------------------------------------------------------------------
    // CLEAR  — called by logout handlers before navigating back to Login
    // -------------------------------------------------------------------------

    public static void clearSession() {
        loggedInUserId   = -1;
        loggedInUsername = null;
        loggedInFullName = null;
    }
}