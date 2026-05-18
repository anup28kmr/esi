package ee.ut.quickbite.orderservice.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Consumer-side mirror of restaurant-service's RestaurantResponse. We carry
 * just enough to construct the pickup address for delivery scheduling AND
 * to verify ownership when an owner accepts/rejects an order. Everything
 * else (operating hours, audit timestamps) is ignored by Jackson.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RestaurantSummaryResponse(
        UUID restaurantId,
        UUID ownerId,
        String name,
        String address,
        String city) {}
