# SQL Database Migration & Role-Based Access Control Implementation

## Overview
The Voting System has been completely refactored to migrate from plain text file storage to a modern SQL (SQLite) database system, coupled with comprehensive role-based access control (RBAC). The system NO LONGER requires a Superadmin role - all admin roles have predefined selective permissions.

---

## Key Changes

### 1. **Database Backend Migration**
- **Old System**: Plain text files (`database_admins.txt`, `database_voters.txt`, etc.)
- **New System**: SQLite database (`election_system.db`)

**Benefits:**
- Data integrity and consistency
- ACID transaction support
- Scalability
- Better security
- Automatic backups possible
- Concurrent access support

### 2. **Role-Based Access Control (RBAC)**

The system now uses **6 predefined roles** instead of a superadmin model:

#### **Available Roles:**

1. **VoterManager** 
   - Permissions: Add voters, Delete voters, View voters, Edit voters
   - Can manage voter registration lifecycle
   - Cannot access other modules

2. **NomineeManager**
   - Permissions: Add nominees, Delete nominees, View nominees, Edit nominees
   - Can manage election candidates
   - Cannot access voting or results

3. **ElectionManager**
   - Permissions: Configure election, Activate/deactivate election, View election status, Monitor voters
   - Can manage the entire election process
   - Can activate/deactivate voting
   - Cannot directly view results

4. **ReportViewer**
   - Permissions: View election results only
   - Read-only access to vote statistics
   - Cannot modify any data

5. **AuditViewer**
   - Permissions: View audit logs only
   - Track all admin actions in the system
   - Useful for compliance and monitoring

6. **SystemAdmin** (Emergency Use Only)
   - Permissions: Full system access
   - Reserve for critical maintenance only
   - Should be used sparingly

### 3. **NO SUPERADMIN Role**
- ❌ No admin can create other admins (removes privilege escalation risk)
- ✅ Initial admin must be created during system setup via FirstSetup dialog
- ✅ Each role has specific, limited permissions

---

## New Files Created

### `Data/DatabaseManager.java` (Enhanced)
- Central database management
- SQL table creation and initialization
- Connection pooling
- Query execution helpers

### `Data/SqlAdminManager.java` (NEW)
- Manages admin authentication from SQL
- Validates admin credentials
- Admin CRUD operations
- Permission checking
- Audit logging integration

### `Data/SqlElectionDataManager.java` (NEW)
- Voter management (add, delete, register)
- Nominee management
- Vote recording and tallying
- Election statistics
- Atomic vote transactions

### `Utils/AdminRole.java` (NEW)
- Defines all 6 roles and their permissions
- Permission validation methods
- Role descriptions
- Permission mapping

### `Data/DataMigrationUtility.java` (NEW)
- One-time migration tool
- Migrates data from text files to SQL
- Preserves all existing data
- Interactive command-line interface
- Handles error recovery

### `Framesg/AdminLogin.java` (Updated)
- SQL-based authentication
- FirstSetup dialog for initial admin
- Role-based login handling
- Database integration

### `Framesg/AdminDashboard.java` (Completely Refactored)
- Role-based UI (shows only available features)
- SQL data operations
- Simplified feature set (no old complex dialogs)
- Permission-based action handling
- Audit logging for all operations

### `Main.java` (Updated)
- Database initialization on startup
- Migration offer if old files detected
- Cleaner startup flow

---

## Database Schema

### Tables Created:

```sql
-- Admins with role-based permissions
CREATE TABLE admins (
    admin_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL,
    salt TEXT NOT NULL,
    is_active INTEGER DEFAULT 1,
    permissions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
)

-- Voters with registration status
CREATE TABLE voters (
    voter_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    password_hash TEXT,
    salt TEXT,
    email TEXT,
    is_registered INTEGER DEFAULT 0,
    has_voted INTEGER DEFAULT 0,
    registered_at TIMESTAMP,
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)

-- Nominees for elections
CREATE TABLE nominees (
    nominee_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    party TEXT NOT NULL,
    position TEXT,
    is_active INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)

-- Vote records with voter and nominee references
CREATE TABLE votes (
    vote_id INTEGER PRIMARY KEY AUTOINCREMENT,
    voter_id TEXT NOT NULL,
    nominee_id TEXT NOT NULL,
    cast_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (voter_id) REFERENCES voters(voter_id),
    FOREIGN KEY (nominee_id) REFERENCES nominees(nominee_id)
)

-- Audit trail for admin actions
CREATE TABLE audit_logs (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    admin_id TEXT,
    action TEXT NOT NULL,
    details TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES admins(admin_id)
)

-- Election configuration
CREATE TABLE election_config (
    config_id TEXT PRIMARY KEY,
    is_active INTEGER DEFAULT 0,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    election_name TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
```

---

## Migration Process

### For Existing Systems:

1. **On First Start**: Application detects old plain text files
2. **User is Prompted**: "Migrate data to SQL system?"
3. **Automatic Migration** runs (if user accepts):
   - Admins → SQL admins table
   - Voters → SQL voters table
   - Nominees → SQL nominees table
   - Votes → SQL votes table
   - Salts preserved → SQL salt column

4. **Backup Created**: Old files remain as backup
5. **Optional Cleanup**: User can delete old files after verification

### Manual Migration:

```bash
java Data.DataMigrationUtility
```

---

## First-Time Setup

When no admins exist in the database:

