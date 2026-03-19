package paymentengine.dto;

import paymentengine.domain.Transaction;
import paymentengine.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponseDTO(
        Long transactionId,
        Long sourceAccountId,
        Long destinationAccountId,
        BigDecimal amount,
        TransactionStatus status,
        LocalDateTime createdAt
) {
    public TransferResponseDTO(Transaction transaction) {
        this(
                transaction.getId(),
                transaction.getSourceAccount().getId(),
                transaction.getDestinationAccount().getId(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }
}
