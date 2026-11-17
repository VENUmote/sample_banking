package model;

/** Returned after a successful signup: the new user plus the auto-created bank account. */
public class RegistrationResult {

    private final User user;
    private final BankAccount account;

    public RegistrationResult(User user, BankAccount account) {
        this.user = user;
        this.account = account;
    }

    public User getUser() { return user; }
    public BankAccount getAccount() { return account; }
}
