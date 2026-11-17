package service;

import java.math.BigDecimal;

/** Thrown when a withdrawal amount exceeds the available balance (HTTP 422). */
public class InsufficientBalanceException extends Exception {

    private static final long serialVersionUID = 1L;

    private final BigDecimal availableBalance;
    private final BigDecimal requestedAmount;

    public InsufficientBalanceException(BigDecimal availableBalance, BigDecimal requestedAmount) {
        super("Insufficient balance. Available balance is less than the requested withdrawal amount.");
        this.availableBalance = availableBalance;
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getAvailableBalance() { return availableBalance; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
}
