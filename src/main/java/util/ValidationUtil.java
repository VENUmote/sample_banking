package util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Server-side validation rules. The JavaScript frontend repeats these rules for a
 * friendlier experience, but the server never trusts the browser - every request is
 * re-validated here.
 */
public final class ValidationUtil {

    private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]{1,99}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    /** Indian 10-digit mobile number starting with 6-9. */
    private static final Pattern MOBILE = Pattern.compile("^[6-9]\\d{9}$");
    /** 4-30 chars, starts with a letter, letters/digits/underscore only. */
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{3,29}$");
    /** 8-64 chars with upper, lower, digit and special character. (BCrypt uses max 72 bytes.) */
    private static final Pattern PASSWORD =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$");
    private static final Pattern ACCOUNT_NUMBER = Pattern.compile("^\\d{12}$");
    private static final Pattern AMOUNT_FORMAT = Pattern.compile("^\\d{1,13}(\\.\\d{1,2})?$");

    /** Maximum amount allowed in a single deposit or withdrawal. */
    public static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("1000000.00");
    public static final int MAX_DESCRIPTION_LENGTH = 255;

    private ValidationUtil() {
    }

    /** Null-safe trim: returns "" for null. */
    public static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Validates every signup field and returns all problems at once.
     *
     * @return field -> message map; empty when the input is valid
     */
    public static Map<String, String> validateRegistration(String fullName, String email, String mobile,
                                                           String username, String password,
                                                           String confirmPassword, String address) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (isBlank(fullName)) {
            errors.put("fullName", "Full name is required.");
        } else if (!NAME.matcher(fullName).matches()) {
            errors.put("fullName", "Name must be 2-100 letters (spaces, . ' - allowed).");
        }

        if (isBlank(email)) {
            errors.put("email", "Email address is required.");
        } else if (email.length() > 150 || !EMAIL.matcher(email).matches()) {
            errors.put("email", "Enter a valid email address.");
        }

        if (isBlank(mobile)) {
            errors.put("mobile", "Mobile number is required.");
        } else if (!MOBILE.matcher(mobile).matches()) {
            errors.put("mobile", "Enter a valid 10-digit mobile number starting with 6-9.");
        }

        if (isBlank(username)) {
            errors.put("username", "Username is required.");
        } else if (!USERNAME.matcher(username).matches()) {
            errors.put("username", "Username must be 4-30 characters, start with a letter, and use only letters, digits or _.");
        }

        String passwordError = passwordError(password);
        if (passwordError != null) {
            errors.put("password", passwordError);
        }

        if (isBlank(confirmPassword)) {
            errors.put("confirmPassword", "Please confirm your password.");
        } else if (password != null && !password.equals(confirmPassword)) {
            errors.put("confirmPassword", "Passwords do not match.");
        }

        if (isBlank(address)) {
            errors.put("address", "Address is required.");
        } else if (address.length() < 5 || address.length() > 255) {
            errors.put("address", "Address must be 5-255 characters.");
        }
        return errors;
    }

    /** @return an error message, or null when the password meets the strength policy */
    public static String passwordError(String password) {
        if (isBlank(password)) {
            return "Password is required.";
        }
        if (!PASSWORD.matcher(password).matches()) {
            return "Password must be 8-64 characters with uppercase, lowercase, a digit and a special character.";
        }
        return null;
    }

    public static boolean isValidEmail(String email) {
        return !isBlank(email) && EMAIL.matcher(email).matches();
    }

    public static boolean isValidMobile(String mobile) {
        return !isBlank(mobile) && MOBILE.matcher(mobile).matches();
    }

    /**
     * Validates a 12-digit account number.
     *
     * @return the trimmed account number
     */
    public static String validateAccountNumber(String raw) throws ValidationException {
        String value = clean(raw);
        if (!ACCOUNT_NUMBER.matcher(value).matches()) {
            throw new ValidationException("Invalid account number.",
                    Map.of("accountNumber", "Account number must be exactly 12 digits."));
        }
        return value;
    }

    /**
     * Parses and validates a money amount: positive, max 2 decimal places, within the limit.
     * Uses BigDecimal - never double - so there are no floating-point rounding errors.
     */
    public static BigDecimal parseAmount(String raw) throws ValidationException {
        String value = clean(raw).replace(",", "");
        if (value.isEmpty()) {
            throw new ValidationException("Amount is required.", Map.of("amount", "Amount is required."));
        }
        if (!AMOUNT_FORMAT.matcher(value).matches()) {
            throw new ValidationException("Invalid amount.",
                    Map.of("amount", "Enter a positive amount with up to 2 decimal places."));
        }
        BigDecimal amount = new BigDecimal(value).setScale(2);
        if (amount.signum() <= 0) {
            throw new ValidationException("Amount must be greater than zero.",
                    Map.of("amount", "Amount must be greater than ₹0.00."));
        }
        if (amount.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new ValidationException("Amount exceeds the per-transaction limit.",
                    Map.of("amount", "Maximum per transaction is ₹10,00,000.00."));
        }
        return amount;
    }

    /**
     * Validates an optional description. Control characters are stripped.
     *
     * @param defaultValue used when the description is blank
     */
    public static String validateDescription(String raw, String defaultValue) throws ValidationException {
        String value = clean(raw).replaceAll("\\p{Cntrl}", " ");
        if (value.isEmpty()) {
            return defaultValue;
        }
        if (value.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("Description is too long.",
                    Map.of("description", "Description must be at most 255 characters."));
        }
        return value;
    }

    /**
     * Parses an ISO date (yyyy-MM-dd) used by the history filters.
     *
     * @return the date, or null when blank
     */
    public static LocalDate parseOptionalDate(String raw, String field) throws ValidationException {
        String value = clean(raw);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date.", Map.of(field, "Use the format YYYY-MM-DD."));
        }
    }
}
