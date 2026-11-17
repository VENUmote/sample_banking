# Net Banking Management System (Java · Servlets · JDBC · MySQL)

A complete, responsive, database-driven net banking web application:

* Sign up (BCrypt-hashed password, auto-generated 12-digit account number, ₹0.00 opening balance)
* Sign in / sign out with session-based authentication, Forgot Password
* Dashboard: customer name, account number & type, current balance, total credited, total debited, total transactions, recent transactions
* Deposit (credit) and withdrawal (debit) with full ACID database transactions and row locking
* Transaction history with search, CREDIT/DEBIT filter, date-range filter and sorting
* Balance reconciliation: **Current Balance = Total Credit − Total Debit** checked on every dashboard load
* Blue & white responsive UI with navbar, sidebar, cards, popups and toast notifications

**Tech stack:** Java 17 · Jakarta Servlet 6.0 · JDBC · MySQL 8 · Maven · Apache Tomcat 10.1 · HTML5 · CSS3 · vanilla JavaScript · BCrypt (jBCrypt) · Gson

---

## Table of contents

1. [Prerequisites](#1-prerequisites)
2. [Step-by-step setup in VS Code (Windows)](#2-step-by-step-setup-in-vs-code-windows)
3. [Using the application](#3-using-the-application)
4. [Project structure – purpose of every folder](#4-project-structure--purpose-of-every-folder)
5. [Architecture & request flow](#5-architecture--request-flow)
6. [Java classes and methods explained](#6-java-classes-and-methods-explained)
7. [Database design and SQL queries explained](#7-database-design-and-sql-queries-explained)
8. [Frontend pages and scripts explained](#8-frontend-pages-and-scripts-explained)
9. [REST API reference](#9-rest-api-reference)
10. [Security measures](#10-security-measures)
11. [Balance calculation & consistency](#11-balance-calculation--consistency)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. Prerequisites

| Software | Version | Download |
|---|---|---|
| JDK | **17 or newer** (17 / 21 LTS) | https://adoptium.net |
| Apache Maven | 3.9+ | https://maven.apache.org/download.cgi |
| MySQL Server + MySQL Workbench | 8.0.16+ (CHECK constraints) | https://dev.mysql.com/downloads/installer/ |
| Apache Tomcat | **10.1.x** (NOT 9.x – this app uses `jakarta.servlet`) | https://tomcat.apache.org/download-10.cgi |
| Visual Studio Code | latest | https://code.visualstudio.com |

VS Code extensions (the project's `.vscode/extensions.json` will prompt you to install them):

* **Extension Pack for Java** (Microsoft) – Java language support, Maven, debugger
* **Community Server Connectors** (Red Hat) – start/stop Tomcat and deploy the WAR from VS Code
* *(optional)* **MySQL** by Weijan Chen – browse the database inside VS Code

---

## 2. Step-by-step setup in VS Code (Windows)

### Step 1 – Install the JDK and Maven and set environment variables

1. Install the JDK (e.g. `C:\Program Files\Eclipse Adoptium\jdk-17`).
2. Unzip Maven to e.g. `C:\apache-maven-3.9.9`.
3. Open **Start → "Edit the system environment variables" → Environment Variables** and add:
   * `JAVA_HOME` = `C:\Program Files\Eclipse Adoptium\jdk-17...`
   * `MAVEN_HOME` = `C:\apache-maven-3.9.9`
   * Append to `Path`: `%JAVA_HOME%\bin` and `%MAVEN_HOME%\bin`
4. Open a **new** Command Prompt and verify:
   ```bat
   java -version
   mvn -v
   ```

### Step 2 – Install MySQL and create the database

1. Install MySQL Server 8 with the MySQL Installer; note the **root password** you choose.
2. Open **MySQL Workbench → Local instance → File → Open SQL Script →** `database/schema.sql` → click ⚡ **Execute**.
   *Or* from a terminal in the project folder:
   ```bat
   mysql -u root -p < database\schema.sql
   ```
3. Confirm: `USE netbanking_db; SHOW TABLES;` → `bank_accounts`, `transactions`, `users`.

### Step 3 – Configure the JDBC connection

Edit **`src/main/resources/db.properties`**:

```properties
db.url=jdbc:mysql://localhost:3306/netbanking_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata&characterEncoding=UTF-8
db.user=root
db.password=YOUR_MYSQL_PASSWORD
```

> The file is compiled into `WEB-INF/classes`, which Tomcat never serves to browsers, so the
> credentials are not exposed to frontend code. You can instead set the environment variables
> `DB_URL`, `DB_USER`, `DB_PASSWORD`, which override the file.

### Step 4 – Install Tomcat 10.1

Download the **64-bit Windows zip** of Tomcat 10.1 and unzip to e.g. `C:\apache-tomcat-10.1.x`.
(Optionally set `CATALINA_HOME=C:\apache-tomcat-10.1.x`, used by `deploy.bat`.)

### Step 5 – Open the project in VS Code

1. Unzip `NetBanking.zip`, then **File → Open Folder… →** select the `NetBanking` folder.
2. Install the recommended extensions when prompted.
3. Wait for the Java extension to finish importing the Maven project (status bar shows "Java: Ready").

### Step 6 – Build the WAR

Open the VS Code terminal (**Ctrl + `**) and run:

```bat
mvn clean package
```

The first build downloads dependencies. Result: **`target\NetBanking.war`** → `BUILD SUCCESS`.

### Step 7 – Deploy and run (choose ONE option)

**Option A – Community Server Connectors (inside VS Code)**

1. In the Explorer sidebar open the **SERVERS** panel.
2. Right-click **Community Server Connector → Create New Server… → No, use server on disk** → choose your Tomcat 10.1 folder.
3. Right-click the Tomcat server → **Start Server**.
4. Right-click `target/NetBanking.war` in the Explorer → **Run on Server** → choose the Tomcat server.

**Option B – `deploy.bat` (command line)**

```bat
set CATALINA_HOME=C:\apache-tomcat-10.1.x
deploy.bat
```
It runs `mvn clean package`, copies the WAR into `%CATALINA_HOME%\webapps`, and starts Tomcat.

**Option C – manual**

Copy `target\NetBanking.war` into `C:\apache-tomcat-10.1.x\webapps\` and run `C:\apache-tomcat-10.1.x\bin\startup.bat`.

### Step 8 – Open the application

👉 **http://localhost:8080/NetBanking/**

After code changes: run `mvn clean package` again and redeploy (Option A: right-click server → *Publish Server (Full)*).

---

## 3. Using the application

1. **Create New Account** → fill the form → popup *"Welcome to Net Banking! Your account has been successfully created."* with your generated **account number**.
2. **Login** with your username *or* email → popup *"Welcome back, Venu! You have successfully logged into Net Banking."*
3. **Dashboard** shows balance **₹0.00**.
4. **Deposit** ₹5,000 → popup shows Previous ₹0.00 → Deposit ₹5,000.00 → New ₹5,000.00.
5. **Withdraw** ₹1,500 → Remaining ₹3,500.00. Try ₹10,000 → **Insufficient Balance** popup (the declined attempt is logged with status FAILED).
6. **Transaction History** → search, filter CREDIT/DEBIT, choose dates, change sort.
7. **Logout** (navbar or sidebar). Sessions also expire after 15 minutes of inactivity.

Password rule: 8–64 characters with an uppercase letter, a lowercase letter, a digit and a special character, e.g. `Venu@1234`.

---

## 4. Project structure – purpose of every folder

```
NetBanking/
├── pom.xml                         Maven build: dependencies, Java 17, WAR packaging
├── README.md                       This guide
├── deploy.bat                      Windows one-click build + deploy to Tomcat
├── .vscode/                        VS Code recommended extensions & settings
├── database/
│   ├── schema.sql                  Creates netbanking_db and the 3 tables
│   └── sample_queries.sql          Example / diagnostic queries (incl. reconciliation)
└── src/main/
    ├── java/
    │   ├── controller/             Web layer: Servlets (HTTP endpoints), Filters, Listener
    │   ├── dao/                    Data Access Objects: all SQL (PreparedStatement) lives here
    │   ├── model/                  Plain Java objects (POJOs) mirroring tables and API payloads
    │   ├── service/                Business logic, validation orchestration, DB transactions
    │   └── util/                   Reusable helpers: DB connection, BCrypt, validation, JSON, session
    ├── resources/
    │   └── db.properties           JDBC URL / user / password (server-side only)
    └── webapp/
        ├── index.html              Entry point → redirects to /login
        ├── error.html              Friendly 404/500 page
        ├── css/style.css           Blue & white responsive theme
        ├── js/                     Frontend logic (fetch API calls, popups, tables)
        └── WEB-INF/
            ├── web.xml             Filters, session config, welcome & error pages
            └── views/              HTML pages; WEB-INF makes them unreachable except via ViewServlet
```

The layering is **controller → service → dao → MySQL**. Each layer only talks to the one below
it, which keeps HTTP concerns, business rules and SQL separate and easy to test or change.

---

## 5. Architecture & request flow

```
Browser (HTML/CSS/JS)
   │  fetch('api/deposit', POST form data + X-CSRF-Token header)
   ▼
CharacterEncodingFilter  → UTF-8 + security headers
   ▼
AuthFilter               → session exists? CSRF token valid?   (else 401 / 403 / redirect)
   ▼
DepositServlet (controller) → reads parameters, maps exceptions to HTTP status + JSON
   ▼
BankingService (service)    → validates, opens DB transaction, enforces business rules
   ▼
BankAccountDAO / TransactionDAO (dao) → PreparedStatement SQL
   ▼
MySQL (InnoDB)  → row lock, UPDATE balance, INSERT transaction, COMMIT
```

Pages are plain HTML served by `ViewServlet`; each page's JavaScript calls the JSON APIs and
renders the data. Every API response uses the same envelope:

```json
{ "success": true, "message": "Amount deposited successfully.", "data": { ... } }
{ "success": false, "message": "Please correct the highlighted fields.", "errors": { "amount": "..." }, "data": null }
```

---

## 6. Java classes and methods explained

### 6.1 `util` package – shared helpers

| Class | Purpose | Methods |
|---|---|---|
| **DBConnection** | Single place that creates JDBC connections. Loads `db.properties` once (static block), lets env vars override, registers the MySQL driver. | `getConnection()` – opens a new connection (caller closes it via try-with-resources). `rollbackQuietly(con)` – rolls back without masking the original exception. `envOr()` – env-var override helper. |
| **PasswordUtil** | BCrypt hashing (cost 12, random salt per password). | `hash(plain)` – returns a 60-char BCrypt hash. `verify(plain, hash)` – constant-time check; false for malformed hashes. `simulateVerify(plain)` – dummy check so unknown usernames take the same time as wrong passwords (prevents user enumeration). |
| **ValidationUtil** | All server-side validation rules (regex + limits). | `validateRegistration(...)` – returns a map of every invalid signup field. `passwordError(pwd)` – strength policy. `isValidEmail` / `isValidMobile`. `validateAccountNumber(raw)` – exactly 12 digits. `parseAmount(raw)` – BigDecimal, > 0, ≤ 2 decimals, ≤ ₹10,00,000. `validateDescription(raw, default)` – ≤ 255 chars, strips control chars. `parseOptionalDate(raw, field)` – ISO date for history filters. `clean` / `isBlank` – null-safe helpers. |
| **ValidationException** | Checked exception carrying a message + per-field errors for the UI. | `getFieldErrors()` |
| **AccountNumberGenerator** | Produces random 12-digit numbers `5010XXXXXXXX` using `SecureRandom`. | `generate()` |
| **JsonUtil** | Writes the JSON envelope with Gson (LocalDateTime → `yyyy-MM-dd HH:mm:ss`). | `write(resp, status, body)`, `success(resp, msg, data)`, `error(resp, status, msg, fieldErrors[, data])` |
| **SessionUtil** | Session-based authentication + CSRF token. | `login(req, user)` – invalidates any old session (session-fixation defence), stores userId/name/username and a random CSRF token. `logout(req)`. `getUserId(req)` / `isLoggedIn(req)` / `getString(req, name)`. `isValidCsrf(req)` – constant-time compare of `X-CSRF-Token` header. |

### 6.2 `model` package – data objects

| Class | Purpose |
|---|---|
| **User** | Row of `users`. `passwordHash` is `transient` so it is never serialised to JSON. `getFirstName()` builds "Welcome back, Venu!". |
| **BankAccount** | Row of `bank_accounts`. New accounts start at `0.00`, `ACTIVE`. `isActive()`. |
| **Transaction** | Row of `transactions`; constants `CREDIT/DEBIT/SUCCESS/FAILED`. `deriveCreditDebit()` fills the separate *Credit Amount* / *Debit Amount* columns for the history table. |
| **TransactionFilter** | Optional search criteria (type, from/to date, keyword, sort, limit ≤ 500). |
| **TransactionResult** | Deposit/withdraw outcome: transaction id, previous balance, amount, new balance – used for the success popup. |
| **DashboardSummary** | Everything the dashboard displays, including `balanceVerified`. |
| **RegistrationResult** | New `User` + its `BankAccount`, returned after signup. |

### 6.3 `dao` package – SQL access (PreparedStatement only)

Every DAO method receives a `Connection` from the service so several calls can share one transaction.

**UserDAO**

| Method | SQL | Purpose |
|---|---|---|
| `existsByEmail` | `SELECT 1 FROM users WHERE email = ? LIMIT 1` | Unique-email check at signup |
| `existsByUsername` | `SELECT 1 FROM users WHERE username = ? LIMIT 1` | Unique-username check |
| `insert` | `INSERT INTO users (...) VALUES (?,?,?,?,?,?,?)` | Saves the user, returns generated `user_id` |
| `findByUsernameOrEmail` | `SELECT ... WHERE username = ? OR email = ?` | Login lookup |
| `findById` | `SELECT ... WHERE user_id = ?` | Dashboard customer name |
| `findForPasswordReset` | `SELECT ... WHERE username = ? AND email = ? AND mobile = ?` | Forgot-password identity check |
| `updatePassword` | `UPDATE users SET password_hash = ? WHERE user_id = ?` | Saves the new hash |

**BankAccountDAO**

| Method | SQL | Purpose |
|---|---|---|
| `existsByAccountNumber` | `SELECT 1 FROM bank_accounts WHERE account_number = ?` | Guarantees generated numbers are unique |
| `insert` | `INSERT INTO bank_accounts (...)` | Opens the account with ₹0.00 |
| `findByUserId` | `SELECT ... WHERE user_id = ?` | The logged-in user's account |
| `findByAccountNumberForUpdate` | `SELECT ... WHERE account_number = ? FOR UPDATE` | **Locks the row** during deposit/withdrawal so concurrent requests queue up |
| `updateBalance` | `UPDATE bank_accounts SET balance = ? WHERE account_id = ?` | Writes the new balance |

**TransactionDAO**

| Method | SQL | Purpose |
|---|---|---|
| `insert` | `INSERT INTO transactions (...)` | Ledger entry with `balance_after` |
| `sumByType` | `SELECT COALESCE(SUM(amount),0) ... WHERE account_id=? AND transaction_type=? AND status='SUCCESS'` | Total Credit / Total Debit |
| `countSuccessful` | `SELECT COUNT(*) ... status='SUCCESS'` | Total Transactions |
| `findRecent` | `... ORDER BY transaction_date DESC, transaction_id DESC LIMIT ?` | Dashboard recent list |
| `search` | Dynamic `WHERE` (type / date range / `description LIKE ?` / id) + `ORDER BY` ASC/DESC from a whitelist + `LIMIT ?` | History page filters – every value still bound with `?` |

### 6.4 `service` package – business logic

**UserService**

* `register(...)` – trims/normalises input (email lower-cased), validates every field, then in **one DB transaction**: checks duplicate email/username, hashes the password, inserts the user, generates a unique account number, inserts the SAVINGS account with ₹0.00, **commits**. Any failure rolls back both inserts. A race on the UNIQUE constraint is converted into a friendly validation error.
* `uniqueAccountNumber(con)` – retries generation up to 10 times until the number is unused.
* `authenticate(identifier, password)` – finds the user by username or email and verifies the BCrypt hash; returns `null` for any mismatch (generic error message), refuses LOCKED users.
* `resetPassword(...)` – validates, verifies username + email + mobile together, stores a new BCrypt hash.

**BankingService**

* `getAccountForUser(userId)` – used by `/api/me` to pre-fill the forms.
* `getDashboard(userId)` – reads user, account, totals, count and recent transactions in a single **REPEATABLE READ, read-only** transaction (consistent snapshot) and sets `balanceVerified = (totalCredit − totalDebit == balance)`; logs SEVERE on mismatch.
* `deposit(...)` / `withdraw(...)` – thin wrappers around `execute(...)`.
* `execute(...)` – the core money-movement algorithm:
  1. validate account number, amount, description;
  2. `setAutoCommit(false)`;
  3. `SELECT … FOR UPDATE` the account row;
  4. **authorisation** – account must belong to the session user (else `AccessDeniedException` → 403);
  5. account must be ACTIVE;
  6. for DEBIT: if `amount > balance` → record a FAILED audit row, commit, throw `InsufficientBalanceException` (balance untouched);
  7. compute new balance with `BigDecimal` (`add` / `subtract`);
  8. `UPDATE` balance + `INSERT` ledger row;
  9. `commit()`; on any exception → `rollback()`.
* `getHistory(userId, filter)` – always looks up the account **from the session user**, so a user can never read someone else's transactions.

**AccessDeniedException** (→ HTTP 403) and **InsufficientBalanceException** (→ HTTP 422, carries available & requested amounts).

### 6.5 `controller` package – HTTP layer

| Class | Mapping | Purpose |
|---|---|---|
| **CharacterEncodingFilter** | `/*` | UTF-8 for requests/responses; `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` headers |
| **AuthFilter** | protected pages & APIs (see `web.xml`) | No session → redirect to `/login` (pages) or 401 JSON (APIs). POST without valid CSRF token → 403. Adds no-cache headers so Back after logout can't show data. |
| **ViewServlet** | `/login`, `/signup`, `/forgot-password`, `/dashboard`, `/deposit`, `/withdraw`, `/transactions` | `doGet` forwards to `/WEB-INF/views/<page>.html`; logged-in users visiting login/signup are redirected to the dashboard |
| **SignupServlet** | `POST /api/signup` | Calls `UserService.register`, returns account number for the welcome popup |
| **LoginServlet** | `POST /api/login` | Calls `authenticate`, starts the session via `SessionUtil.login`, returns the welcome message |
| **LogoutServlet** | `POST /api/logout` | Invalidates the session |
| **ForgotPasswordServlet** | `POST /api/forgot-password` | Calls `resetPassword` |
| **SessionInfoServlet** | `GET /api/me` | Name, account number, balance, CSRF token for the current user |
| **DashboardServlet** | `GET /api/dashboard` | Returns `DashboardSummary` |
| **DepositServlet** | `POST /api/deposit` | Calls `deposit`; maps 400/403/500 |
| **WithdrawServlet** | `POST /api/withdraw` | Calls `withdraw`; insufficient funds → 422 with available balance |
| **TransactionHistoryServlet** | `GET /api/transactions` | `buildFilter()` validates `type`, `from`, `to`, `q`, `sort`, then calls `getHistory` |
| **AppContextListener** | `@WebListener` | Logs startup; on shutdown de-registers the JDBC driver and stops MySQL's cleanup thread (prevents Tomcat memory-leak warnings on redeploy) |

Exception handling pattern in every servlet: `ValidationException` → 400 with field errors,
`AccessDeniedException` → 403, `InsufficientBalanceException` → 422, `SQLException` → logged
server-side with stack trace, client gets a generic 500 message (no internal details leaked).

---

## 7. Database design and SQL queries explained

### Tables

**users**

| Column | Type | Notes |
|---|---|---|
| user_id | BIGINT AUTO_INCREMENT | **PK** |
| full_name | VARCHAR(100) | |
| email | VARCHAR(150) | **UNIQUE** (`uq_users_email`) |
| mobile | VARCHAR(15) | indexed |
| username | VARCHAR(30) | **UNIQUE** (`uq_users_username`), case-insensitive collation |
| password_hash | CHAR(60) | BCrypt hash – never plain text |
| address | VARCHAR(255) | |
| status | ENUM('ACTIVE','LOCKED') | |
| created_at / updated_at | TIMESTAMP | auto-maintained |

**bank_accounts**

| Column | Type | Notes |
|---|---|---|
| account_id | BIGINT AUTO_INCREMENT | **PK** |
| user_id | BIGINT | **FK → users.user_id**, **UNIQUE** (one account per user) |
| account_number | VARCHAR(20) | **UNIQUE** |
| account_type | ENUM('SAVINGS','CURRENT') | default SAVINGS |
| balance | **DECIMAL(15,2)** | default 0.00, **CHECK (balance >= 0)** |
| status | ENUM('ACTIVE','FROZEN','CLOSED') | |

**transactions**

| Column | Type | Notes |
|---|---|---|
| transaction_id | BIGINT AUTO_INCREMENT | **PK** (shown as "Txn ID") |
| account_id | BIGINT | **FK → bank_accounts.account_id** |
| transaction_type | ENUM('CREDIT','DEBIT') | |
| amount | **DECIMAL(15,2)** | **CHECK (amount > 0)** |
| balance_after | **DECIMAL(15,2)** | balance right after this transaction, CHECK ≥ 0 |
| description | VARCHAR(255) | |
| status | ENUM('SUCCESS','FAILED') | FAILED = declined withdrawal (no money moved) |
| transaction_date | TIMESTAMP | default CURRENT_TIMESTAMP |

Indexes: `idx_txn_account_date (account_id, transaction_date)` speeds up history and recent
lists; `idx_txn_account_type_status (account_id, transaction_type, status)` speeds up the
SUM/COUNT totals. Foreign keys use `ON DELETE RESTRICT`, so financial history can never be
silently deleted.

Why **DECIMAL(15,2)**: exact base-10 arithmetic (no floating-point errors such as
0.1 + 0.2 = 0.30000000000000004), up to ₹9,99,99,99,99,999.99. Java uses `BigDecimal` end to end.

### Sample queries

`database/sample_queries.sql` contains the exact queries used by the DAOs plus:

* a **reconciliation query** returning any account whose stored balance ≠ credits − debits (should return 0 rows);
* daily credit/debit summary;
* failed withdrawal attempts;
* a demo of the `START TRANSACTION … SELECT … FOR UPDATE … COMMIT/ROLLBACK` pattern.

---

## 8. Frontend pages and scripts explained

| Page (`WEB-INF/views/`) | URL | Purpose |
|---|---|---|
| `login.html` | `/login` | Username/email + password, Login button, Forgot Password link, Create New Account link |
| `signup.html` | `/signup` | 7 registration fields, password strength meter, show/hide password; welcome popup with account number |
| `forgot-password.html` | `/forgot-password` | Identity check (username + email + mobile) and new password |
| `dashboard.html` | `/dashboard` | Balance card, account details, credit/debit/count cards, recent transactions, "Welcome back" popup |
| `deposit.html` | `/deposit` | Account number (pre-filled, read-only), amount with quick-amount chips, description |
| `withdraw.html` | `/withdraw` | Same as deposit, plus insufficient-balance popup |
| `transactions.html` | `/transactions` | Search, type filter, date range, sort, totals of shown rows, full table with status |

| Script (`js/`) | Purpose |
|---|---|
| `common.js` | `NB` module: `request()` fetch wrapper (form-encoded body, CSRF header, 401 → login), `toast()`, `modal()` popups, field-error helpers, INR & date formatting, navbar + sidebar rendering, `logout()` |
| `auth.js` | Login, signup and forgot-password form handling with client-side validation mirroring the server rules |
| `dashboard.js` | Loads `/api/dashboard`, fills cards and recent table, shows the one-time welcome popup |
| `transaction-form.js` | Shared by deposit & withdraw: validation, submit, success popup (previous / amount / new balance), insufficient-balance popup |
| `history.js` | Builds query string from filters, loads `/api/transactions`, renders table, debounced search-as-you-type |

`css/style.css` – CSS variables for the blue/white palette, auth split-screen, fixed navbar,
collapsible sidebar (hamburger under 900 px), cards, tables that turn into stacked cards on
phones, modal and toast styles.

All dynamic text is inserted with `textContent` (never `innerHTML`), so stored data can't inject scripts (XSS).

---

## 9. REST API reference

| Method | Endpoint | Auth | Parameters | Success `data` |
|---|---|---|---|---|
| POST | `/api/signup` | – | fullName, email, mobile, username, password, confirmPassword, address | fullName, username, accountNumber, accountType, balance |
| POST | `/api/login` | – | identifier, password | fullName, firstName |
| POST | `/api/forgot-password` | – | username, email, mobile, newPassword, confirmPassword | – |
| POST | `/api/logout` | session + CSRF | – | – |
| GET | `/api/me` | session | – | fullName, username, csrfToken, accountNumber, accountType, balance |
| GET | `/api/dashboard` | session | – | DashboardSummary |
| POST | `/api/deposit` | session + CSRF | accountNumber, amount, description | TransactionResult |
| POST | `/api/withdraw` | session + CSRF | accountNumber, amount, description | TransactionResult (422 if insufficient) |
| GET | `/api/transactions` | session | type (ALL/CREDIT/DEBIT), from, to (yyyy-MM-dd), q, sort (desc/asc) | Transaction[] |

---

## 10. Security measures

| Requirement | Implementation |
|---|---|
| Password hashing | BCrypt cost 12 with per-password salt (`PasswordUtil`) |
| SQL injection | 100 % `PreparedStatement` with `?` placeholders; sort direction from a whitelist; LIKE wildcards escaped |
| Session authentication | `HttpSession` with userId; new session on login (session fixation); 15-min timeout; HttpOnly + SameSite=Strict cookie; cookie-only tracking (no `;jsessionid` in URLs) |
| Unauthorized access | `AuthFilter` on every protected page/API; WEB-INF views not directly reachable; account ownership checked on every deposit/withdrawal; history always scoped to the session user |
| CSRF | Random per-session token, required in `X-CSRF-Token` header on protected POSTs |
| Server-side validation | `ValidationUtil` for every field, regardless of browser checks |
| Negative balances | Service check + `SELECT … FOR UPDATE` + DB `CHECK (balance >= 0)` |
| Atomic money movement | JDBC transactions with commit/rollback |
| Credentials | `db.properties` on the server classpath / environment variables – never in HTML/JS |
| Error leakage | Generic messages to clients, details only in server logs; custom error page |
| XSS / clickjacking | `textContent` rendering, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` |
| Username enumeration | Generic login error + dummy BCrypt check for unknown users |

For production also: serve over HTTPS and enable `<secure>true</secure>` in `web.xml`, use the
least-privilege DB user from `schema.sql`, add a connection pool (Tomcat JDBC / HikariCP),
OTP-based password reset and login-attempt throttling.

---

## 11. Balance calculation & consistency

```
Total Credit    = SUM(amount) WHERE type = 'CREDIT' AND status = 'SUCCESS'
Total Debit     = SUM(amount) WHERE type = 'DEBIT'  AND status = 'SUCCESS'
Current Balance = Total Credit − Total Debit
```

* Every successful deposit/withdrawal updates `bank_accounts.balance` **and** inserts the
  ledger row in the **same** DB transaction, so the two can never diverge.
* The row lock (`FOR UPDATE`) means two simultaneous withdrawals of ₹3,000 from a ₹5,000 balance
  are processed one after the other – the second is declined.
* The dashboard recomputes credits − debits and shows "✓ Verified" when it equals the stored
  balance (a warning is logged and displayed otherwise).
* FAILED (declined) rows are kept for audit but excluded from totals.

Worked example: deposit ₹5,000 (0 → 5,000), withdraw ₹1,500 (5,000 → 3,500):
Total Credit 5,000.00 − Total Debit 1,500.00 = **3,500.00** = stored balance ✓

---

## 12. Troubleshooting

| Problem | Fix |
|---|---|
| `mvn` is not recognized | Add `%MAVEN_HOME%\bin` to `Path`, open a new terminal |
| HTTP 404 at `/NetBanking/` | Check the WAR is in `webapps` and Tomcat logs (`logs\catalina.*.log`) show it deployed; URL is case-sensitive |
| `ClassNotFoundException: jakarta.servlet...` or `javax.servlet` errors | You are using Tomcat 9 – install **Tomcat 10.1** |
| `Access denied for user 'root'@'localhost'` | Wrong password in `db.properties`; rebuild & redeploy after editing |
| `Communications link failure` | MySQL service not running – start "MySQL80" in `services.msc`; check port 3306 |
| `Unknown database 'netbanking_db'` | Run `database/schema.sql` |
| `Public Key Retrieval is not allowed` | Keep `allowPublicKeyRetrieval=true` in the JDBC URL |
| Port 8080 already in use | Change the `Connector port` in `conf\server.xml` or stop the other program |
| Changes don't appear | `mvn clean package`, redeploy, then hard refresh (Ctrl + F5) |
| "Session expired" immediately | Make sure cookies are enabled; open via `localhost`, not a `file://` path |

Tomcat logs: `C:\apache-tomcat-10.1.x\logs\` (`catalina.<date>.log`, `localhost.<date>.log`).
