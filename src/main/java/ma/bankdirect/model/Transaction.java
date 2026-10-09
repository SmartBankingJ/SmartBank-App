package ma.bankdirect.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ma.bankdirect.model.enums.Direction;
import ma.bankdirect.model.enums.TransactionStatus;
import ma.bankdirect.model.enums.TransactionType;

public class Transaction {
    private long id;
    private long accountId;
    private TransactionType type;
    private Direction direction;       // DEBIT or CREDIT
    private BigDecimal amount;
    private BigDecimal balanceAfter;  // Balance snapshot immediately after this op
    private String label;
    private String reference;         // Unique reference e.g. "VIR-9F8A2B1C"
    private String counterpartRib;    // The other account's RIB (if applicable)
    private TransactionStatus status; // PENDING, COMPLETED, FAILED
    private LocalDateTime createdAt;

    public Transaction() {}

    public Transaction(long id, long accountId, TransactionType type, Direction direction,
                       BigDecimal amount, BigDecimal balanceAfter, String label,
                       String reference, String counterpartRib, TransactionStatus status,
                       LocalDateTime createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.type = type;
        this.direction = direction;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.label = label;
        this.reference = reference;
        this.counterpartRib = counterpartRib;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Static factory helper for DEBIT
    public static Transaction debit(long accountId, TransferRequest req, String ref, 
                                    BigDecimal balanceAfter, LocalDateTime now) {
        return new Transaction(0, accountId, TransactionType.INTERNAL_TRANSFER,
                Direction.DEBIT, req.amount(), balanceAfter, req.label(),
                ref, null, TransactionStatus.COMPLETED, now);
    }

    // Static factory helper for CREDIT
    public static Transaction credit(long accountId, TransferRequest req, String ref, 
                                     BigDecimal balanceAfter, LocalDateTime now) {
        return new Transaction(0, accountId, TransactionType.INTERNAL_TRANSFER,
                Direction.CREDIT, req.amount(), balanceAfter, req.label(),
                ref, null, TransactionStatus.COMPLETED, now);
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getAccountId() { return accountId; }
    public void setAccountId(long accountId) { this.accountId = accountId; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getCounterpartRib() { return counterpartRib; }
    public void setCounterpartRib(String counterpartRib) { this.counterpartRib = counterpartRib; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
