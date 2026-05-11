package ee.ut.anup.orderservice.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
    String menuItemId, String name, BigDecimal unitPrice, Integer quantity) {}
