package ma.bankdirect.model;

public class Customer {
    private long id;
    private String firstName;
    private String lastName;
    private String cin;       // Carte d'Identité Nationale (ex: "BK123456")
    private String email;
    private String phone;
    private String address;
    private Long advisorId;   // Can be null if no assigned advisor

    public Customer() {}

    public Customer(long id, String firstName, String lastName, String cin, 
                    String email, String phone, String address, Long advisorId) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.cin = cin;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.advisorId = advisorId;
    }

    // Helper method: Computed property (Course rule 3.3: never store fullName in DB)
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getCin() { return cin; }
    public void setCin(String cin) { this.cin = cin; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Long getAdvisorId() { return advisorId; }
    public void setAdvisorId(Long advisorId) { this.advisorId = advisorId; }
}
