package ee.ut.ege.deliveryservice.exception;

public class PaymentNotCompletedException extends RuntimeException {
    public PaymentNotCompletedException(String message) {
        super(message);
    }
}
