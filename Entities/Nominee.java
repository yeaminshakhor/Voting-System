package Entities;

/**
 * Represents a nominee with an identifier, name, party, and election association.
 */
public class Nominee {
    private String nomineeId;
    private String nomineeName;
    private String partyName;
    private String electionId;

    public Nominee() {}

    public Nominee(String nomineeId, String nomineeName, String partyName) {
        this.nomineeId = nomineeId;
        this.nomineeName = nomineeName;
        this.partyName = partyName;
        this.electionId = "DEFAULT";
    }

    public Nominee(String nomineeId, String nomineeName, String partyName, String electionId) {
        this.nomineeId = nomineeId;
        this.nomineeName = nomineeName;
        this.partyName = partyName;
        this.electionId = (electionId == null || electionId.isEmpty()) ? "DEFAULT" : electionId;
    }

    public String getNomineeId() { return nomineeId; }
    public String getId() { return nomineeId; }
    public void setId(String nomineeId) { this.nomineeId = nomineeId; }
    public void setNomineeId(String nomineeId) { this.nomineeId = nomineeId; }
    public String getNomineeName() { return nomineeName; }
    public String getName() { return nomineeName; }
    public void setName(String nomineeName) { this.nomineeName = nomineeName; }
    public void setNomineeName(String nomineeName) { this.nomineeName = nomineeName; }
    public String getPartyName() { return partyName; }
    public String getParty() { return partyName; }
    public void setPartyName(String partyName) { this.partyName = partyName; }
    public void setParty(String partyName) { this.partyName = partyName; }
    public String getElectionId() { return electionId; }
    public void setElectionId(String electionId) { this.electionId = electionId; }

    @Override
    public String toString() {
        return nomineeName + " (" + partyName + ")";
    }
}