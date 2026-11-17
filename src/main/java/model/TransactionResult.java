package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Outcome of a deposit or withdrawal, returned to the browser for the success popup. */
public class TransactionResult {

    private final long transactionId;
    private final String transactionType;
    private final String accountNumber;
    private final BigDecimal previousBalance;
    private final BigDecimal amount;
    private final BigDecimal newBalance;
    private final String description;
    private final LocalDateTime timestamp;

    public TransactionResult(long transactionId, String transactionType, String accountNumber,
                             BigDecimal previousBalance, BigDecimal amount, BigDecimal newBalance,
                             String description) {
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.accountNumber = accountNumber;
        this.previousBalance = previousBalance;
        this.amount = amount;
        this.newBalance = newBalance;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    public long getTransactionId() { return transactionId; }
    public String getTransactionType() { return transactionType; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getPreviousBalance() { return previousBalance; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getNewBalance() { return newBalance; }
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
