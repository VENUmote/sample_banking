package model;

import java.time.LocalDate;

/**
 * Search criteria for the transaction history page. Every field is optional;
 * the DAO only adds a WHERE clause for the fields that are set.
 */
public class TransactionFilter {

    public static final int MAX_ROWS = 500;

    private String type;          // CREDIT, DEBIT or null for both
    private LocalDate fromDate;   // inclusive
    private LocalDate toDate;     // inclusive
    private String keyword;       // matches description or transaction id
    private boolean ascending;    // false = latest first (default)
    private int limit = MAX_ROWS;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public boolean isAscending() { return ascending; }
    public void setAscending(boolean ascending) { this.ascending = ascending; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = Math.max(1, Math.min(limit, MAX_ROWS)); }
}
