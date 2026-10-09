package ma.bankdirect.model;

import java.math.BigDecimal;

public record TransferRequest(
    long fromAccountId,
    Long toAccountId,       // null if external/beneficiary transfer
    Long beneficiaryId,     // null if internal transfer
    BigDecimal amount,
    String label
) {}
