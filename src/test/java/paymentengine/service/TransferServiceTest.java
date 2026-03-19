package paymentengine.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import paymentengine.domain.Account;
import paymentengine.domain.AccountStatus;
import paymentengine.domain.Transaction;
import paymentengine.domain.TransactionStatus;
import paymentengine.exception.InsufficientBalanceException;
import paymentengine.repository.AccountRepository;
import paymentengine.repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransferService transferService;


    @Test
    @DisplayName("Should execute transfer successfully when balance is sufficient")
    void shouldExecuteTransferSuccessfully() {

        // Arrange
        Account source = new Account(1L, "111", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        Account destination = new Account(2L, "222", new BigDecimal("500.00"), AccountStatus.ACTIVE);

        when(accountRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(source));
        when(accountRepository.findByIdWithPessimisticLock(2L)).thenReturn(Optional.of(destination));

        Transaction savedTransaction = new Transaction();
        savedTransaction.setId(100L);
        savedTransaction.setStatus(TransactionStatus.SUCCESS);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // Act
        Transaction result = transferService.executeTransfer(1L, 2L, new BigDecimal("250.00"));

        // Assert
        assertNotNull(result);
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertEquals(new BigDecimal("750.00"), source.getBalance());
        assertEquals(new BigDecimal("750.00"), destination.getBalance());

        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when source account does not have enough funds")
    void shouldThrowExceptionWhenInsufficientBalance() {

        // Arrange
        Account source = new Account(1L, "111", new BigDecimal("100.00"), AccountStatus.ACTIVE);
        Account destination = new Account(2L, "222", new BigDecimal("500.00"), AccountStatus.ACTIVE);

        when(accountRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(source));
        when(accountRepository.findByIdWithPessimisticLock(2L)).thenReturn(Optional.of(destination));

        // Act & Assert
        assertThrows(InsufficientBalanceException.class, () -> {
            transferService.executeTransfer(1L, 2L, new BigDecimal("250.00"));
        });

        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
