# Project Issues – Voting System

Summary of issues found across the codebase. **Critical** items block admin login or cause runtime errors.

---

## Critical (blocking / runtime errors)

### 1. **Main calls non-existent `ensureDefaultSuperAdmin()`**
- **Where:** `Main.java` line 54: `SqlAdminManager.ensureDefaultSuperAdmin();`
- **Problem:** `SqlAdminManager` does not define `ensureDefaultSuperAdmin()`. It has `initializeAdminSystem()` and a private `createDefaultSuperAdmin()`.
- **Effect:** Compile error, or at runtime `NoSuchMethodError`. Default SuperAdmin is never created, so admin login fails.
- **Fix:** Either add `ensureDefaultSuperAdmin()` in `SqlAdminManager` that creates default superadmin when none exist, or call `SqlAdminManager.initializeAdminSystem()` from `Main` (and ensure that creates the default admin).

### 2. **AdminLogin emergency button uses invalid role "SUPERADMIN"**
- **Where:** `Framesg/AdminLogin.java` line 188: `SqlAdminManager.addAdmin("admin", "System Administrator", "admin123", "SUPERADMIN");`
- **Problem:** `AdminRole.isValidRole("SUPERADMIN")` is false. Valid constant is `AdminRole.SUPERADMIN` = `"SuperAdmin"`.
- **Effect:** Emergency “Create default admin” fails with “Invalid role” and no admin is created.
- **Fix:** Use `AdminRole.SUPERADMIN` (or the string `"SuperAdmin"`) instead of `"SUPERADMIN"`.

### 3. **AdminLogin calls non-existent `migrateAllAdminsFromTextFile()`**
- **Where:** `Framesg/AdminLogin.java` line 215: `int migrated = SqlAdminManager.migrateAllAdminsFromTextFile();`
- **Problem:** `SqlAdminManager` has no method `migrateAllAdminsFromTextFile()`.
- **Effect:** Compile error or `NoSuchMethodError` when user chooses “Migrate from text file” in the emergency dialog.
- **Fix:** Implement `migrateAllAdminsFromTextFile()` in `SqlAdminManager` (e.g. using `AdminData` / `DataMigrationUtility` logic), or remove/change that option and the call.

### 4. **Admins table schema mismatch (DatabaseManager vs SqlAdminManager)**
- **Where:** `Data/DatabaseManager.java` creates `admins` with: `admin_id, name, password_hash, role, salt, is_active, permissions, created_at, last_login`.  
  `SqlAdminManager.createAdminTableIfNotExists()` uses the same plus `needs_password_reset`, `updated_at`.
- **Problem:** `DatabaseManager.createTables()` runs first and creates `admins` without `needs_password_reset` or `updated_at`. Table already exists, so `SqlAdminManager`’s “CREATE TABLE IF NOT EXISTS” does not add columns. Later code uses `needs_password_reset` in SELECT/UPDATE.
- **Effect:** `SQLException` (e.g. “no such column: needs_password_reset”) when validating login, adding admin, or updating password.
- **Fix:** Add missing columns in `DatabaseManager.createTables()` for `admins` (`needs_password_reset`, `updated_at`), or run a one-time migration to add them to existing DBs.

### 5. **audit_logs table column name mismatch**
- **Where:** `DatabaseManager` creates `audit_logs` with `log_id`; `SqlAdminManager.createAdminTableIfNotExists()` creates it with `id`.
- **Problem:** If `DatabaseManager` creates the table first, the PK is `log_id`. Inserts from `SqlAdminManager` don’t reference the PK so they may still work; any code that selects by `id` would fail.
- **Effect:** Possible `SQLException` when querying audit logs by primary key. Currently `SqlAdminManager.logAdminAction` only inserts, so may work until something reads by `id`.
- **Fix:** Use a single definition (e.g. `log_id` everywhere or `id` everywhere) in both `DatabaseManager` and `SqlAdminManager`.

---

## High (wrong behavior / data split)

### 6. **Voter flow uses text files; Admin dashboard uses SQL**
- **Where:** `VoterLogin`, `VoterRegistration`, `VoterVoting`, `VotingFrame`, `VoterProfile`, `RegisterFrame`, `ElectionResults`, etc. use `Data.ElectionData` (text files).  
  `AdminDashboard` uses `SqlElectionDataManager` (SQL).
- **Problem:** After migration, voter/nominee/vote data lives in SQL, but voter-facing screens still use `ElectionData` (database_voters.txt, database_nominees.txt, database_votes.txt). Migrated data is not used for login or voting.
- **Effect:** Migrated voters cannot log in via the current voter login; admins see SQL data; voters see (possibly stale) text file data. Two sources of truth.
- **Fix:** Use `SqlElectionDataManager` (and SQL) for voter login, registration, and voting when the app is in “SQL mode”, or route `ElectionData` to SQL backend.

