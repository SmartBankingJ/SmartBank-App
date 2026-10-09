package ma.bankdirect.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ma.bankdirect.model.enums.AccountStatus;
import ma.bankdirect.model.enums.AccountType;

public class Account {
    private long id;
    private long customerId;
    private String rib;             // 24 digits in Morocco
    private AccountType type;       // CURRENT, SAVINGS, PREPAID
    private String currency;        // "MAD"
    private BigDecimal balance;     // ALWAYS BigDecimal!
    private AccountStatus status;   // ACTIVE, BLOCKED, CLOSED
    private LocalDateTime openedAt;

    public Account() {}

    public Account(long id, long customerId, String rib, AccountType type, 
                   String currency, BigDecimal balance, AccountStatus status, 
                   LocalDateTime openedAt) {
        this.id = id;
        this.customerId = customerId;
        this.rib = rib;
        this.type = type;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.openedAt = openedAt;
    }

    /**
     * Returns masked RIB for safe display in ComboBoxes & Lists.
     * Example: "011780000012345678901234" -> "0117 ... 1234"
     */
    public String getMaskedRib() {
        if (rib == null || rib.length() < 8) return rib;
        return rib.substring(0, 4) + " ... " + rib.substring(rib.length() - 4);
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }

    public String getRib() { return rib; }
    public void setRib(String rib) { this.rib = rib; }

    public AccountType getType() { return type; }
    public void setType(AccountType type) { this.type = type; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }
}
