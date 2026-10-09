package ma.bankdirect.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferReceipt(
    String reference,
    BigDecimal amount,
    BigDecimal newBalance,
    LocalDateTime executedAt
) {}
