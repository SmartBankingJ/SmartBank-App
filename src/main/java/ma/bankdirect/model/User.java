package ma.bankdirect.model;

public class User {
    private long id;
    private String username;
    private String passwordHash;
    private String role;           // "CUSTOMER", "ADMIN", "ADVISOR"
    private String status;         // "ACTIVE", "LOCKED"
    private int failedAttempts;
    private long customerId;       // Foreign key to Customer.id

    public User() {}

    public User(long id, String username, String passwordHash, String role, 
                String status, int failedAttempts, long customerId) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.failedAttempts = failedAttempts;
        this.customerId = customerId;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }
}
