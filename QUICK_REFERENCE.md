# Quick Reference Guide

## 🎯 For Voters

### Registration
1. Start app → Click "Register"
2. Enter: Student ID, Full Name, Password (6+ chars)
3. Click "Register"
4. Redirected to login page

### Login & Voting
1. Start app → Click "Voter Login"
2. Enter: Student ID, Password
3. Click "Login"
4. Browse nominees and cast vote

## 🔧 For Administrators

### Import Voters

**Option 1: CSV File (Easiest)**
```bash
# Prepare CSV file: voters.csv
# Format: student_id,full_name,email
22-47797-2,Miskat Jahan,miskat@example.edu
24-59145-3,Yeamin Shakhor,yeamin@example.edu

# In Admin Dashboard:
# 1. Click "Manage Voters" → "Import Portal"
# 2. Select "CSV File Import" tab
# 3. Choose voters.csv
# 4. Click "Import"
```

**Option 2: Direct Portal (Automated)**
```bash
# 1. Edit portal_config.txt
moodle_url=https://moodle.example.edu/api/student
moodle_token=your_token_here

# 2. In Admin Dashboard:
#    Click "Import Portal" → "Direct Portal Fetch"
# 3. Select portal type (Moodle/LDAP)
# 4. Enter student ID
# 5. Click "Fetch from Portal"
```

**Option 3: Bulk API Import**
```java
// In code or utility:
ElectionData.importVotersFromCSV("students.csv");

// OR for portal:
PortalIntegration.bulkImportFromPortal("moodle", studentIds);
```

### Add Voters Manually (One by One)
```
Admin Dashboard → "Add Voter" card
├─ Enter: Voter ID, Full Name, Password
└─ Click "Add Voter"
```

### Monitor System
- Check `logs.txt` for all actions
- View login attempts in admin dashboard
- Check audit trail for security events

## 🔐 For Security Teams

### Password Storage
- Location: `database_voters.txt`
- Format: `id:name:hashed_password`
- Hash Algorithm: SHA-256 with PBKDF2 (10,000 iterations)
- Salt Location: `database_voter_salts.txt` (one per voter)

### Check Voter Record
```bash
$ cat database_voters.txt | grep "22-47797-2"
22-47797-2:Miskat Jahan:cTCjqf63ZPEPu9xB7GHS988qYZmR+BgQF6v6gIXd1dw=

# Get salt:
$ cat database_voter_salts.txt | grep "22-47797-2"
22-47797-2:A5fkIn/N61MEUCgpFfytdN9IgLco+ciQb6BjW4nxA0k=
```

### Verify Security
- All passwords should be 40+ characters (hashed)
- Every voter should have a salt entry
- No plaintext passwords allowed
- Check `logs.txt` for failed login attempts

## 📡 For IT Integration Teams

### Portal Configuration
Edit `portal_config.txt`:

```properties
# MOODLE LMS
moodle_url=https://moodle.yourinstitution.edu/webservice/rest/server.php?wsfunction=core_user_get_users&moodlewsrestformat=json
moodle_token=your_moodle_api_token

# LDAP
ldap_url=ldap://ldap.yourinstitution.edu:389
ldap_base_dn=dc=institution,dc=edu
ldap_bind_dn=cn=admin,dc=institution,dc=edu
ldap_password=your_ldap_password

# Custom REST API
rest_url=https://api.yourinstitution.edu/students
rest_token=your_api_token
```

### Test Connection
```java
// In admin code:
if (PortalIntegration.testConnection("moodle")) {
    System.out.println("✓ Portal is accessible");
} else {
    System.out.println("✗ Portal connection failed");
}
```

### Fetch Student Data
```java
PortalIntegration.StudentRecord student = 
    PortalIntegration.fetchStudentData("moodle", "22-47797-2");

if (student != null) {
    System.out.println("ID: " + student.id);
    System.out.println("Name: " + student.name);
    System.out.println("Email: " + student.email);
}
```

## 🆘 Troubleshooting

### "Voter ID already registered"
**Cause:** Voter already has an account
**Fix:** Have voter use login page, not register page

### "Password format unrecognized"
**Cause:** Old plaintext password in database
**Fix:** Import fresh voter data from portal or clear database

### "Portal connection failed"
**Cause:** Wrong URL, invalid token, or firewall blocked
**Fix:**
1. Check portal_config.txt settings
2. Verify portal URL is accessible
3. Test API token with portal admin
4. Check firewall/proxy rules

### "Failed to add voter"
**Cause:** Voter ID already exists or invalid data
**Fix:** Check database_voters.txt for duplicate ID

## 📊 Database Files

| File | Purpose | Format |
|------|---------|--------|
| database_voters.txt | Voter accounts | id:name:hashed_password |
| database_voter_salts.txt | Password salts | id:salt |
| database_nominees.txt | Election nominees | id:name:party:election_id |
| database_votes.txt | Cast votes | nominee_id:voter_hash |
| logs.txt | Audit trail | timestamp:action:details |

## 🚀 Common Commands

### Clear Old Voter Data (Start Fresh)
```bash
rm database_voters.txt database_voter_salts.txt
```

### Check Voter Count
```bash
wc -l database_voters.txt
```

### View Recent Login Attempts
```bash
tail -20 logs.txt
```

### Find Specific Voter
```bash
grep "22-47797-2" database_voters.txt
```

## 📞 Support Contacts

For Issues:
- **Code Problems:** Check logs.txt for error details
- **Portal Integration:** Verify portal_config.txt settings
- **Password Issues:** Voter can re-register via registration page
- **System Access:** Check brute force lockout (15 min timeout)

## 📚 Documentation Files

- **IMPLEMENTATION_GUIDE.md** - Complete setup guide
- **PERMANENT_IMPLEMENTATION.md** - Technical architecture
- **DEPLOYMENT_READY.txt** - Deployment checklist
- **portal_config.txt** - Portal configuration help
- **README.md** - Project overview

---

Keep this guide handy for quick reference! 📋

