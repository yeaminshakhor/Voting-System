# Multi-Election Voting Control System

## Overview

This system allows elections administrators to control whether voters can participate in multiple concurrent voting events or if they are restricted to voting in only one election.

## Features Implemented

### 1. Election-Specific Voting Tracking

- **Per-Election Voting**: System now tracks which elections each voter has participated in
- **Format**: Voter voting log format upgraded to include election name
  - **Old Format**: `voterId:timestamp`
  - **New Format**: `voterId:timestamp:electionName`
  - **Backward Compatible**: Old format entries treated as "DEFAULT" election

### 2. Global Multi-Election Voting Policy

Administrators can set a system-wide policy for multi-election voting:

#### **ALLOW Multi-Election Voting** (Default)

- Voters can vote in multiple different elections if they're running concurrently
- A voter can participate in Election A, Election B, and Election C (if all are active)
- Each election can only receive ONE vote per voter
- **Use Case**: Multiple independent elections running in parallel

#### **RESTRICT to Single Election**

- Once a voter votes in ANY election, they cannot vote in any other election
- A voter who votes in Election A is blocked from voting in Election B
- **Use Case**: Single-day voting where only one election should be held at a time

### 3. Admin Voting Policy Management Panel

#### Location

- **AdminDashboard** → **Voting Policy** card
- **Permission Required**: `PERM_MANAGE_ELECTIONS`

#### Functionality

1. **View Current Policy**
   - Displays the current voting policy (ALLOW or RESTRICT)
   - Shows policy explanation

2. **Change Policy**
   - **Allow Multi-Election Voting** button
     - Enables voters to vote in multiple elections
     - No confirmation if already enabled
   - **Restrict to Single Election** button
     - Disables multi-election voting
     - Shows warning about voters already voted in other elections
     - Requires confirmation

3. **Voter Voting History**
   - Search by voter ID
   - View all elections a voter has voted in
   - Check voting status across elections

### 4. Voter API Methods

#### ElectionData Class Methods

```java
// Check if voter can vote in a specific election
boolean canVoteInElection(String voterId, String electionName)

// Check if voter has voted in a specific election  
boolean hasVotedInElection(String voterId, String electionName)

// Get all elections a voter has voted in
List<String> getVoterElectionHistory(String voterId)

// Check global multi-election voting policy
boolean isMultiElectionVotingAllowed()

// Set global voting policy
void setMultiElectionVotingPolicy(boolean allowMultiVoting)
```

### 5. Updated Voting Eligibility Checks

#### VoterVoting Interface

- Updated `checkVotingEligibility()` to:
  - Check current election
  - Verify voter eligibility based on policy
  - Display appropriate messages
  - Show voter history

#### VotingFrame Interface

- Updated `castVote()` to:
  - Check election-specific eligibility
  - Enforce multi-election voting restrictions
  - Provide clear feedback about restrictions

### 6. Vote Recording Enhancement

```java
// Vote recording now includes election information
public boolean castVoteInElection(String voterId, String nomineeId, String electionName)
```

**Voter Voted Log Format**: `voterId:timestamp:electionName`

## Database Files

### New Files Created

- `election_multi_voting_policy.txt`: Stores the global voting policy
  - Content: `ALLOW` or `RESTRICT`
  - Default value: `ALLOW`

### Modified Files

- `database_voter_voted_log.txt`: Updated format to include election name
  - Old entries: `voterId:timestamp`
  - New entries: `voterId:timestamp:electionName`

## User Experience

### For Voters

#### Scenario 1: Multi-Election Voting Allowed

1. Voter logs in during Election A
2. Casts vote in Election A
3. Can later log in and vote in Election B
4. **Result**: Voter has voted in 2 elections

#### Scenario 2: Single-Election Restriction Enabled

1. Voter logs in during Election A
2. Casts vote in Election A
3. Tries to vote in Election B
4. **Result**: Message displays "❌ You have already voted! According to policy, you can only vote in one election."
5. Shows voting history: "You already voted in: Election A"

#### Scenario 3: Multiple Elections Running

**With ALLOW Policy**:

- Voter can choose which elections to vote in
- Can vote in some but not others

**With RESTRICT Policy**:

- Voter must choose carefully
- First vote locks out all other elections

### For Administrators

#### Admin Panel Features

1. **View Current Policy**: See what rule is active
2. **Change Policy**: Toggle between ALLOW and RESTRICT
3. **Voter History**: Search any voter ID to see:
   - Which elections they voted in
   - Number of elections participated in
   - Timeline of participation

#### Decision Matrix

| Scenario | Policy | Voter Action | Result |
| --- | --- | --- | --- |
| Multi-election day | ALLOW | Vote in Election A | Can vote in B, C, etc. |
| Multi-election day | RESTRICT | Vote in Election A | Cannot vote in B or C |
| Single election | ALLOW | Vote in Election A | Not relevant (only 1 election) |
| Sequential elections | RESTRICT | Vote in Election A | Blocked from later elections |

