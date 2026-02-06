Voting System
🗳️ Online Voting System
A complete Java-based Online Voting System with admin controls, voter management, and real-time election monitoring.

📋 Overview
The Online Voting System is a robust desktop application built with Java Swing that facilitates secure digital voting. It provides separate interfaces for administrators and voters, with comprehensive election management capabilities for educational institutions, organizations, or small communities.

✨ Key Features
👥 For Voters
📝 Voter Registration: Register new accounts or set passwords for existing voter IDs

🔐 Secure Login: Password-protected authentication with validation

✓ Voting Interface: Simple candidate selection with confirmation dialog

🚫 Double-Voting Prevention: System ensures each voter votes only once

📊 Vote Confirmation: Immediate feedback after voting
👨‍💼 For Administrators
👥 Voter Management: Add, view, and delete voters with full details

🗳️ Nominee Management: Manage election candidates and parties

📈 Live Election Monitoring: Real-time vote counts and statistics

👀 Voter Status Tracking: Monitor participation in real-time

📄 Results Publication: Generate official election results report

⚙️ Election Configuration: Set up different voting systems

🔒 Security & Integrity
Role-based access control (SuperAdmin/Admin/Moderator)

Password validation and encryption

Comprehensive audit logging

Prevention of duplicate voting

Data validation at all levels
🏗️ System Architecture
src/
├── Main.java                    # Main application entry point
├── Data/                       # Data management layer
│   ├── AdminData.java          # Admin authentication & management
│   └── ElectionData.java       # Core election data operations
├── Entities/                   # Business entities
│   ├── Admin.java              # Administrator entity
│   ├── Nominee.java           # Candidate/Nominee entity
│   └── Voter.java             # Voter entity
└── Framesg/                    # User Interface components
    ├── AdminDashboard.java     # Admin control panel (main interface)
    ├── AdminLogin.java         # Admin authentication screen
    ├── VoterLogin.java         # Voter authentication screen
    ├── VoterRegistration.java  # Voter registration interface
    ├── VoterVoting.java        # Voting interface
    ├── VotingFrame.java        # Alternative voting interface
    └── ... (additional UI components)

    The system uses simple, human-readable text files for data persistence:

File	Purpose	Format
database_admins.txt	-Admin credentials and roles	-id:name:password:role
database_voters.txt	-Voter information	-id:name:password
database_nominees.txt	- Candidate information-	id:name:party
database_votes.txt	-Vote records-	voterId:nomineeId:timestamp
election_results.txt	Generated election results	Formatted report
logs.txt	System activity logs	Timestamped events
election_config.txt	Election configuration	Voting system type
