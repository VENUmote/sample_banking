-- =====================================================================
--  Net Banking Management System - MySQL 8.0+ schema
--  Run in MySQL Workbench or:  mysql -u root -p < database/schema.sql
--  WARNING: this script DROPS and recreates the three tables.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS netbanking_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE netbanking_db;

-- Drop in reverse dependency order (child tables first)
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS bank_accounts;
DROP TABLE IF EXISTS users;

-- ---------------------------------------------------------------------
-- 1. users : customer profile + BCrypt password hash
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id        BIGINT        NOT NULL AUTO_INCREMENT,
    full_name      VARCHAR(100)  NOT NULL,
    email          VARCHAR(150)  NOT NULL,
    mobile         VARCHAR(15)   NOT NULL,
    username       VARCHAR(30)   NOT NULL,
    password_hash  CHAR(60)      NOT NULL,           -- BCrypt hashes are always 60 chars
    address        VARCHAR(255)  NOT NULL,
    status         ENUM('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username),
    INDEX idx_users_mobile (mobile)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 2. bank_accounts : one account per user, balance kept in DECIMAL(15,2)
-- ---------------------------------------------------------------------
CREATE TABLE bank_accounts (
    account_id      BIGINT        NOT NULL AUTO_INCREMENT,
    user_id         BIGINT        NOT NULL,
    account_number  VARCHAR(20)   NOT NULL,
    account_type    ENUM('SAVINGS','CURRENT') NOT NULL DEFAULT 'SAVINGS',
    balance         DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status          ENUM('ACTIVE','FROZEN','CLOSED') NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_bank_accounts PRIMARY KEY (account_id),
    CONSTRAINT uq_account_number UNIQUE (account_number),
    CONSTRAINT uq_accounts_user UNIQUE (user_id),
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 3. transactions : immutable ledger of every credit / debit attempt
-- ---------------------------------------------------------------------
CREATE TABLE transactions (
    transaction_id    BIGINT        NOT NULL AUTO_INCREMENT,
    account_id        BIGINT        NOT NULL,
    transaction_type  ENUM('CREDIT','DEBIT') NOT NULL,
    amount            DECIMAL(15,2) NOT NULL,
    balance_after     DECIMAL(15,2) NOT NULL,
    description       VARCHAR(255)  NULL,
    status            ENUM('SUCCESS','FAILED') NOT NULL DEFAULT 'SUCCESS',
    transaction_date  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_transactions PRIMARY KEY (transaction_id),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id)
        REFERENCES bank_accounts (account_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_balance_after_non_negative CHECK (balance_after >= 0),
    INDEX idx_txn_account_date (account_id, transaction_date),
    INDEX idx_txn_account_type_status (account_id, transaction_type, status)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Optional: a dedicated, least-privilege application user.
-- Uncomment, choose a strong password, and put it in db.properties.
-- ---------------------------------------------------------------------
-- CREATE USER IF NOT EXISTS 'netbank_app'@'localhost' IDENTIFIED BY 'ChangeMe@123';
-- GRANT SELECT, INSERT, UPDATE ON netbanking_db.* TO 'netbank_app'@'localhost';
-- FLUSH PRIVILEGES;
