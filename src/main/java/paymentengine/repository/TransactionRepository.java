package paymentengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import paymentengine.domain.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
