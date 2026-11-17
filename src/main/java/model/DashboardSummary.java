package model;

import java.math.BigDecimal;
import java.util.List;

/** Everything the dashboard page shows, assembled by BankingService in one consistent read. */
public class DashboardSummary {

    private String customerName;
    private String username;
    private String accountNumber;
    private String accountType;
    private BigDecimal currentBalance;
    private BigDecimal totalCredit;
    private BigDecimal totalDebit;
    private int totalTransactions;
    /** true when stored balance == totalCredit - totalDebit (reconciliation check). */
    private boolean balanceVerified;
    private List<Transaction> recentTransactions;

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }

    public BigDecimal getTotalCredit() { return totalCredit; }
    public void setTotalCredit(BigDecimal totalCredit) { this.totalCredit = totalCredit; }

    public BigDecimal getTotalDebit() { return totalDebit; }
    public void setTotalDebit(BigDecimal totalDebit) { this.totalDebit = totalDebit; }

    public int getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }

    public boolean isBalanceVerified() { return balanceVerified; }
    public void setBalanceVerified(boolean balanceVerified) { this.balanceVerified = balanceVerified; }

    public List<Transaction> getRecentTransactions() { return recentTransactions; }
    public void setRecentTransactions(List<Transaction> recentTransactions) { this.recentTransactions = recentTransactions; }
}
