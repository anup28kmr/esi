package ee.ut.anup.orderservice.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Consumer-side mirror of restaurant-service's RestaurantResponse. We only
 * need restaurantId and the location fields to construct the pickup address
 * for the delivery task -- everything else (owner, operating hours, audit
 * timestamps) is left to be ignored by Jackson.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RestaurantSummaryResponse(
        UUID restaurantId,
        String name,
        String address,
        String city) {}
