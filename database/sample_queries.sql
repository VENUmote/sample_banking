-- =====================================================================
--  Sample / diagnostic SQL queries for the Net Banking database.
--  These mirror the PreparedStatement queries used in the DAO classes
--  ('?' placeholders replaced with example values).
-- =====================================================================
USE netbanking_db;

-- 1. List all customers with their account and balance
SELECT u.user_id, u.full_name, u.username, u.email, a.account_number, a.account_type, a.balance
FROM users u
JOIN bank_accounts a ON a.user_id = u.user_id
ORDER BY u.user_id;

-- 2. Check whether an email / username is already registered (UserDAO.existsByEmail / existsByUsername)
SELECT 1 FROM users WHERE email = 'venu@example.com' LIMIT 1;
SELECT 1 FROM users WHERE username = 'venu' LIMIT 1;

-- 3. Login lookup by username OR email (UserDAO.findByUsernameOrEmail)
SELECT user_id, full_name, email, mobile, username, password_hash, address, status, created_at
FROM users WHERE username = 'venu' OR email = 'venu';

-- 4. Lock the account row before changing its balance (BankAccountDAO.findByAccountNumberForUpdate)
--    Must be run inside a transaction:
START TRANSACTION;
SELECT account_id, user_id, account_number, account_type, balance, status, created_at
FROM bank_accounts WHERE account_number = '501012345678' FOR UPDATE;
-- UPDATE bank_accounts SET balance = 5000.00 WHERE account_id = 1;
-- INSERT INTO transactions (account_id, transaction_type, amount, balance_after, description, status)
--   VALUES (1, 'CREDIT', 5000.00, 5000.00, 'Salary', 'SUCCESS');
ROLLBACK;  -- change to COMMIT to persist

-- 5. Total credit / total debit (TransactionDAO.sumByType)
SELECT COALESCE(SUM(amount), 0) AS total_credit
FROM transactions WHERE account_id = 1 AND transaction_type = 'CREDIT' AND status = 'SUCCESS';

SELECT COALESCE(SUM(amount), 0) AS total_debit
FROM transactions WHERE account_id = 1 AND transaction_type = 'DEBIT' AND status = 'SUCCESS';

-- 6. Reconciliation check: stored balance must equal credits - debits (should return 0 rows)
SELECT a.account_number, a.balance AS stored_balance,
       COALESCE(SUM(CASE WHEN t.transaction_type = 'CREDIT' THEN t.amount END), 0)
     - COALESCE(SUM(CASE WHEN t.transaction_type = 'DEBIT'  THEN t.amount END), 0) AS computed_balance
FROM bank_accounts a
LEFT JOIN transactions t ON t.account_id = a.account_id AND t.status = 'SUCCESS'
GROUP BY a.account_id, a.account_number, a.balance
HAVING stored_balance <> computed_balance;

-- 7. Recent 5 transactions (TransactionDAO.findRecent)
SELECT transaction_id, account_id, transaction_type, amount, balance_after, description, status, transaction_date
FROM transactions WHERE account_id = 1
ORDER BY transaction_date DESC, transaction_id DESC LIMIT 5;

-- 8. Filtered history: DEBIT only, within a date range, description contains 'rent' (TransactionDAO.search)
SELECT transaction_id, account_id, transaction_type, amount, balance_after, description, status, transaction_date
FROM transactions
WHERE account_id = 1
  AND transaction_type = 'DEBIT'
  AND transaction_date >= '2026-11-01 00:00:00'
  AND transaction_date <  '2026-12-01 00:00:00'
  AND (description LIKE '%rent%' OR CAST(transaction_id AS CHAR) = 'rent')
ORDER BY transaction_date DESC, transaction_id DESC
LIMIT 500;

-- 9. Daily credit/debit summary for an account
SELECT DATE(transaction_date) AS txn_day,
       SUM(CASE WHEN transaction_type = 'CREDIT' THEN amount ELSE 0 END) AS credited,
       SUM(CASE WHEN transaction_type = 'DEBIT'  THEN amount ELSE 0 END) AS debited,
       COUNT(*) AS txn_count
FROM transactions
WHERE account_id = 1 AND status = 'SUCCESS'
GROUP BY DATE(transaction_date)
ORDER BY txn_day DESC;

-- 10. Failed (insufficient balance) withdrawal attempts
SELECT transaction_id, amount, balance_after, description, transaction_date
FROM transactions WHERE status = 'FAILED' ORDER BY transaction_date DESC;
