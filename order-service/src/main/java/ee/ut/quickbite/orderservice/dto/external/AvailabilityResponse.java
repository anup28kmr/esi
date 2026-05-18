package ee.ut.quickbite.orderservice.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumer-side mirror of restaurant-service's AvailabilityResponse. Only the
 * fields the order placement flow reads are declared; unknown JSON properties
 * are ignored so future restaurant-service additions don't break us.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AvailabilityResponse(
        UUID restaurantId,
        boolean isOpen,
        boolean acceptsOrders,
        String operatingHours,
        Instant checkedAt) {}
