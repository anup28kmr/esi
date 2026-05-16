package ee.ut.anup.orderservice.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Consumer-side mirror of menu-service ValidateMenuItemsResponse. menu-service
 * decorates `isAvailable` with @JsonProperty("isAvailable") so we accept the
 * same JSON key explicitly here.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ValidateMenuItemsResponse(
        boolean allValid,
        List<Line> items,
        BigDecimal totalAmount,
        String currency) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Line(
            UUID menuItemId,
            int quantity,
            boolean exists,
            @JsonProperty("isAvailable") boolean isAvailable,
            BigDecimal unitPriceAmount,
            String unitPriceCurrency,
            BigDecimal lineTotal,
            String error) {}
}
