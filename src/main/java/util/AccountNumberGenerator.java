package util;

import java.security.SecureRandom;

/**
 * Generates 12-digit bank account numbers of the form {@code 5010XXXXXXXX}.
 *
 * <p>SecureRandom makes numbers unpredictable. Uniqueness is guaranteed by the service
 * layer (it re-checks the database and retries) and by the UNIQUE constraint on
 * {@code bank_accounts.account_number}.</p>
 */
public final class AccountNumberGenerator {

    private static final String BANK_PREFIX = "5010";
    private static final SecureRandom RANDOM = new SecureRandom();

    private AccountNumberGenerator() {
    }

    /** @return a new random 12-digit account number */
    public static String generate() {
        StringBuilder sb = new StringBuilder(BANK_PREFIX);
        // First random digit is 1-9 so the number never looks like it has a leading zero block.
        sb.append(1 + RANDOM.nextInt(9));
        for (int i = 0; i < 7; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
