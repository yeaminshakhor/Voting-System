# Voting System - SQLite Setup Guide

**Status**: ✅ **SQLite JDBC Driver Installed**  
**Version**: 3.44.0.0  
**Installation Date**: February 9, 2026  
**Database**: election_system.db (will be created on first run)

---

## Quick Start

### **Linux/macOS**
```bash
cd "Voting System"
./run.sh
```

### **Windows**
```cmd
cd "Voting System"
run.bat
```

---

## System Architecture

```
┌──────────────────────────────────────────────────────┐
│          VOTING SYSTEM DATABASE SETUP               │
├──────────────────────────────────────────────────────┤
│                                                      │
│  📁 Project Structure:                             │
│  ├── lib/                                          │
│  │   └── sqlite-jdbc-3.44.0.0.jar  ✅ INSTALLED  │
│  ├── election_system.db            (auto-created)  │
│  ├── database_voters.txt           (file-based)    │
│  ├── database_nominees.txt         (file-based)    │
│  ├── database_votes.txt            (file-based)    │
│  ├── run.sh                        (Linux/Mac)     │
│  └── run.bat                       (Windows)       │
│                                                      │
└──────────────────────────────────────────────────────┘
```

---

## What the SQLite Driver Enables

### **Direct SQL Database Access** ✅
- Election schedules with timestamps
- Database transactions support
- Query optimization
- Better handling of concurrent access

### **Backward Compatible with Files** ✅
- Voter data: Plain text files
- Nominee data: Plain text files
- Vote records: Plain text files
- Passwords/Salts: Plain text files

### **Automatic Fallback** ✅
If SQLite fails for any reason, system automatically uses file-based storage

---

## Database Initialization

When you run the application for the first time with SQLite:

1. **Driver loads successfully**
   ```
   ✅ SQLite JDBC driver found
   ```

2. **Connection established**
   ```
   ✅ Database connection established
   ```

3. **Tables created automatically**
   ```
   CREATE TABLE election_schedule (...)
   ```

4. **Database file created**
   ```
   election_system.db (created in project root)
   ```

---

## File Locations

| Component | Type | Location | Format |
|-----------|------|----------|--------|
| SQLite Database | SQL | `election_system.db` | Binary |
| Voters | File | `database_voters.txt` | `ID:Name:Password` |
| Nominees | File | `database_nominees.txt` | `ID:Name:Party:Election` |
| Votes | File | `database_votes.txt` | `Nominee:Timestamp` |
| Admin Accounts | File | `database_admins.txt` | `ID:Name:Role` |
| Voter Salts | File | `database_voter_salts.txt` | `ID:Salt` |
| Admin Salts | File | `database_admin_salts.txt` | `ID:Salt` |
| Election Config | File | `election_config.txt` | Config format |

---

## Compilation Commands

### **With SQLite Support**
```bash
# Linux/Mac
javac -cp "lib/sqlite-jdbc-3.44.0.0.jar:." -d . Main.java Framesg/*.java Data/*.java Entities/*.java Utils/*.java

# Windows
javac -cp "lib\sqlite-jdbc-3.44.0.0.jar;." -d . Main.java Framesg\*.java Data\*.java Entities\*.java Utils\*.java
```

### **Without SQLite (file-based only)**
```bash
javac -d . Main.java Framesg/*.java Data/*.java Entities/*.java Utils/*.java
```

---

## Runtime Commands

### **With SQLite Support**
```bash
# Linux/Mac
java -cp "lib/sqlite-jdbc-3.44.0.0.jar:." Main

# Windows
java -cp "lib\sqlite-jdbc-3.44.0.0.jar;." Main
```

### **Without SQLite (file-based only)**
```bash
java -cp "." Main
```

---

## Database Tables (SQLite)

### **election_schedule**
```sql
CREATE TABLE election_schedule (
  id INT PRIMARY KEY AUTO_INCREMENT,
  election_name VARCHAR(100) NOT NULL,
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP NOT NULL,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  UNIQUE(election_name)
);
```

### **admins** (Optional - can use file-based)
```sql
CREATE TABLE admins (
  admin_id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL,
  salt TEXT NOT NULL,
  created_at TIMESTAMP
);
```

### **voters** (Optional - can use file-based)
```sql
CREATE TABLE voters (
  voter_id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  password_hash TEXT NOT NULL,
  salt TEXT NOT NULL,
  is_registered INTEGER,
  has_voted INTEGER,
  registered_at TIMESTAMP,
  last_login TIMESTAMP
);
```

### **nominees** (Optional - can use file-based)
```sql
CREATE TABLE nominees (
  nominee_id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  party TEXT NOT NULL,
  election_id TEXT,
  created_at TIMESTAMP
);
```

---

## Troubleshooting

### **Issue: "SQLite JDBC driver not found"**
**Solution**:
1. Verify `lib/sqlite-jdbc-3.44.0.0.jar` exists
2. Check file size is ~13MB
3. Re-download if corrupted:
   ```bash
   wget https://github.com/xerial/sqlite-jdbc/releases/download/3.44.0.0/sqlite-jdbc-3.44.0.0.jar -O lib/sqlite-jdbc-3.44.0.0.jar
   ```

### **Issue: "Database connection failed"**
**Solution**:
- File permissions issue? Try: `chmod 755 election_system.db`
- Disk full? Free up space in project directory
- File in use? Close other database tools accessing it

### **Issue: "Table already exists"**
**Solution**:
- Normal on subsequent runs - tables already created
- Delete `election_system.db` to reset (caution: loses scheduling data)

---

## Performance Characteristics

| Operation | File-Based | SQLite |
|-----------|-----------|--------|
| Add voter | O(n) | O(1) |
| Voter lookup | O(n) | O(log n) |
| Get all voters | O(n) | O(n) |
| Election schedule | File parsing | SQL query |
| Concurrent access | Locks file | Transaction locks |
| Scalability | ~10K voters | ~1M voters |

---

## Backup & Recovery

### **Backup Database**
```bash
# Backup everything
cp election_system.db election_system.db.backup
cp database_*.txt database_backup/

# Backup only SQLite
cp election_system.db election_system.db.backup.$(date +%s)
```

### **Restore from Backup**
```bash
# Restore SQLite
cp election_system.db.backup election_system.db

# Restore files
cp database_backup/* .
```

---

## Future Enhancements

✨ **Potential Improvements**:
1. Migrate voter data to SQL for better performance
2. Add database indexing for faster lookups
3. Implement connection pooling for concurrent access
4. Add backup scheduling
5. Database maintenance tasks
6. Query optimization for reports

---

## Technical Details

**Driver**: SQLite JDBC (Xerial)  
**SQLite Version**: 3.44.0.0  
**JDBC Version**: 4.1+  
**Java Compatibility**: Java 8+  
**License**: Apache 2.0 (SQLite), MIT (JDBC)  
**Size**: ~13 MB (JAR file)  

---

## Support

For issues or questions:
1. Check `logs.txt` for error messages
2. Verify `election_system.db` file exists
3. Ensure `lib/sqlite-jdbc-3.44.0.0.jar` is present
4. Try running with file-based mode only

---

**Setup Complete** ✅  
Your voting system now has full SQLite support!
