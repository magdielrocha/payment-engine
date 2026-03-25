package paymentengine.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import paymentengine.config.RabbitMQConfig;
import paymentengine.dto.TransactionEventDTO;

@Component
public class TransactionConsumer {

    @RabbitListener(queues = RabbitMQConfig.TRANSACTION_QUEUE)
    public void processTransactionReceipt(TransactionEventDTO event) {
        System.out.println("=================================================");
        System.out.println("📥 [WORKER] New transfer received from the queue!");
        System.out.println("Transaction ID: " + event.transactionId());
        System.out.println("Amount: R$ " + event.amount());
        System.out.println("Starting PDF receipt generation...");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("✅ [WORKER] Receipt successfully generated and sent to the clients!");
        System.out.println("=================================================");
    }
}
