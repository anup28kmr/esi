package ee.ut.ege.deliveryservice.service;

import ee.ut.ege.deliveryservice.client.PaymentClient;
import ee.ut.ege.deliveryservice.client.PaymentClientResponse;
import ee.ut.ege.deliveryservice.domain.Delivery;
import ee.ut.ege.deliveryservice.domain.DeliveryStatus;
import ee.ut.ege.deliveryservice.dto.*;
import ee.ut.ege.deliveryservice.exception.DeliveryNotFoundException;
import ee.ut.ege.deliveryservice.exception.InvalidDeliveryStateException;
import ee.ut.ege.deliveryservice.exception.PaymentNotCompletedException;
import ee.ut.ege.deliveryservice.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final PaymentClient paymentClient;
    private final DeliveryEventPublisher eventPublisher;

    @Transactional
    public DeliveryResponse createDelivery(CreateDeliveryRequest request) {
        if (deliveryRepository.existsByOrderId(request.getOrderId())) {
            throw new InvalidDeliveryStateException(
                    "Delivery already exists for order: " + request.getOrderId());
        }

        // Real cross-service call: verify payment is COMPLETED before scheduling delivery
        PaymentClientResponse payment = paymentClient.getPaymentByOrderId(request.getOrderId())
                .orElseThrow(() -> new PaymentNotCompletedException(
                        "No payment found for order: " + request.getOrderId()));

        if (!"COMPLETED".equals(payment.getStatus())) {
            throw new PaymentNotCompletedException(
                    "Payment for order " + request.getOrderId()
                    + " is not completed (status: " + payment.getStatus() + ")");
        }

        Delivery delivery = Delivery.builder()
                .orderId(request.getOrderId())
                .status(DeliveryStatus.PENDING)
                .pickupAddress(request.getPickupAddress())
                .deliveryAddress(request.getDeliveryAddress())
                .estimatedDeliveryTime(request.getEstimatedDeliveryTime())
                .build();

        delivery = deliveryRepository.save(delivery);
        eventPublisher.publishDeliveryCreated(delivery.getDeliveryId(), delivery.getOrderId());
        return toResponse(delivery);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getDelivery(UUID deliveryId) {
        return toResponse(findById(deliveryId));
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getDeliveryByOrderId(UUID orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new DeliveryNotFoundException(
                        "No delivery found for order: " + orderId));
        return toResponse(delivery);
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getAllDeliveries() {
        return deliveryRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getDeliveriesByStatus(DeliveryStatus status) {
        return deliveryRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeliveryResponse assignDriver(UUID deliveryId, AssignDriverRequest request) {
        Delivery delivery = findById(deliveryId);

        if (delivery.getStatus() != DeliveryStatus.PENDING) {
            throw new InvalidDeliveryStateException(
                    "Driver can only be assigned to PENDING deliveries, current status: "
                    + delivery.getStatus());
        }

        delivery.setDriverId(request.getDriverId());
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery = deliveryRepository.save(delivery);

        eventPublisher.publishDeliveryStatusUpdated(
                delivery.getDeliveryId(), delivery.getOrderId(), DeliveryStatus.ASSIGNED);
        return toResponse(delivery);
    }

    @Transactional
    public DeliveryResponse updateStatus(UUID deliveryId, UpdateDeliveryStatusRequest request) {
        Delivery delivery = findById(deliveryId);
        DeliveryStatus current = delivery.getStatus();
        DeliveryStatus next = request.getStatus();

        validateTransition(current, next);

        delivery.setStatus(next);
        if (next == DeliveryStatus.DELIVERED) {
            delivery.setActualDeliveryTime(Instant.now());
        }
        delivery = deliveryRepository.save(delivery);

        eventPublisher.publishDeliveryStatusUpdated(
                delivery.getDeliveryId(), delivery.getOrderId(), next);
        return toResponse(delivery);
    }

    @Transactional
    public DeliveryResponse cancelDelivery(UUID deliveryId) {
        Delivery delivery = findById(deliveryId);

        if (delivery.getStatus() == DeliveryStatus.DELIVERED
                || delivery.getStatus() == DeliveryStatus.CANCELLED) {
            throw new InvalidDeliveryStateException(
                    "Cannot cancel a delivery with status: " + delivery.getStatus());
        }

        delivery.setStatus(DeliveryStatus.CANCELLED);
        delivery = deliveryRepository.save(delivery);

        eventPublisher.publishDeliveryStatusUpdated(
                delivery.getDeliveryId(), delivery.getOrderId(), DeliveryStatus.CANCELLED);
        return toResponse(delivery);
    }

    private void validateTransition(DeliveryStatus current, DeliveryStatus next) {
        boolean valid = switch (next) {
            case PICKED_UP  -> current == DeliveryStatus.ASSIGNED;
            case IN_TRANSIT -> current == DeliveryStatus.PICKED_UP;
            case DELIVERED  -> current == DeliveryStatus.IN_TRANSIT;
            case FAILED     -> current == DeliveryStatus.PICKED_UP
                               || current == DeliveryStatus.IN_TRANSIT;
            case CANCELLED  -> current != DeliveryStatus.DELIVERED
                               && current != DeliveryStatus.CANCELLED;
            default         -> false;
        };

        if (!valid) {
            throw new InvalidDeliveryStateException(
                    "Cannot transition from " + current + " to " + next);
        }
    }

    private Delivery findById(UUID deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryNotFoundException("Delivery not found: " + deliveryId));
    }

    private DeliveryResponse toResponse(Delivery d) {
        return DeliveryResponse.builder()
                .deliveryId(d.getDeliveryId())
                .orderId(d.getOrderId())
                .driverId(d.getDriverId())
                .status(d.getStatus())
                .pickupAddress(d.getPickupAddress())
                .deliveryAddress(d.getDeliveryAddress())
                .estimatedDeliveryTime(d.getEstimatedDeliveryTime())
                .actualDeliveryTime(d.getActualDeliveryTime())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
