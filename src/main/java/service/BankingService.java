package service;

import dao.BankAccountDAO;
import dao.TransactionDAO;
import dao.UserDAO;
import model.BankAccount;
import model.DashboardSummary;
import model.Transaction;
import model.TransactionFilter;
import model.TransactionResult;
import model.User;
import util.DBConnection;
import util.ValidationException;
import util.ValidationUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Core banking logic: dashboard figures, deposits, withdrawals and history.
 *
 * <p><b>Consistency rules</b></p>
 * <ul>
 *   <li>Each deposit/withdrawal runs in ONE database transaction: lock row, compute new
 *       balance, update balance, insert ledger entry, commit. Any error rolls everything back.</li>
 *   <li>{@code SELECT ... FOR UPDATE} serialises concurrent operations on the same account.</li>
 *   <li>Current Balance = Total Credit - Total Debit (successful transactions only); the dashboard
 *       re-computes this and reports whether it matches the stored balance.</li>
 * </ul>
 */
public class BankingService {

    private static final Logger LOG = Logger.getLogger(BankingService.class.getName());
    private static final int RECENT_TRANSACTIONS = 5;

    private final UserDAO userDAO = new UserDAO();
    private final BankAccountDAO accountDAO = new BankAccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    /** @return the account of the logged-in user, or null */
    public BankAccount getAccountForUser(long userId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            return accountDAO.findByUserId(con, userId);
        }
    }

    /**
     * Builds all dashboard numbers from one consistent snapshot (REPEATABLE READ, read-only).
     */
    public DashboardSummary getDashboard(long userId) throws SQLException, AccessDeniedException {
        try (Connection con = DBConnection.getConnection()) {
            con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            con.setAutoCommit(false);
            con.setReadOnly(true);
            try {
                User user = userDAO.findById(con, userId);
                BankAccount account = accountDAO.findByUserId(con, userId);
                if (user == null || account == null) {
                    throw new AccessDeniedException("No bank account found for this user.");
                }
                long accountId = account.getAccountId();

                BigDecimal totalCredit = transactionDAO.sumByType(con, accountId, Transaction.CREDIT);
                BigDecimal totalDebit = transactionDAO.sumByType(con, accountId, Transaction.DEBIT);
                BigDecimal computedBalance = totalCredit.subtract(totalDebit);
                boolean verified = computedBalance.compareTo(account.getBalance()) == 0;
                if (!verified) {
                    LOG.severe("Balance mismatch on account " + account.getAccountNumber()
                            + ": stored=" + account.getBalance() + " computed=" + computedBalance);
                }

                DashboardSummary s = new DashboardSummary();
                s.setCustomerName(user.getFullName());
                s.setUsername(user.getUsername());
                s.setAccountNumber(account.getAccountNumber());
                s.setAccountType(account.getAccountType());
                s.setCurrentBalance(account.getBalance());
                s.setTotalCredit(totalCredit);
                s.setTotalDebit(totalDebit);
                s.setTotalTransactions(transactionDAO.countSuccessful(con, accountId));
                s.setBalanceVerified(verified);
                s.setRecentTransactions(transactionDAO.findRecent(con, accountId, RECENT_TRANSACTIONS));
                con.commit();
                return s;
            } catch (SQLException | AccessDeniedException | RuntimeException e) {
                DBConnection.rollbackQuietly(con);
                throw e;
            }
        }
    }

    /** Credits money: New Balance = Previous Balance + Amount. */
    public TransactionResult deposit(long userId, String accountNumber, String amount, String description)
            throws ValidationException, AccessDeniedException, SQLException {
        try {
            return execute(userId, accountNumber, amount, description, Transaction.CREDIT);
        } catch (InsufficientBalanceException e) {
            throw new IllegalStateException("A deposit can never be short of funds", e);
        }
    }

    /** Debits money: Remaining Balance = Previous Balance - Amount (never below zero). */
    public TransactionResult withdraw(long userId, String accountNumber, String amount, String description)
            throws ValidationException, AccessDeniedException, InsufficientBalanceException, SQLException {
        return execute(userId, accountNumber, amount, description, Transaction.DEBIT);
    }

    /**
     * Shared deposit/withdrawal workflow executed inside one ACID transaction.
     */
    private TransactionResult execute(long userId, String rawAccountNumber, String rawAmount,
                                      String rawDescription, String type)
            throws ValidationException, AccessDeniedException, InsufficientBalanceException, SQLException {

        // 1. Server-side validation (before touching the database)
        String accountNumber = ValidationUtil.validateAccountNumber(rawAccountNumber);
        BigDecimal amount = ValidationUtil.parseAmount(rawAmount);
        String description = ValidationUtil.validateDescription(rawDescription,
                Transaction.CREDIT.equals(type) ? "Cash Deposit" : "Cash Withdrawal");

        try (Connection con = DBConnection.getConnection()) {
            con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            con.setAutoCommit(false);
            try {
                // 2. Lock the account row so no concurrent request can change it
                BankAccount account = accountDAO.findByAccountNumberForUpdate(con, accountNumber);

                // 3. Authorisation: the account must belong to the logged-in user
                if (account == null || account.getUserId() != userId) {
                    throw new AccessDeniedException("You are not authorised to operate on this account.");
                }
                if (!account.isActive()) {
                    throw new ValidationException("This account is " + account.getStatus().toLowerCase(Locale.ROOT)
                            + " and cannot be used for transactions.");
                }

                BigDecimal previous = account.getBalance();
                BigDecimal newBalance;

                if (Transaction.DEBIT.equals(type)) {
                    // 4a. Prevent negative balances
                    if (amount.compareTo(previous) > 0) {
                        // Keep an audit trail of the refused attempt; balance is unchanged.
                        transactionDAO.insert(con, new Transaction(account.getAccountId(), type, amount,
                                previous, description + " (declined: insufficient balance)", Transaction.FAILED));
                        con.commit();
                        throw new InsufficientBalanceException(previous, amount);
                    }
                    newBalance = previous.subtract(amount);
                } else {
                    newBalance = previous.add(amount);
                }

                // 4b. Update balance and write the ledger entry - both or neither
                accountDAO.updateBalance(con, account.getAccountId(), newBalance);
                long txnId = transactionDAO.insert(con, new Transaction(account.getAccountId(), type, amount,
                        newBalance, description, Transaction.SUCCESS));

                con.commit();
                return new TransactionResult(txnId, type, accountNumber, previous, amount, newBalance, description);

            } catch (InsufficientBalanceException e) {
                throw e;                                   // already committed the audit row
            } catch (ValidationException | AccessDeniedException | SQLException | RuntimeException e) {
                DBConnection.rollbackQuietly(con);
                throw e;
            }
        }
    }

    /**
     * Transaction history for the logged-in user's own account only.
     */
    public List<Transaction> getHistory(long userId, TransactionFilter filter)
            throws SQLException, AccessDeniedException {
        try (Connection con = DBConnection.getConnection()) {
            BankAccount account = accountDAO.findByUserId(con, userId);
            if (account == null) {
                throw new AccessDeniedException("No bank account found for this user.");
            }
            return transactionDAO.search(con, account.getAccountId(), filter);
        }
    }
}
