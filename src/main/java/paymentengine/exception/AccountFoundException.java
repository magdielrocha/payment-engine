package paymentengine.exception;

public class AccountFoundException extends RuntimeException {
    public AccountFoundException(String message) {
        super(message);
    }
}
