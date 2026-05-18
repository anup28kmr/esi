package ee.ut.quickbite.orderservice.dto;

import java.math.BigDecimal;

public record OrderLineRequest(
    String menuItemId, String name, BigDecimal unitPrice, Integer quantity) {}
