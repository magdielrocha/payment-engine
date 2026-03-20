package paymentengine.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import paymentengine.domain.Account;
import paymentengine.domain.Transaction;
import paymentengine.domain.TransactionStatus;
import paymentengine.exception.AccountNotFoundException;
import paymentengine.exception.InactiveAccountException;
import paymentengine.exception.InsufficientBalanceException;
import paymentengine.repository.AccountRepository;
import paymentengine.repository.TransactionRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;


    @Transactional
    public Transaction executeTransfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount) {


        if (sourceAccountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same");
        }

        Account sourceAccount = accountRepository.findByIdWithPessimisticLock(sourceAccountId)
                .orElseThrow(() -> new AccountNotFoundException("Source account not found with ID: " + sourceAccountId));

        Account destinationAccount = accountRepository.findByIdWithPessimisticLock(destinationAccountId)
                .orElseThrow(() -> new AccountNotFoundException("Destination account not found with ID: " + destinationAccountId));


        if (!sourceAccount.isActive() || !destinationAccount.isActive()) {
            throw new InactiveAccountException("Both accounts must be ACTIVE to perform a transfer");
        }

        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in source account");
        }

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        destinationAccount.setBalance(destinationAccount.getBalance().add(amount));

        Transaction transaction = new Transaction();
        transaction.setSourceAccount(sourceAccount);
        transaction.setDestinationAccount(destinationAccount);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);


        return transactionRepository.save(transaction);

    }



}
