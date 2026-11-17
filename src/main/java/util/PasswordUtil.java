package util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt password hashing helper.
 *
 * <p>BCrypt embeds a random salt and the cost factor inside the 60-character hash, so the
 * same password never produces the same hash twice and plain-text passwords are never stored.</p>
 */
public final class PasswordUtil {

    /** Cost factor 12 = 2^12 hashing rounds (~250 ms) - slow enough to resist brute force. */
    private static final int COST = 12;

    /** A valid hash used to keep login timing constant when the username does not exist. */
    private static final String DUMMY_HASH = BCrypt.hashpw("dummy-password-for-timing", BCrypt.gensalt(COST));

    private PasswordUtil() {
    }

    /** Hashes a plain-text password with a fresh random salt. */
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(COST));
    }

    /**
     * Verifies a plain-text password against a stored BCrypt hash.
     *
     * @return true only when the password matches; false for null/malformed hashes
     */
    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || !storedHash.startsWith("$2")) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Performs a throw-away BCrypt check so that "unknown user" and "wrong password"
     * take the same time, preventing username enumeration via response timing.
     */
    public static void simulateVerify(String plainPassword) {
        verify(plainPassword == null ? "" : plainPassword, DUMMY_HASH);
    }
}
