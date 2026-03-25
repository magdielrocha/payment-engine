package paymentengine.dto;

import java.math.BigDecimal;

public record TransactionEventDTO(
        Long transactionId,
        Long sourceAccountId,
        Long destinationAccountId,
        BigDecimal amount
) {}
