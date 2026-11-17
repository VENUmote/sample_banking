package dao;

import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * Data Access Object for the {@code users} table.
 *
 * <p>Every method receives a {@link Connection} from the service layer so several DAO
 * calls can run inside one database transaction. All SQL uses PreparedStatement
 * placeholders - user input is never concatenated into SQL (prevents SQL injection).</p>
 */
public class UserDAO {

    private static final String COLUMNS =
            "user_id, full_name, email, mobile, username, password_hash, address, status, created_at";

    /** @return true when a user with this email already exists */
    public boolean existsByEmail(Connection con, String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** @return true when this username is already taken (case-insensitive via column collation) */
    public boolean existsByUsername(Connection con, String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Inserts a new user.
     *
     * @return the generated user_id
     */
    public long insert(Connection con, User user) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, mobile, username, password_hash, address, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getMobile());
            ps.setString(4, user.getUsername());
            ps.setString(5, user.getPasswordHash());
            ps.setString(6, user.getAddress());
            ps.setString(7, user.getStatus() == null ? "ACTIVE" : user.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    user.setUserId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Creating user failed: no generated key returned.");
    }

    /** Finds a user for login - the identifier can be a username or an email. */
    public User findByUsernameOrEmail(Connection con, String identifier) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE username = ? OR email = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, identifier);
            ps.setString(2, identifier.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** @return the user with this id, or null */
    public User findById(Connection con, long userId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Identity check for Forgot Password: username, email and mobile must all match. */
    public User findForPasswordReset(Connection con, String username, String email, String mobile)
            throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE username = ? AND email = ? AND mobile = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, mobile);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Replaces a user's password hash. */
    public void updatePassword(Connection con, long userId, String newHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    /** Converts the current ResultSet row into a User object. */
    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getLong("user_id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setMobile(rs.getString("mobile"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setAddress(rs.getString("address"));
        u.setStatus(rs.getString("status"));
        Timestamp created = rs.getTimestamp("created_at");
        u.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return u;
    }
}
