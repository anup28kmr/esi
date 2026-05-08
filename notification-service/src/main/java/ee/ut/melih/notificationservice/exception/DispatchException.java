package ee.ut.melih.notificationservice.exception;

public class DispatchException extends RuntimeException {
  public DispatchException(String message, Throwable cause) {
    super(message, cause);
  }

  public DispatchException(String message) {
    super(message);
  }
}