1. Click "Admin Login"
2. See "First setup" warning
3. Click "Setup Initial Admin"
4. Fill in admin details:
   - Admin ID
   - Full Name
   - Password
   - Role (dropdown with 4 options)
5. Admin account created
6. Can now log in

### Initial Admin Recommendations:
- Create one **ElectionManager** for overall control
- Create one **VoterManager** for voter data
- Create one **NomineeManager** for candidates
- Create one **ReportViewer** for results viewing

---

## Authentication & Security

### Password Hashing:
- SHA-256 with 10,000 iterations (key stretching)
- Unique salt per user (256-bit)
- Base64 encoded for storage
- Backward compatible with old hashed passwords

### Features:
- ✅ No plaintext passwords stored
- ✅ Salt unique per user
- ✅ Password validation on login
- ✅ Last login tracking
- ✅ Account active/inactive status
- ✅ Audit logging of all admin actions

---

## Permission Examples

### VoterManager with these permissions:
- ✅ Can add new voters
- ✅ Can view voter list
- ✅ Can delete voters
- ❌ Cannot see nominees
- ❌ Cannot configure election
- ❌ Cannot view results

### ElectionManager can:
- ✅ Configure election schedule
- ✅ Activate/deactivate voting
- ✅ View election status
- ✅ Monitor voter participation
- ❌ Cannot add voters (ElectionManager lacks permission)
- ❌ Cannot view final results

### ReportViewer can:
- ✅ View live results
- ✅ See vote distribution
- ✅ View statistics
- ❌ Cannot modify anything
- ❌ Cannot access other features

---

## Migration from Old System

### Step-by-Step:

```
1. Start the application normally
   ↓
2. Application detects old text files
   ↓
3. Dialog asks about migration
   ↓
4. User chooses to migrate (or skip)
   ↓
5. DataMigrationUtility runs
   ↓
6. All data is imported into SQLite
   ↓
7. System continues with SQL backend
   ↓
8. Old files can be safely deleted
```

---

## System Architecture

### Old Architecture (Text Files):
```
App → Text File I/O → database_admins.txt
                   → database_voters.txt
                   → database_nominees.txt
                   → database_votes.txt
```

### New Architecture (SQL):
```
App → SqlAdminManager
   ├→ DatabaseManager → SQLite connection → election_system.db
   ├→ SqlElectionDataManager
   └→ AdminRole → Permission validation
```

---

## Files Removed/Changed

### Removed from Active Use:
- Plain text files are no longer read by application
- Old AdminData.java file still exists (backup)
- Old ElectionData.java file still exists (backup)

### Updated:
- `Main.java` - Database initialization
- `AdminLogin.java` - SQL authentication
- `AdminDashboard.java` - Role-based UI
- `Admin.java` - Enhanced entity (still used)

---

## Testing Checklist

- [ ] Database initializes on startup
- [ ] First-time setup dialog works
- [ ] Initial admin can be created
- [ ] Admin can log in
- [ ] AdminDashboard shows correct features per role
- [ ] VoterManager can add/delete voters
- [ ] NomineeManager can add/delete nominees
- [ ] ReportViewer sees only results
- [ ] AuditViewer sees only audit logs
- [ ] Permission denials show correct messages
- [ ] Audit logs record admin actions
- [ ] Role switching doesn't break permissions
- [ ] Data migration preserves all records
- [ ] Old text files can be deleted safely

---

## Configuration

### Database Location:
- File: `election_system.db`
- Stored in working directory
- Can be backed up like any other file

### Admin Roles Configuration:
- Edit `Utils/AdminRole.java` to modify permissions
- Add new roles in `ROLE_PERMISSIONS` map
- Update role selection dropdown in UI

---

## Troubleshooting

### "SQLite JDBC driver not found"
- Install SQLite JDBC library
- Add to classpath if building manually
- Application will inform if driver is missing

### "Database connection failed"
- Check if `election_system.db` is writable
- Check disk space
- Check folder permissions

### "Admin authentication failed"
- Verify admin exists in SQL database
- Check password (case-sensitive)
- Check if admin is marked active

### "Role not found"
- Go to `AdminRole.java` and verify role exists
- Check spelling of role name
- Ensure role is defined in `ROLE_PERMISSIONS` map

---

## Future Enhancements

- [ ] Password reset functionality
- [ ] Role assignment by another admin
- [ ] Advanced permission matrix
- [ ] Real-time audit log viewer
- [ ] Database backup utility
- [ ] Admin activity dashboard
- [ ] Two-factor authentication
- [ ] Security compliance reports

---

## Summary of Benefits

**Before (Plain Text Files):**
- ❌ No transactions
- ❌ No referential integrity
- ❌ Difficult to scale
- ❌ Security vulnerabilities
- ❌ Privilege escalation risk (superadmin)

**After (SQL + RBAC):**
- ✅ ACID transactions
- ✅ Referential integrity
- ✅ Scalable architecture
- ✅ Secure data storage
- ✅ Fine-grained access control
- ✅ Audit trail
- ✅ No superadmin elevation
- ✅ Clear role responsibilities

---

## Contact & Support

For issues or questions:
1. Check the troubleshooting section
2. Review the AdminRole.java for permissions
3. Check audit logs for action history
4. Verify database file exists and is readable

---

**System Version:** 3.0 (SQL + RBAC)
**Database**: SQLite 3.x
**Last Updated:** February 2026
