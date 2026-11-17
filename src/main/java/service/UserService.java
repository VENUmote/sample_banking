package service;

import dao.BankAccountDAO;
import dao.UserDAO;
import model.BankAccount;
import model.RegistrationResult;
import model.User;
import util.AccountNumberGenerator;
import util.DBConnection;
import util.PasswordUtil;
import util.ValidationException;
import util.ValidationUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Business logic for registration, login and password reset.
 * Owns database transactions; DAOs only run SQL.
 */
public class UserService {

    private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 10;

    private final UserDAO userDAO = new UserDAO();
    private final BankAccountDAO accountDAO = new BankAccountDAO();

    /**
     * Registers a customer and opens a SAVINGS account with balance 0.00 - atomically.
     * If anything fails, neither the user nor the account is saved.
     *
     * @throws ValidationException for invalid input or duplicate email/username
     */
    public RegistrationResult register(String fullName, String email, String mobile, String username,
                                       String password, String confirmPassword, String address)
            throws ValidationException, SQLException {

        fullName = ValidationUtil.clean(fullName).replaceAll("\\s+", " ");
        email = ValidationUtil.clean(email).toLowerCase();
        mobile = ValidationUtil.clean(mobile);
        username = ValidationUtil.clean(username);
        address = ValidationUtil.clean(address);

        Map<String, String> errors = ValidationUtil.validateRegistration(
                fullName, email, mobile, username, password, confirmPassword, address);
        if (!errors.isEmpty()) {
            throw new ValidationException("Please correct the highlighted fields.", errors);
        }

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                Map<String, String> dup = new LinkedHashMap<>();
                if (userDAO.existsByEmail(con, email)) {
                    dup.put("email", "This email is already registered.");
                }
                if (userDAO.existsByUsername(con, username)) {
                    dup.put("username", "This username is already taken.");
                }
                if (!dup.isEmpty()) {
                    throw new ValidationException("Email or username already exists.", dup);
                }

                User user = new User(fullName, email, mobile, username, PasswordUtil.hash(password), address);
                userDAO.insert(con, user);

                BankAccount account = new BankAccount(user.getUserId(), uniqueAccountNumber(con), "SAVINGS");
                accountDAO.insert(con, account);

                con.commit();
                return new RegistrationResult(user, account);
            } catch (SQLIntegrityConstraintViolationException e) {
                // Race condition: another request registered the same email/username first.
                DBConnection.rollbackQuietly(con);
                throw new ValidationException("Email or username already exists.",
                        Map.of("username", "This username or email was just taken. Please try another."));
            } catch (ValidationException | SQLException | RuntimeException e) {
                DBConnection.rollbackQuietly(con);
                throw e;
            }
        }
    }

    /** Generates account numbers until one is not yet used. */
    private String uniqueAccountNumber(Connection con) throws SQLException {
        for (int i = 0; i < MAX_ACCOUNT_NUMBER_ATTEMPTS; i++) {
            String candidate = AccountNumberGenerator.generate();
            if (!accountDAO.existsByAccountNumber(con, candidate)) {
                return candidate;
            }
        }
        throw new SQLException("Could not generate a unique account number.");
    }

    /**
     * Verifies login credentials.
     *
     * @param identifier username or email
     * @return the user when the BCrypt hash matches; null otherwise
     */
    public User authenticate(String identifier, String password) throws ValidationException, SQLException {
        identifier = ValidationUtil.clean(identifier);
        Map<String, String> errors = new LinkedHashMap<>();
        if (identifier.isEmpty()) {
            errors.put("identifier", "Username or email is required.");
        }
        if (ValidationUtil.isBlank(password)) {
            errors.put("password", "Password is required.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Please enter your login details.", errors);
        }
        if (identifier.length() > 150 || password.length() > 128) {
            return null;
        }

        try (Connection con = DBConnection.getConnection()) {
            User user = userDAO.findByUsernameOrEmail(con, identifier);
            if (user == null) {
                PasswordUtil.simulateVerify(password);   // equal timing for unknown users
                return null;
            }
            if (!PasswordUtil.verify(password, user.getPasswordHash())) {
                return null;
            }
            if (!"ACTIVE".equals(user.getStatus())) {
                throw new ValidationException("This account is locked. Please contact the bank.");
            }
            return user;
        }
    }

    /**
     * Resets a forgotten password after verifying username + registered email + mobile.
     */
    public void resetPassword(String username, String email, String mobile,
                              String newPassword, String confirmPassword)
            throws ValidationException, SQLException {

        username = ValidationUtil.clean(username);
        email = ValidationUtil.clean(email).toLowerCase();
        mobile = ValidationUtil.clean(mobile);

        Map<String, String> errors = new LinkedHashMap<>();
        if (username.isEmpty()) {
            errors.put("username", "Username is required.");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            errors.put("email", "Enter your registered email address.");
        }
        if (!ValidationUtil.isValidMobile(mobile)) {
            errors.put("mobile", "Enter your registered 10-digit mobile number.");
        }
        String pwdError = ValidationUtil.passwordError(newPassword);
        if (pwdError != null) {
            errors.put("newPassword", pwdError);
        }
        if (newPassword != null && !newPassword.equals(confirmPassword)) {
            errors.put("confirmPassword", "Passwords do not match.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Please correct the highlighted fields.", errors);
        }

        try (Connection con = DBConnection.getConnection()) {
            User user = userDAO.findForPasswordReset(con, username, email, mobile);
            if (user == null) {
                // Deliberately vague: do not reveal which of the three values was wrong.
                throw new ValidationException("The details you entered do not match our records.");
            }
            userDAO.updatePassword(con, user.getUserId(), PasswordUtil.hash(newPassword));
        }
    }
}
