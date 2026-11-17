package dao;

import model.BankAccount;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/** Data Access Object for the {@code bank_accounts} table. */
public class BankAccountDAO {

    private static final String COLUMNS =
            "account_id, user_id, account_number, account_type, balance, status, created_at";

    /** @return true when the account number is already in use */
    public boolean existsByAccountNumber(Connection con, String accountNumber) throws SQLException {
        String sql = "SELECT 1 FROM bank_accounts WHERE account_number = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Creates a new account row (initial balance comes from the object, normally 0.00).
     *
     * @return the generated account_id
     */
    public long insert(Connection con, BankAccount account) throws SQLException {
        String sql = "INSERT INTO bank_accounts (user_id, account_number, account_type, balance, status) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, account.getUserId());
            ps.setString(2, account.getAccountNumber());
            ps.setString(3, account.getAccountType());
            ps.setBigDecimal(4, account.getBalance());
            ps.setString(5, account.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    account.setAccountId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Creating bank account failed: no generated key returned.");
    }

    /** @return the account owned by this user, or null */
    public BankAccount findByUserId(Connection con, long userId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM bank_accounts WHERE user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * Loads an account and takes an exclusive row lock ({@code SELECT ... FOR UPDATE}).
     * Must be called with auto-commit OFF. Any other deposit/withdrawal on the same account
     * waits until this transaction commits or rolls back, so two concurrent withdrawals can
     * never both spend the same money.
     */
    public BankAccount findByAccountNumberForUpdate(Connection con, String accountNumber) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM bank_accounts WHERE account_number = ? FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Writes the new balance. The CHECK (balance >= 0) constraint is a final safety net. */
    public void updateBalance(Connection con, long accountId, BigDecimal newBalance) throws SQLException {
        String sql = "UPDATE bank_accounts SET balance = ? WHERE account_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setLong(2, accountId);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Balance update affected no rows for account " + accountId);
            }
        }
    }

    private BankAccount map(ResultSet rs) throws SQLException {
        BankAccount a = new BankAccount();
        a.setAccountId(rs.getLong("account_id"));
        a.setUserId(rs.getLong("user_id"));
        a.setAccountNumber(rs.getString("account_number"));
        a.setAccountType(rs.getString("account_type"));
        a.setBalance(rs.getBigDecimal("balance"));
        a.setStatus(rs.getString("status"));
        Timestamp created = rs.getTimestamp("created_at");
        a.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return a;
    }
}
