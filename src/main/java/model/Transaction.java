package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One ledger entry - maps to one row of the {@code transactions} table. */
public class Transaction {

    public static final String CREDIT = "CREDIT";
    public static final String DEBIT = "DEBIT";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    private long transactionId;
    private long accountId;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private String status;
    private LocalDateTime transactionDate;

    // Convenience columns for the history table (credit OR debit is filled, never both)
    private BigDecimal creditAmount;
    private BigDecimal debitAmount;

    public Transaction() {
    }

    public Transaction(long accountId, String transactionType, BigDecimal amount,
                       BigDecimal balanceAfter, String description, String status) {
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.status = status;
    }

    /** Fills creditAmount / debitAmount from type + amount (called by the DAO after loading). */
    public void deriveCreditDebit() {
        this.creditAmount = CREDIT.equals(transactionType) ? amount : null;
        this.debitAmount = DEBIT.equals(transactionType) ? amount : null;
    }

    public long getTransactionId() { return transactionId; }
    public void setTransactionId(long transactionId) { this.transactionId = transactionId; }

    public long getAccountId() { return accountId; }
    public void setAccountId(long accountId) { this.accountId = accountId; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public BigDecimal getCreditAmount() { return creditAmount; }
    public BigDecimal getDebitAmount() { return debitAmount; }
}
