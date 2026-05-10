package ee.ut.melih.notificationservice.config;

public final class KafkaTopics {
  public static final String PAYMENT_EVENTS = "payment-events";
  public static final String DELIVERY_EVENTS = "delivery-events";
  public static final String ORDER_EVENTS = "order-events";
  public static final String NOTIFICATION_DLQ = "notification-events.DLQ";

  private KafkaTopics() {}
}
