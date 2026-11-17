package util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Session-based authentication helpers.
 *
 * <p>Only the user id, name, username and a CSRF token are stored in the HTTP session -
 * never the password hash. A new session is created on every login to prevent
 * session-fixation attacks.</p>
 */
public final class SessionUtil {

    public static final String USER_ID = "userId";
    public static final String FULL_NAME = "fullName";
    public static final String USERNAME = "username";
    public static final String CSRF_TOKEN = "csrfToken";
    public static final String CSRF_HEADER = "X-CSRF-Token";

    private static final SecureRandom RANDOM = new SecureRandom();

    private SessionUtil() {
    }

    /** Starts a fresh authenticated session for {@code user}. */
    public static void login(HttpServletRequest req, User user) {
        HttpSession old = req.getSession(false);
        if (old != null) {
            old.invalidate();               // drop any pre-login session (session fixation defence)
        }
        HttpSession session = req.getSession(true);
        session.setAttribute(USER_ID, user.getUserId());
        session.setAttribute(FULL_NAME, user.getFullName());
        session.setAttribute(USERNAME, user.getUsername());
        session.setAttribute(CSRF_TOKEN, newToken());
    }

    /** Invalidates the current session, if any. */
    public static void logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /** @return the logged-in user's id, or null when not authenticated */
    public static Long getUserId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        Object id = session.getAttribute(USER_ID);
        return (id instanceof Long) ? (Long) id : null;
    }

    public static boolean isLoggedIn(HttpServletRequest req) {
        return getUserId(req) != null;
    }

    /** @return a session attribute as String, or null */
    public static String getString(HttpServletRequest req, String name) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(name);
        return value == null ? null : value.toString();
    }

    /** Constant-time comparison of the X-CSRF-Token header with the session token. */
    public static boolean isValidCsrf(HttpServletRequest req) {
        String expected = getString(req, CSRF_TOKEN);
        String actual = req.getHeader(CSRF_HEADER);
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
