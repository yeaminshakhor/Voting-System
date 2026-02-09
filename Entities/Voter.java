package Entities;

public class Voter {
    private String voterId;
    private String name;
    private String password;

    public Voter() {}

    public Voter(String voterId, String name, String password) {
        this.voterId = voterId;
        this.name = (name == null || name.isEmpty()) ? "NA" : name;
        this.password = password;
    }

    public String getVoterId() { return voterId; }
    public String getId() { return voterId; }
    public void setId(String voterId) { this.voterId = voterId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}