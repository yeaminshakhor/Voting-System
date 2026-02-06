package Entities;

/**
 * Represents a nominee with an identifier, name, and party.
 */
public class Nominee {
    private String nomineeId;
    private String nomineeName;
    private String partyName;

    public Nominee() {}

    public Nominee(String nomineeId, String nomineeName, String partyName) {
        this.nomineeId = nomineeId;
        this.nomineeName = nomineeName;
        this.partyName = partyName;
    }

    public String getNomineeId() { return nomineeId; }
    public void setNomineeId(String nomineeId) { this.nomineeId = nomineeId; }
    public String getNomineeName() { return nomineeName; }
    public void setNomineeName(String nomineeName) { this.nomineeName = nomineeName; }
    public String getPartyName() { return partyName; }
    public void setPartyName(String partyName) { this.partyName = partyName; }

    @Override
    public String toString() {
        return nomineeName + " (" + partyName + ")";
    }
}