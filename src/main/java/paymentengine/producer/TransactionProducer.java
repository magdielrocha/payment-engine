package paymentengine.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import paymentengine.config.RabbitMQConfig;
import paymentengine.dto.TransactionEventDTO;

@Service
@RequiredArgsConstructor
public class TransactionProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendTransactionEvent(TransactionEventDTO event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.TRANSACTION_QUEUE, event);
        System.out.println("Message published to the queue: " + event.transactionId());
    }

}
