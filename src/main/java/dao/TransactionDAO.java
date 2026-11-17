package dao;

import model.Transaction;
import model.TransactionFilter;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Data Access Object for the {@code transactions} ledger table. */
public class TransactionDAO {

    private static final String SELECT =
            "SELECT transaction_id, account_id, transaction_type, amount, balance_after, description, "
                    + "status, transaction_date FROM transactions";

    /**
     * Records a ledger entry.
     *
     * @return the generated transaction_id
     */
    public long insert(Connection con, Transaction t) throws SQLException {
        String sql = "INSERT INTO transactions "
                + "(account_id, transaction_type, amount, balance_after, description, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, t.getAccountId());
            ps.setString(2, t.getTransactionType());
            ps.setBigDecimal(3, t.getAmount());
            ps.setBigDecimal(4, t.getBalanceAfter());
            ps.setString(5, t.getDescription());
            ps.setString(6, t.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    t.setTransactionId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Recording transaction failed: no generated key returned.");
    }

    /**
     * Total Credit / Total Debit = SUM(amount) of SUCCESSFUL transactions of the given type.
     *
     * @return the sum, or 0.00 when there are none
     */
    public BigDecimal sumByType(Connection con, long accountId, String type) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM transactions "
                + "WHERE account_id = ? AND transaction_type = ? AND status = 'SUCCESS'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1).setScale(2);
            }
        }
    }

    /** @return number of successful transactions on the account */
    public int countSuccessful(Connection con, long accountId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM transactions WHERE account_id = ? AND status = 'SUCCESS'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** @return the latest {@code limit} transactions, newest first */
    public List<Transaction> findRecent(Connection con, long accountId, int limit) throws SQLException {
        String sql = SELECT + " WHERE account_id = ? ORDER BY transaction_date DESC, transaction_id DESC LIMIT ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setInt(2, limit);
            return mapAll(ps);
        }
    }

    /**
     * Dynamic search for the history page. Only the WHERE clauses for supplied filters are
     * added, but every value is still bound with a '?' placeholder. The sort direction is
     * chosen from a fixed whitelist (ASC/DESC), never from raw user input.
     */
    public List<Transaction> search(Connection con, long accountId, TransactionFilter f) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE account_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(accountId);

        if (f.getType() != null) {
            sql.append(" AND transaction_type = ?");
            params.add(f.getType());
        }
        if (f.getFromDate() != null) {
            sql.append(" AND transaction_date >= ?");
            params.add(Timestamp.valueOf(f.getFromDate().atStartOfDay()));
        }
        if (f.getToDate() != null) {
            sql.append(" AND transaction_date < ?");                         // < next day = inclusive end date
            params.add(Timestamp.valueOf(f.getToDate().plusDays(1).atStartOfDay()));
        }
        if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
            sql.append(" AND (description LIKE ? OR CAST(transaction_id AS CHAR) = ?)");
            params.add("%" + escapeLike(f.getKeyword()) + "%");
            params.add(f.getKeyword());
        }
        String direction = f.isAscending() ? "ASC" : "DESC";
        sql.append(" ORDER BY transaction_date ").append(direction)
           .append(", transaction_id ").append(direction)
           .append(" LIMIT ?");
        params.add(f.getLimit());

        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return mapAll(ps);
        }
    }

    /** Escapes LIKE wildcards so a search for "50%" matches the literal text. */
    private static String escapeLike(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private List<Transaction> mapAll(PreparedStatement ps) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    private Transaction map(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTransactionId(rs.getLong("transaction_id"));
        t.setAccountId(rs.getLong("account_id"));
        t.setTransactionType(rs.getString("transaction_type"));
        t.setAmount(rs.getBigDecimal("amount"));
        t.setBalanceAfter(rs.getBigDecimal("balance_after"));
        t.setDescription(rs.getString("description"));
        t.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("transaction_date");
        t.setTransactionDate(ts == null ? null : ts.toLocalDateTime());
        t.deriveCreditDebit();
        return t;
    }
}
