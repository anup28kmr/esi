package ee.ut.quickbite.orderservice.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ee.ut.quickbite.orderservice.constants.OrderServiceConstants;
import ee.ut.quickbite.orderservice.entity.Order;
import ee.ut.quickbite.orderservice.repository.OrderRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges delivery-service status changes into order-service's own DB so
 * the order timeline reflects late-stage transitions (PICKED_UP, DELIVERED)
 * that originate in the delivery driver flow.
 *
 * <p>Wire format: delivery-service publishes the {@code delivery-events}
 * topic with {@code StringSerializer} -- the value on the wire is a JSON
 * envelope {@code {id, type, occurredAt, payload: {deliveryId, orderId,
 * status[, driverId]}}}. We deserialize as {@code String} and parse with a
 * locally-constructed Jackson ObjectMapper because under Boot 4 + Jackson 3
 * the auto-configured {@code com.fasterxml.jackson} mapper bean is not
 * available for DI in this service.
 *
 * <p>Order-id mapping: order-service's PK is a Long but delivery-service's
 * orderId is a UUID. {@code OrderServiceImpl#placeOrder} derives the UUID
 * via {@code new UUID(0L, orderPk)}, so we recover the PK from the low
 * bits when the high bits are zero. Any other UUID shape is treated as
 * "not from us" and skipped.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class DeliveryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventConsumer.class);

    private static final String DELIVERY_EVENTS_TOPIC = "delivery-events";
    private static final String STATUS_CHANGED_TYPE = "delivery.status-changed";
    private static final String DELIVERY_STATUS_PICKED_UP = "PICKED_UP";
    private static final String DELIVERY_STATUS_DELIVERED = "DELIVERED";

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public DeliveryEventConsumer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @KafkaListener(topics = DELIVERY_EVENTS_TOPIC, groupId = "order-service")
    @Transactional
    public void onDeliveryEvent(String payload) {
        if (payload == null || payload.isBlank()) {
            log.warn("Ignoring empty delivery-events record");
            return;
        }
        Map<String, Object> envelope;
        try {
            envelope = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Could not parse delivery-events JSON: {}", e.getMessage());
            return;
        }
        String type = asString(envelope.get("type"));
        if (!STATUS_CHANGED_TYPE.equals(type)) {
            // delivery.created and any future types are irrelevant here.
            return;
        }
        Object rawPayload = envelope.get("payload");
        if (!(rawPayload instanceof Map<?, ?> p)) {
            log.warn("delivery.status-changed missing payload object");
            return;
        }
        String deliveryStatus = asString(p.get("status"));
        if (!DELIVERY_STATUS_PICKED_UP.equals(deliveryStatus)
                && !DELIVERY_STATUS_DELIVERED.equals(deliveryStatus)) {
            // PENDING/ASSIGNED/IN_TRANSIT/FAILED/CANCELLED don't change
            // the order timeline in this service -- ignore quietly.
            return;
        }
        Optional<Long> orderPk = decodeOrderPk(asString(p.get("orderId")));
        if (orderPk.isEmpty()) {
            log.warn("delivery.status-changed orderId not decodable to Long PK: {}", p.get("orderId"));
            return;
        }
        Long id = orderPk.get();
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            log.warn("delivery.status-changed for unknown orderId={} (status={})", id, deliveryStatus);
            return;
        }
        String newStatus = deliveryStatus.equals(DELIVERY_STATUS_PICKED_UP)
                ? OrderServiceConstants.STATUS_PICKED_UP
                : OrderServiceConstants.STATUS_DELIVERED;
        String previous = order.getStatus();
        if (newStatus.equalsIgnoreCase(previous)) {
            // Duplicate delivery (at-least-once). Acknowledge and move on.
            return;
        }
        order.setStatus(newStatus);
        orderRepository.save(order);
        log.info("order status mirrored from delivery orderId={} {} -> {}", id, previous, newStatus);
    }

    /**
     * Recovers the Long PK from a UUID emitted by {@code OrderServiceImpl}
     * (msb == 0; lsb holds the PK). Returns empty for UUIDs that don't
     * match that shape so we don't blindly stomp on rows by accident.
     */
    private static Optional<Long> decodeOrderPk(String orderIdRaw) {
        if (orderIdRaw == null || orderIdRaw.isBlank()) {
            return Optional.empty();
        }
        try {
            UUID uuid = UUID.fromString(orderIdRaw);
            if (uuid.getMostSignificantBits() != 0L) {
                return Optional.empty();
            }
            return Optional.of(uuid.getLeastSignificantBits());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