### 7. **SqlAdminManager never called for initialization from Main**
- **Where:** `SqlAdminManager.initializeAdminSystem()` creates/verifies the admins table and default SuperAdmin but is never invoked.
- **Problem:** `Main` calls the non-existent `ensureDefaultSuperAdmin()` and does not call `initializeAdminSystem()`. So the “enhanced” admin table (with `needs_password_reset`, etc.) and default SuperAdmin are only created if something else calls `initializeAdminSystem()`.
- **Effect:** Default admin creation and table shape depend on code paths that may not run; contributes to “admin login still fails”.
- **Fix:** From `Main`, call `SqlAdminManager.initializeAdminSystem()` (and remove or replace the call to `ensureDefaultSuperAdmin()` once that method exists or is replaced).

### 8. **DatabaseManager.executeUpdate / executeQuery can NPE**
- **Where:** `Data/DatabaseManager.java` – `executeUpdate()` and `executeQuery()` use `connection` without checking for null.
- **Problem:** If `getConnection()` previously failed, `connection` is null. Callers that use `executeUpdate`/`executeQuery` directly can get `NullPointerException`.
- **Fix:** At the start of both methods, if `getConnection() == null` (or `connection == null`), return false / null and optionally log.

---

## Medium (consistency / UX)

### 9. **Legacy admin login fallback removed or unused**
- **Where:** Earlier design had `SqlAdminManager.validateAdminCredentials()` fall back to `AdminData.validateAdminCredentials()` when the admin was not in SQL. Current `SqlAdminManager` (from the read) only checks SQL.
- **Problem:** If migration was skipped or failed, text-file admins cannot log in via the SQL Admin Login screen.
- **Fix:** Restore a safe fallback: if not found in SQL, validate with `AdminData` and on success migrate that admin into SQL, then allow login (and ensure migration success before returning true).

### 10. **Emergency dialog option labels vs behavior**
- **Where:** `AdminLogin.handleEmergencyButton()` – options 1, 2, 3 and the handling for YES/NO/CANCEL.
- **Problem:** Dialog text says “1. Create default admin, 2. Reset all passwords, 3. Migrate from text file” but the mapping is YES=1, NO=2, CANCEL=3. Users may not expect CANCEL to run migration.
- **Fix:** Clarify labels or use a list dialog so the action is unambiguous.

### 11. **VoterRegistration uses ElectionData.registerVoter(voter)**
- **Where:** `VoterRegistration.java` uses `ElectionData.registerVoter(voter)` (and related) which work on text files.
- **Problem:** Same as #6: after migration, registration should update SQL so that voter login and admin dashboard see the same data.
- **Fix:** When running with SQL, use `SqlElectionDataManager` for registration (and ensure “registered” and password are stored in SQL).

### 12. **Multiple entry points (Main vs Start/landing)**
- **Where:** `Main.java` (SQL version) and `Start.java` → `landing` (legacy UI with different flow).
- **Problem:** Two UIs and two data paths; confusion about which to run and which credentials work where.
- **Fix:** Document clearly; optionally make `Main` the only launcher and have it offer “legacy” vs “SQL” or migrate fully to SQL.

---

## Low / code quality

### 13. **AdminLogin compiled with obfuscated names**
- **Where:** `AdminLogin.java` uses `var1`, `var2`, `var3`, `var4`, etc. and `this.setDefaultCloseOperation(3)`.
- **Problem:** Hard to read and maintain; magic number for `JFrame.EXIT_ON_CLOSE`.
- **Fix:** Use descriptive variable names and constants (e.g. `JFrame.EXIT_ON_CLOSE`).

### 14. **DataMigrationUtility migrates admins without needs_password_reset**
- **Where:** `DataMigrationUtility.migrateAdminToDB()` inserts into `admins` with only `admin_id, name, password_hash, role, salt, is_active`.
- **Problem:** If the table is later extended with `needs_password_reset` (or other columns) with NOT NULL or no default, migration could fail on stricter schemas.
- **Fix:** Ensure migration INSERT matches the schema used by `SqlAdminManager` (e.g. include `needs_password_reset`, `permissions` if required).

### 15. **getCurrentAdmin() always returns null**
- **Where:** `SqlAdminManager.getCurrentAdmin()` always returns `null`.
- **Problem:** Audit logs for “who did this” show “system” instead of the actual admin when called from GUI.
- **Fix:** Implement a simple session/thread-local or static “current admin” set at login and cleared at logout, and use it in `getCurrentAdmin()`.

---

## Summary

| Severity | Count | Notes |
|----------|--------|--------|
| Critical | 5     | Admin login broken; schema mismatch; missing methods |
| High     | 3     | Voter path on text files; init not called; NPE risk |
| Medium   | 4     | Legacy fallback; UX; dual entry points |
| Low      | 3     | Naming; migration columns; current admin tracking |

**Immediate fixes recommended:**  
1) Add `ensureDefaultSuperAdmin()` in `SqlAdminManager` (or call `initializeAdminSystem()` from Main and ensure it creates default admin).  
2) Use `AdminRole.SUPERADMIN` in AdminLogin emergency button.  
3) Implement or remove `migrateAllAdminsFromTextFile()` and fix the dialog.  
4) Align `admins` (and optionally `audit_logs`) schema in `DatabaseManager` with `SqlAdminManager`.  
5) Add null check in `DatabaseManager.executeUpdate`/`executeQuery`.
