package ma.bankdirect.model;

import java.time.LocalDateTime;

public class Beneficiary {
    private long id;
    private long customerId;
    private String fullName;
    private String rib;
    private String bankName;
    private LocalDateTime createdAt;

    public Beneficiary() {}

    public Beneficiary(long id, long customerId, String fullName, 
                       String rib, String bankName, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.fullName = fullName;
        this.rib = rib;
        this.bankName = bankName;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRib() { return rib; }
    public void setRib(String rib) { this.rib = rib; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
