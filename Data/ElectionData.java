package Data;

import Entities.Nominee;
import Entities.Voter;
import java.io.*;
import java.util.*;

public class ElectionData {

    public static final String VOTER_FILE = "database_voters.txt";
    public static final String NOMINEE_FILE = "database_nominees.txt";
    public static final String VOTE_FILE = "database_votes.txt";

    // -------------------- VOTER FUNCTIONS --------------------

    // Check if voter ID exists in the database
    public static boolean voterIdExists(String voterId) {
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 1 && parts[0].equals(voterId)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Check if voter is already registered (has password)
    public static boolean isVoterRegistered(String voterId) {
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 3 && parts[0].equals(voterId)) {
                    return !parts[2].isEmpty();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Register voter or set password for existing unregistered voter
    public static boolean registerVoter(Voter voter) {
        List<String> lines = new ArrayList<>();
        boolean voterExists = false;
        boolean alreadyRegistered = false;
        
        // Read all lines and check status
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 3 && parts[0].equals(voter.getVoterId())) {
                    voterExists = true;
                    if (!parts[2].isEmpty()) {
                        alreadyRegistered = true; // Already has password
                    }
                }
                lines.add(line);
            }
        } catch (IOException e) {
            // File might not exist, that's OK
        }
        
        // If already registered, cannot register again
        if (alreadyRegistered) {
            System.out.println("Voter " + voter.getVoterId() + " is already registered!");
            return false;
        }
        
        // If voter exists but unregistered, update password
        if (voterExists) {
            List<String> updatedLines = new ArrayList<>();
            for (String line : lines) {
                String[] parts = line.split(":");
                if (parts.length >= 3 && parts[0].equals(voter.getVoterId())) {
                    updatedLines.add(parts[0] + ":" + parts[1] + ":" + voter.getPassword());
                } else {
                    updatedLines.add(line);
                }
            }
            lines = updatedLines;
        } else {
            // Add as completely new voter
            lines.add(voter.getVoterId() + ":" + voter.getName() + ":" + voter.getPassword());
        }
        
        // Write back to file
        try (PrintWriter writer = new PrintWriter(new FileWriter(VOTER_FILE))) {
            for (String line : lines) {
                writer.println(line);
            }
            System.out.println("Voter " + voter.getVoterId() + " registered successfully!");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Validate voter login
    public static boolean validateVoter(String voterId, String password) {
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 3 && parts[0].equals(voterId)) {
                    if (parts[2].equals(password)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Check if voter already voted
    public static boolean hasVoted(String voterId) {
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 1 && parts[0].equals(voterId)) {
                    return true;
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    // Get all voters
    public static String[] getAllVoters() {
        List<String> voters = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    voters.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return voters.toArray(new String[0]);
    }

    // Get all voter names
    public static String[] getAllVoterNames() {
        List<String> names = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 2) {
                    names.add(parts[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return names.toArray(new String[0]);
    }

    // Delete voter by name
    public static boolean deleteVoter(String voterName) {
        List<String> lines = new ArrayList<>();
        boolean deleted = false;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 2 && parts[1].equals(voterName)) {
                    deleted = true;
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        
        if (!deleted) return false;
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(VOTER_FILE))) {
            for (String line : lines) {
                writer.println(line);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Update voter password
    public static boolean updateVoterPassword(String voterId, String newPassword) {
        List<String> lines = new ArrayList<>();
        boolean updated = false;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 3 && parts[0].equals(voterId)) {
                    lines.add(parts[0] + ":" + parts[1] + ":" + newPassword);
                    updated = true;
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        
        if (!updated) return false;
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(VOTER_FILE))) {
            for (String line : lines) {
                writer.println(line);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // -------------------- NOMINEE FUNCTIONS --------------------

    public static boolean nomineeIdExists(String nomineeId) {
        try (BufferedReader reader = new BufferedReader(new FileReader(NOMINEE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 1 && parts[0].equals(nomineeId)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean addNominee(Nominee nominee) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(NOMINEE_FILE, true))) {
            writer.write(nominee.getNomineeId() + ":" + nominee.getNomineeName() + ":" + nominee.getPartyName());
            writer.newLine();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteNominee(String nomineeName) {
        List<String> lines = new ArrayList<>();
        boolean deleted = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(NOMINEE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 2 && parts[1].equals(nomineeName)) {
                    deleted = true;
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!deleted) return false;

        try (PrintWriter writer = new PrintWriter(new FileWriter(NOMINEE_FILE))) {
            for (String l : lines) {
                writer.println(l);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get all nominees
    public static String[] getAllNominees() {
        List<String> nominees = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(NOMINEE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    nominees.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nominees.toArray(new String[0]);
    }

    // -------------------- VOTE FUNCTIONS --------------------

    public static boolean castVote(String voterId, String nomineeId) {
        if (hasVoted(voterId)) {
            return false;
        }
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(VOTE_FILE, true))) {
            writer.write(voterId + ":" + nomineeId + ":" + System.currentTimeMillis());
            writer.newLine();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String[] getAllVotes() {
        List<String> votes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    votes.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return votes.toArray(new String[0]);
    }

    public static Map<String, Integer> getVoteCounts() {
        Map<String, Integer> counts = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 2) {
                    counts.put(parts[1], counts.getOrDefault(parts[1], 0) + 1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return counts;
    }

    public static int getTotalVotesCast() {
        int total = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTE_FILE))) {
            while (reader.readLine() != null) total++;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return total;
    }

    public static int getTotalRegisteredVoters() {
        int total = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(VOTER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length >= 3 && !parts[2].isEmpty()) {
                    total++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return total;
    }
}