## Configuration Example

### Step 1: Access Admin Dashboard

- Login as Admin with `PERM_MANAGE_ELECTIONS`
- Click "Manage Voting Policy" card

### Step 2: Enable Multi-Election Voting

- Click "✅ Allow Multi-Election Voting" button
- Policy updated to ALLOW
- Voters can now vote in multiple elections

### Step 3: Check Voter History

- Enter Voter ID in search box
- Click "Search"
- View which elections this voter participated in

### Step 4: Change to Restriction

- Click "🛑 Restrict to Single Election"
- Confirm warning dialog
- Policy updates to RESTRICT
- New voters cannot vote in multiple elections
- Existing voters already logged in can finish voting

## Technical Implementation Details

### Backward Compatibility

- Old voter voted log entries without election name treated as "DEFAULT" election
- System automatically upgrades format when new votes recorded
- No data loss or migration needed

### Election Name Resolution

```java
// Automatically uses current active election
String currentElection = ElectionScheduler.getCurrentActiveElection();
if (currentElection == null) {
    currentElection = "DEFAULT";
}
```

### Voting Flow

1. Voter attempts to vote
2. System gets current active election
3. Calls `canVoteInElection(voterId, election)`
4. This method:
   - Checks if multi-voting allowed
   - Checks if voter already voted in this election
   - Returns true/false
5. If allowed, proceeds with vote recording
6. Records vote with election name: `castVoteInElection(voterId, nomineeId, election)`

## Error Messages

### Multi-Voting Disabled Error

```
❌ You have already voted!

According to the voting policy, you can only vote in one election.
You have already voted in: Election A

If you believe this is an error, please contact the election administrator.
```

### Already Voted in This Election

```
⚠️ You have already cast your vote in this election!

Each voter can only vote once per election.
If you believe this is an error, please contact the election administrator.
```

## Voter Voting History Display

```
Your Voting History
═══════════════════════════════════════════

Voter ID: 22-47797-2
Total Elections Voted In: 2

Elections:
  1. Election A
  2. Election B

───────────────────────────────────────────
Voting Policy: ALLOW multiple elections
```

## Testing Scenarios

### Test Case 1: Allow Multi-Election

1. Create Election A and Election B
2. Set policy to ALLOW
3. Voter votes in A
4. Voter votes in B
5. **Expected**: Both votes recorded, voter appears in both elections' history

### Test Case 2: Restrict Single Election

1. Create Election A and Election B
2. Set policy to RESTRICT
3. Voter votes in A
4. Voter tries to vote in B
5. **Expected**: Vote B rejected, error message shown

### Test Case 3: Policy Change

1. Policy set to ALLOW
2. Voter A votes in Elections 1 and 2
3. Admin changes policy to RESTRICT
4. Voter B tries to vote in Election 1 then 2
5. **Expected**: Voter B blocked from Election 2

## Admin Monitoring

### Recommended Checks

1. Before opening multi-election voting:
   - Check if only one election is scheduled
   - Review voter database for duplicates

2. During voting period:
   - Monitor voter activity
   - Check voting history for anomalies
   - Track turnout per election

3. After policy change:
   - Verify new restrictions are working
   - Check affected voters are notified properly

## Security Considerations

### Vote Anonymity Preserved

- Votes remain anonymous (no voter ID stored with vote)
- Only participant tracking per election
- Vote choice not linked to voter identity

### Audit Trail

- Voter voted log tracks which elections
- Admins can query voting history
- Timestamp records when vote was cast

## Future Enhancements

Potential improvements:

1. Per-voter override (whitelist/blacklist specific voters)
2. Per-election override (allow specific voters to vote in restricted elections)
3. Timed restrictions (allow multi-voting until time X, then restrict)
4. Role-based policies (different policies for different voter groups)
5. Voting history export/reporting

## Troubleshooting

### Issue: Voter can't vote in second election with ALLOW policy

- **Check**: Is policy really set to ALLOW?
  - Go to Voting Policy panel, verify "ALLOW" button status
- **Check**: Did voter already vote in this election?
  - Search voter in history panel
- **Check**: Is second election active?
  - Check election schedule

### Issue: Voter not appearing in history

- **Check**: Voter ID spelled correctly
- **Check**: Did voter actually vote? (check vote count)
- **Check**: Is voting log file accessible?

## Summary

The Multi-Election Voting Control System provides administrators with flexible control over concurrent election participation:

- **ALLOW**: Support complex multi-election scenarios where voters choose which elections matter to them
- **RESTRICT**: Enforce single-election voting for simplified, focused voting periods
- **Transparent**: Voters see clear messages about restrictions and their voting history
- **Auditable**: Admins can track participation and enforce policies
- **Compatible**: Works with existing election infrastructure without changes
