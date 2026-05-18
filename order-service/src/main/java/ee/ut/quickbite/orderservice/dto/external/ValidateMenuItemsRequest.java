package ee.ut.quickbite.orderservice.dto.external;

import java.util.List;
import java.util.UUID;

/**
 * Producer-side payload for menu-service POST /menu-items/validate. Mirrors
 * the menu-service DTO field-for-field so Jackson can serialize without
 * any custom mapping.
 */
public record ValidateMenuItemsRequest(List<Line> items) {

    public record Line(UUID menuItemId, Integer quantity) {}
}
