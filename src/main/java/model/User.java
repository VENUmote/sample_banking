package model;

import java.time.LocalDateTime;

/** A registered customer - maps to one row of the {@code users} table. */
public class User {

    private long userId;
    private String fullName;
    private String email;
    private String mobile;
    private String username;
    /** transient: never serialised to JSON, so the hash can never leak to the browser. */
    private transient String passwordHash;
    private String address;
    private String status;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(String fullName, String email, String mobile, String username,
                String passwordHash, String address) {
        this.fullName = fullName;
        this.email = email;
        this.mobile = mobile;
        this.username = username;
        this.passwordHash = passwordHash;
        this.address = address;
        this.status = "ACTIVE";
    }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** @return the first word of the full name, used in "Welcome back, Venu!" */
    public String getFirstName() {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        return fullName.trim().split("\\s+")[0];
    }
}
